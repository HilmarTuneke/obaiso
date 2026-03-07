"""
Agentic chatbot for the cargo bike e-commerce store.

Connects to the cargobike MCP server over stdio, uses the RDFS ontology to
ground Claude's understanding of domain concepts, and runs an agentic tool-use
loop to answer natural-language questions.

Usage:
    ANTHROPIC_API_KEY=sk-... python chatbot.py
"""

import asyncio
import json
import pathlib
import sys

import anthropic
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

# ---------------------------------------------------------------------------
# Paths (relative to this file's location)
# ---------------------------------------------------------------------------

_BASE = pathlib.Path(__file__).parent.parent

ONTOLOGY_PATH = (
    _BASE / "cargobike-mcp-starter/src/main/resources/assets/ontology/cargobike.ttl"
)
MCP_JAR = _BASE / "cargobike-mcp-starter/target/quarkus-app/quarkus-run.jar"

MODEL = "claude-sonnet-4-6"


# ---------------------------------------------------------------------------
# System prompt — ontology is embedded so Claude understands domain semantics
# ---------------------------------------------------------------------------

def build_system_prompt(ontology: str) -> str:
    return f"""\
You are a helpful assistant for a cargo bike e-commerce store.
Use the available tools to answer questions about bikes, customers, orders, \
inventory, and shipping.

DOMAIN ONTOLOGY (RDFS/Turtle)
The following ontology defines the meaning of all domain concepts. Use it to
interpret user requests and map them to the correct tool and arguments:

```turtle
{ontology}
```

Semantic mapping hints:
- "cargo bike" / "bike"          → cb:CargoBike        (listCargoBikes, getBikeBySku)
- "electric" / "e-bike"          → cb:EbikeCargoBike   (subclass of cb:CargoBike)
- "stock" / "availability"       → cb:InventoryItem     (getInventoryBySku)
- "shipping cost" / "delivery"   → cb:ShipmentQuote     (getShipmentQuote)
- "order" / "order status"       → cb:Order             (getOrder)
- "customer" / "buyer"           → cb:Customer          (getCustomer)
- cb:hasSku identifies a bike; SKUs look like SKU-CB-001 or SKU-ECB-900
- Customer IDs look like CUST-123, order IDs like ORD-1001

Always call the appropriate tool to retrieve live data rather than guessing.
Summarise results in plain, friendly language.
"""


# ---------------------------------------------------------------------------
# MCP ↔ Anthropic tool schema conversion
# ---------------------------------------------------------------------------

def mcp_tool_to_claude(tool) -> dict:
    """Convert an MCP Tool object to the Anthropic API tool dict format."""
    return {
        "name": tool.name,
        "description": tool.description or "",
        "input_schema": tool.inputSchema,
    }


# ---------------------------------------------------------------------------
# Agentic loop
# ---------------------------------------------------------------------------

async def run_agent(
    session: ClientSession,
    claude_tools: list[dict],
    history: list[dict],
    client: anthropic.Anthropic,
    system: str,
) -> str:
    """
    Drive an agentic tool-use loop:
      1. Call Claude with the current conversation history.
      2. If Claude requests tool calls, execute them via the MCP session.
      3. Feed results back and repeat until Claude produces a plain-text answer.
    """
    while True:
        response = client.messages.create(
            model=MODEL,
            max_tokens=1024,
            system=system,
            tools=claude_tools,
            messages=history,
        )

        tool_uses = [b for b in response.content if b.type == "tool_use"]
        text_blocks = [b for b in response.content if b.type == "text"]

        if not tool_uses:
            return " ".join(b.text for b in text_blocks)

        # Append the full assistant turn (may include both text and tool_use blocks)
        history.append({"role": "assistant", "content": response.content})

        # Execute each tool call and collect results
        tool_results = []
        for block in tool_uses:
            print(f"  [tool] {block.name}({json.dumps(block.input)})")
            mcp_result = await session.call_tool(block.name, block.input)
            # Flatten MCP content items to a single JSON string
            texts = [
                c.text if hasattr(c, "text") else str(c)
                for c in mcp_result.content
            ]
            tool_results.append({
                "type": "tool_result",
                "tool_use_id": block.id,
                "content": json.dumps(texts),
            })

        history.append({"role": "user", "content": tool_results})


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

async def main() -> None:
    if not ONTOLOGY_PATH.exists():
        sys.exit(f"Ontology not found: {ONTOLOGY_PATH}")
    if not MCP_JAR.exists():
        sys.exit(
            f"MCP JAR not found: {MCP_JAR}\n"
            "Build it first:  cd ../cargobike-mcp-starter && mvn -q package"
        )

    ontology = ONTOLOGY_PATH.read_text(encoding="utf-8")
    system_prompt = build_system_prompt(ontology)

    server_params = StdioServerParameters(
        command="java",
        args=["--sun-misc-unsafe-memory-access=allow", "-jar", str(MCP_JAR)],
    )

    client = anthropic.Anthropic()  # reads ANTHROPIC_API_KEY from environment

    print("Cargo Bike Assistant")

    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools_response = await session.list_tools()
            claude_tools = [mcp_tool_to_claude(t) for t in tools_response.tools]
            print("Type your question, or 'quit' to exit.\n")

            while True:
                try:
                    user_input = (await asyncio.to_thread(input, "You: ")).strip()
                except (EOFError, KeyboardInterrupt):
                    print("\nBye!")
                    break

                if not user_input or user_input.lower() in {"quit", "exit", "q"}:
                    print("Bye!")
                    break

                history = [{"role": "user", "content": user_input}]
                answer = await run_agent(
                    session, claude_tools, history, client, system_prompt
                )
                print(f"\nAssistant: {answer}\n")


if __name__ == "__main__":
    asyncio.run(main())
