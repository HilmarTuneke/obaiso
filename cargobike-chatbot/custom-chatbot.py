"""
Agentic chatbot for the cargo bike e-commerce store.

Connects to the cargobike MCP server over stdio, uses the RDFS ontology to
ground Claude's understanding of domain concepts, and runs an agentic tool-use
loop to answer natural-language questions.

Usage:
    python chatbot.py  # reads LLM_API_KEY, LLM_BASE_URL, and LLM_MODEL from .env
"""

import asyncio
import json
import os
import pathlib
import re
import sys
from dotenv import load_dotenv
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client
from openai import OpenAI

_BASE = pathlib.Path(__file__).parent
_PARENT = _BASE.parent
load_dotenv(_BASE / ".env")

_ONTOLOGY_DIR = (
        _PARENT / "cargobike-mcp-starter/src/main/resources/assets/ontology"
)
_ONTOLOGY_FILES = ["catalog.ttl", "customers.ttl", "inventory.ttl", "orders.ttl", "shipping.ttl"]

MCP_JAR = _PARENT / "cargobike-mcp-starter/target/quarkus-app/quarkus-run.jar"


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
- "stock" / "availability"       → inv:InventoryItem     (getInventoryBySku)

ONTOLOGY-GROUNDED REASONING
Before choosing a tool, you MAY call queryOntology with a SPARQL SELECT query to
verify class hierarchies or property applicability (e.g., confirm that
cat:EbikeCargoBike is a subclass of cat:CargoBike before calling listCargoBikes).

In your final answer, populate the reasoning block with real URIs from the ontology
above. Every concept in mapped_concepts and inferences_used must be a real URI.
If no ontology inference was needed, set inferences_used to an empty list.
"""


def mcp_tool_to_openai(tool) -> dict:
    """Convert an MCP Tool object to the OpenAI API tool dict format."""
    description = tool.description or ""
    sem = (tool.model_extra or {}).get("x-semantic") if hasattr(tool, "model_extra") else None
    if sem:
        description += (
            f"\n[x-semantic: ontology={sem.get('ontology', '')},"
            f" operatesOn={sem.get('operatesOn', '')},"
            f" returns={sem.get('returns', '')}]"
        )
    return {
        "type": "function",
        "function": {
            "name": tool.name,
            "description": description,
            "parameters": tool.inputSchema,
        }
    }


def _parse_structured_response(text: str) -> tuple[dict | None, str]:
    """Extract {"reasoning": {...}, "answer": "..."} from the final text."""
    stripped = re.sub(r"^```(?:json)?\s*", "", text.strip(), flags=re.MULTILINE)
    stripped = re.sub(r"\s*```$", "", stripped.strip(), flags=re.MULTILINE)
    try:
        obj = json.loads(stripped.strip())
        if isinstance(obj, dict) and "reasoning" in obj and "answer" in obj:
            return obj["reasoning"], str(obj["answer"])
    except (json.JSONDecodeError, ValueError):
        pass
    return None, text


def _extract_semantic(description: str) -> dict | None:
    """Extract x-semantic annotation from a tool description string."""
    m = re.search(r'\[x-semantic:\s*ontology=(\S+?),\s*operatesOn=(\S+?),\s*returns=(\S+?)\]', description)
    if m:
        return {"ontology": m.group(1), "operatesOn": m.group(2), "returns": m.group(3)}
    return None


# Subclass inferences extracted from the ontology TTL files at startup.
# Maps superclass short name → [(subClass, superPrefix, subPrefix)].
_ONTOLOGY_INFERENCES: dict[str, list[tuple[str, str, str]]] = {}


def _scan_ontology_inferences() -> dict[str, list[tuple[str, str, str]]]:
    """Parse ontology TTL files for rdfs:subClassOf relationships between domain concepts."""
    result: dict[str, list[tuple[str, str, str]]] = {}
    for ttl_file in _ONTOLOGY_FILES:
        path = _ONTOLOGY_DIR / ttl_file
        try:
            text = path.read_text(encoding="utf-8")
        except FileNotFoundError:
            continue
        for m in re.finditer(
                r'([\w-]+):(\w+)\s+a\s+rdfs:Class\s*;\s*rdfs:subClassOf\s+([\w-]+):(\w+)',
                text,
                re.MULTILINE,
        ):
            subPrefix, subClass, supPrefix, superClass = m.groups()
            result.setdefault(superClass, []).append((subClass, supPrefix, subPrefix))
    return result


_ONTOLOGY_INFERENCES = _scan_ontology_inferences()


def _build_reasoning(messages: list[dict], openai_tools: list[dict], user_input: str) -> dict | None:
    """Construct a reasoning trace from the tool calls that actually occurred."""
    tool_map = {t["function"]["name"]: t for t in openai_tools}
    tools_selected = []
    concepts: list[str] = []
    for msg in messages:
        if msg.get("role") == "assistant" and msg.get("tool_calls"):
            for tc in msg["tool_calls"]:
                name = tc["function"]["name"]
                sem = _extract_semantic(tool_map.get(name, {}).get("function", {}).get("description", ""))
                entry = {"tool": name}
                if sem:
                    entry[
                        "justified_by"] = f"{sem['ontology']} — tool operatesOn {sem['operatesOn']}, returns {sem['returns']}"
                    for uri in (sem["ontology"], sem["operatesOn"], sem["returns"]):
                        if uri and uri not in concepts:
                            concepts.append(uri)
                tools_selected.append(entry)
    if not tools_selected:
        return None
    inferences: list[str] = []
    for sup, subs in _ONTOLOGY_INFERENCES.items():
        for subClass, supPrefix, subPrefix in subs:
            if any(subClass in c or sup in c for c in concepts):
                inferences.append(f"{subPrefix}:{subClass} rdfs:subClassOf {supPrefix}:{sup}")
    return {
        "user_intent": user_input,
        "mapped_concepts": concepts,
        "inferences_used": inferences,
        "tools_selected": tools_selected,
    }


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
        openai_tools: list[dict],
        messages: list[dict],
        client: OpenAI,
        model: str,
        user_input: str,
) -> str:
    """Drive an agentic tool-use loop with Generative Engine."""
    while True:
        response = client.chat.completions.create(
            model=model,
            max_tokens=2048,
            messages=messages,
            tools=openai_tools if openai_tools else None,
            tool_choice="auto" if openai_tools else None,
        )

        message = response.choices[0].message
        if message.tool_calls:
            messages.append({
                "role": "assistant",
                "content": message.content,
                "tool_calls": [
                    {
                        "id": tc.id,
                        "type": "function",
                        "function": {
                            "name": tc.function.name,
                            "arguments": tc.function.arguments
                        }
                    }
                    for tc in message.tool_calls
                ]
            })

            for tool_call in message.tool_calls:
                function_name = tool_call.function.name
                function_args = json.loads(tool_call.function.arguments)

                print(f"  [tool] {function_name}({json.dumps(function_args)})")

                mcp_result = await session.call_tool(function_name, function_args)

                texts = [
                    c.text if hasattr(c, "text") else str(c)
                    for c in mcp_result.content
                ]

                messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call.id,
                    "content": json.dumps(texts)
                })
        else:
            raw = message.content or ""
            reasoning, answer = _parse_structured_response(raw)
            if reasoning is None:
                reasoning = _build_reasoning(messages, openai_tools, user_input)
            if reasoning:
                _print_reasoning(reasoning)
            return answer


async def main() -> None:
    missing = [f for f in _ONTOLOGY_FILES if not (_ONTOLOGY_DIR / f).exists()]
    if missing:
        sys.exit(f"Ontology file(s) not found in {_ONTOLOGY_DIR}: {', '.join(missing)}")
    if not MCP_JAR.exists():
        sys.exit(
            f"MCP JAR not found: {MCP_JAR}\n"
            "Build it first:  cd ../cargobike-mcp-starter && mvn -q package"
        )

    # Load configuration from environment variables
    api_key = os.getenv("LLM_API_KEY")
    base_url = os.getenv("LLM_BASE_URL")
    model = os.getenv("LLM_MODEL")

    if not api_key:
        sys.exit("LLM_API_KEY not found in environment variables. Check your .env file.")
    if not base_url:
        sys.exit("LLM_BASE_URL not found in environment variables. Check your .env file.")
    if not model:
        sys.exit("LLM_MODEL not found in environment variables. Check your .env file.")

    ontology = "\n\n".join(
        (_ONTOLOGY_DIR / f).read_text(encoding="utf-8") for f in _ONTOLOGY_FILES
    )
    system_prompt = build_system_prompt(ontology)

    server_params = StdioServerParameters(
        command="java",
        args=["--sun-misc-unsafe-memory-access=allow", "-jar", str(MCP_JAR)],
    )

    client = OpenAI(
        base_url=base_url,
        api_key=api_key
    )

    print(f"Cargo Bike Assistant (Powered by Generative Engine)")
    print(f"Using model: {model}")
    print(f"Endpoint: {base_url}\n")

    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools_response = await session.list_tools()
            openai_tools = [mcp_tool_to_openai(t) for t in tools_response.tools]
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

                messages = [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_input}
                ]
                answer = await run_agent(
                    session, openai_tools, messages, client, model, user_input
                )
                print(f"\nAssistant: {answer}\n")


if __name__ == "__main__":
    asyncio.run(main())
