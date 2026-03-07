# Architecture: Ontology-Grounded Explainable AI Chatbot

## Prompt
What would be a good architecture to implement a chatbot demonstrating explainable AI based on the use of ontologies?

## Core idea

The ontology shouldn't just be context injected into the system prompt. It should be an **active reasoning component** whose influence on each decision is traceable and surfaceable to the user.

---

## Layers

```
┌─────────────────────────────────────────────────────┐
│  Presentation Layer                                 │
│  Chat UI  |  Reasoning Panel  |  Ontology Explorer  │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│  Explanation Layer                                  │
│  Structured reasoning trace (ontology URIs,         │
│  concept mappings, inferences used)                 │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│  Agentic LLM Layer  (Claude)                        │
│  - NL → ontology concept mapping                    │
│  - Tool selection justified by domain/range         │
│  - Produces answer + reasoning block separately     │
└───────────┬────────────────────┬────────────────────┘
            │                    │
┌───────────▼──────────┐  ┌─────▼──────────────────────┐
│  Ontology / SPARQL   │  │  MCP Tool Layer             │
│  Tool (active query) │  │  (existing MCP server)      │
│  Apache Jena /       │  │  Tools annotated with       │
│  Oxigraph triplestore│  │  ontology domain+range      │
└──────────────────────┘  └────────────────────────────┘
```

---

## Key design decisions

### 1. Ontology as an active MCP tool (not just prompt context)

Add a `queryOntology(sparql)` tool to the MCP server backed by a triplestore (Apache Jena or Oxigraph). The LLM can then:

- Look up what `cb:EbikeCargoBike` is a subclass of before choosing a tool
- Discover which properties apply to a class dynamically
- Generate SPARQL that is **auditable** — you can log exactly what was asked

This makes the ontology a live knowledge source, not a static hint.

### 2. Structured reasoning trace alongside the answer

Instead of one response, Claude produces two outputs per turn:

```json
{
  "reasoning": {
    "user_intent": "find a heavy cargo electric bike",
    "mapped_concepts": ["cb:EbikeCargoBike", "cb:hasMaxPayloadKg"],
    "inferences_used": ["cb:EbikeCargoBike rdfs:subClassOf cb:CargoBike"],
    "tools_selected": [
      { "tool": "listCargoBikes", "justified_by": "cb:CargoBike domain" }
    ]
  },
  "answer": "Here are the electric cargo bikes we have..."
}
```

The `reasoning` block is shown in a separate panel — that's the XAI artifact.

### 3. Tool annotations tied to ontology

Your OpenAPI specs already use `x-semantic`. Extend them so each tool declares its ontology domain/range explicitly:

```yaml
x-semantic:
  ontology: https://example.com/ont/cargobike#
  operatesOn: cb:CargoBike
  returns: cb:EbikeCargoBike
```

The MCP server exposes these annotations as part of tool metadata. Claude uses them to justify tool selection formally.

### 4. Inference as a first-class citizen

With a real OWL reasoner (Jena + OWL-Mini or HermiT) you can demonstrate:

- **Subclass inference**: "e-bike" → `cb:EbikeCargoBike` → `cb:CargoBike` → `listCargoBikes` is valid
- **Property inheritance**: `cb:hasSku` applies to `cb:EbikeCargoBike` because it applies to `cb:CargoBike`
- **Constraint validation**: A payload filter of 500 kg violates `cb:hasMaxPayloadKg` range → proactively explain why no results match

These inferences are shown verbatim in the reasoning panel.

---

## Implementation path (fits existing stack)

| Phase | What | Fits existing code |
|---|---|---|
| 1 | Add `queryOntology` SPARQL tool to Quarkus MCP server (Apache Jena embedded) | New `@Tool` method |
| 2 | Extend system prompt to produce structured `reasoning` + `answer` blocks | `build_system_prompt()` |
| 3 | Parse reasoning block in `chatbot.py`, display separately | `run_agent()` |
| 4 | Add OWL reasoner to Jena for subclass/property inference | Jena `OntModel` |
| 5 | Web UI with split chat/reasoning panes | Separate frontend |

---

## What makes it genuinely "explainable"

The key distinction is that the explanation must be **grounded in the formal ontology**, not just generated prose. That means:

- Every concept mentioned in a reasoning block is a real ontology URI
- Inference steps are derivable by running the ontology through a reasoner independently
- The SPARQL queries Claude generates can be re-run by the user to verify results
- The chain `user words → ontology class → tool → API call → answer` is fully logged and displayable

This is different from just saying "I chose this tool because the user asked about bikes" — that's just LLM self-narration, not XAI.

---

The biggest architectural investment is the SPARQL/reasoning tool on the MCP side — everything else extends what you already have in small steps.
