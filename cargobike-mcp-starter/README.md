
# CargoBike MCP Starter Repo

This starter repo demonstrates **ontology-backed API documentation** for an e-commerce site selling cargo bikes. It includes:

- **WireMock mappings** (5 microservices) returning JSON-LD with ontology IRIs
- **OpenAPI 3.1** specs (one per microservice) annotated with `x-semantic`
- **RDFS ontology** (Turtle) for core cargo bike concepts
- **Minimal Quarkus command-mode app** acting as a simple **MCP-like JSON-RPC server over stdio**

## Structure
```
cargobike-mcp-starter/
  ├─ pom.xml
  ├─ README.md
  └─ src/main/
     ├─ java/com/example/mcp/
     │  ├─ App.java
     │  ├─ MCPServer.java
     │  ├─ RpcModels.java
     │  ├─ ToolsRegistry.java
     │  ├─ ResourcesRegistry.java
     │  └─ PromptsRegistry.java
     └─ resources/
        ├─ application.properties
        └─ assets/
           ├─ ontology/cargobike.ttl
           ├─ openapi/*.yaml
           └─ wiremock/mappings/*.json
```

## Prerequisites
- Java 21+
- Apache Maven 3.9+

## Build & Run
```bash
mvn -q package
java --sun-misc-unsafe-memory-access=allow -jar target/quarkus-app/quarkus-run.jar
```

The server reads **JSON-RPC 2.0** requests from stdin and writes responses to stdout.

### Quick test
Open a second terminal and run:
```bash
printf '%s
' '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | java -jar target/quarkus-app/quarkus-run.jar
```
List tools:
```bash
printf '%s
' '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' | java -jar target/quarkus-app/quarkus-run.jar
```
Get bike by SKU:
```bash
printf '%s
' '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"getBikeBySku","arguments":{"sku":"SKU-ECB-900"}}}' | java -jar target/quarkus-app/quarkus-run.jar
```
List bikes:
```bash
printf '%s\n' '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"listCargoBikes","arguments":{}}}' | java -jar
target/quarkus-app/quarkus-run.jar
```
List orders:
```bash
printf '%s\n' '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"listOrders","arguments":{}}}' | java -jar
target/quarkus-app/quarkus-run.jar
```
Get customer:
```bash
printf '%s\n' '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"getCustomer","arguments":{"customerId":"BLUBB-123"}}}' | java -jar
target/quarkus-app/quarkus-run.jar
```
Get inventory:
```bash
printf '%s\n' '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"getInventoryBySku","arguments":{"sku":"SKU-ECB-900"}}}' | java -jar
target/quarkus-app/quarkus-run.jar
```
Get order:
```bash
printf '%s\n' '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"getOrder","arguments":{"orderId":"ORD-1001"}}}' | java -jar
target/quarkus-app/quarkus-run.jar
```

## Notes
- The MCP loop is intentionally minimal to illustrate concepts; extend as needed (error handling, batching, schema validation, etc.).
- The WireMock mappings and OpenAPI specs reference the same ontology IRIs for semantic alignment.
