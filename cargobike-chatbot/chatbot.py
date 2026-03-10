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
import re
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

ONTOLOGY-GROUNDED REASONING
Before choosing a tool, you MAY call queryOntology with a SPARQL SELECT query to
verify class hierarchies or property applicability (e.g., confirm that
cb:EbikeCargoBike is a subclass of cb:CargoBike before calling listCargoBikes).

STRUCTURED RESPONSE FORMAT
Your final answer (once all tool calls are done) MUST be a JSON object with
exactly two keys — no prose before or after it:

{{
  "reasoning": {{
    "user_intent": "<one-line summary of what the user is asking>",
    "mapped_concepts": ["<ontology URI or concept used>", ...],
    "inferences_used": ["<e.g. cb:EbikeCargoBike rdfs:subClassOf cb:CargoBike>", ...],
    "tools_selected": [
      {{"tool": "<toolName>", "justified_by": "<ontology concept or property>"}}
    ]
  }},
  "answer": "<friendly plain-language answer to the user>"
}}

The "reasoning" block is the explainability artifact — every concept must be a
real URI from the ontology above. If no ontology inference was needed, set
"inferences_used" to [].
"""


# ---------------------------------------------------------------------------
# MCP ↔ Anthropic tool schema conversion
# ---------------------------------------------------------------------------

def mcp_tool_to_claude(tool) -> dict:
    """Convert an MCP Tool object to the Anthropic API tool dict format.

    Tool-level x-semantic annotations (operatesOn / returns) are appended to
    the description so Claude can use them to justify tool selection formally.
    """
    description = tool.description or ""
    sem = (tool.model_extra or {}).get("x-semantic") if hasattr(tool, "model_extra") else None
    if sem:
        description += (
            f"\n[x-semantic: ontology={sem.get('ontology', '')},"
            f" operatesOn={sem.get('operatesOn', '')},"
            f" returns={sem.get('returns', '')}]"
        )
    return {
        "name": tool.name,
        "description": description,
        "input_schema": tool.inputSchema,
    }


# ---------------------------------------------------------------------------
# Agentic loop
# ---------------------------------------------------------------------------

def _parse_structured_response(text: str) -> tuple[dict | None, str]:
    """
    Try to extract {"reasoning": {...}, "answer": "..."} from the final text.
    Returns (reasoning_dict, answer_text). If parsing fails, returns (None, text).
    """
    # Strip optional markdown code fences
    stripped = re.sub(r"^```(?:json)?\s*", "", text.strip(), flags=re.MULTILINE)
    stripped = re.sub(r"\s*```$", "", stripped.strip(), flags=re.MULTILINE)
    try:
        obj = json.loads(stripped.strip())
        if isinstance(obj, dict) and "reasoning" in obj and "answer" in obj:
            return obj["reasoning"], str(obj["answer"])
    except (json.JSONDecodeError, ValueError):
        pass
    return None, text


def _print_reasoning(reasoning: dict) -> None:
    print("\n  ┌─ Reasoning trace ────────────────────────────────────")
    if intent := reasoning.get("user_intent"):
        print(f"  │  Intent   : {intent}")
    if concepts := reasoning.get("mapped_concepts"):
        print(f"  │  Concepts : {', '.join(concepts)}")
    if inferences := reasoning.get("inferences_used"):
        for inf in inferences:
            print(f"  │  Inferred : {inf}")
    if tools := reasoning.get("tools_selected"):
        for t in tools:
            print(f"  │  Tool     : {t.get('tool')} — justified by {t.get('justified_by')}")
    print("  └──────────────────────────────────────────────────────")


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
      3. Feed results back and repeat until Claude produces a structured JSON answer.
         The reasoning block is printed to the console; the answer text is returned.
    """
    while True:
        response = client.messages.create(
            model=MODEL,
            max_tokens=2048,
            system=system,
            tools=claude_tools,
            messages=history,
        )

        tool_uses = [b for b in response.content if b.type == "tool_use"]
        text_blocks = [b for b in response.content if b.type == "text"]

        if not tool_uses:
            raw = " ".join(b.text for b in text_blocks)
            reasoning, answer = _parse_structured_response(raw)
            if reasoning:
                _print_reasoning(reasoning)
            return answer

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
