
package com.example.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;

import java.io.InputStream;
import java.util.*;

public class ToolsRegistry {

  private static final String ONTOLOGY_URL = "https://example.com/ont/cargobike#";

  private static final OntModel ONTOLOGY_MODEL = buildOntologyModel();

  private static OntModel buildOntologyModel() {
    final OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    try (final InputStream is = ToolsRegistry.class.getResourceAsStream("/assets/ontology/cargobike.ttl")) {
      if (is != null) {
        m.read(is, ONTOLOGY_URL, "TTL");
      }
    } catch (Exception e) {
      // fall through – empty model
    }
    return m;
  }

  private static final List<Map<String, Object>> CATALOG = List.of(
    Map.of(
      "@type", "cb:CargoBike",
      "cb:hasSku", "SKU-CB-001",
      "cb:modelName", "CargoMaster 500",
      "cb:hasWeightKg", 38.5,
      "cb:hasMaxPayloadKg", 200,
      "cb:hasWheelCount", 2
    ),
    Map.of(
      "@type", "cb:EbikeCargoBike",
      "cb:hasSku", "SKU-ECB-900",
      "cb:modelName", "E-Cargo Pro",
      "cb:hasWeightKg", 42.0,
      "cb:hasMaxPayloadKg", 220,
      "cb:hasWheelCount", 3,
      "cb:hasBatteryCapacityWh", 750
    ),
    Map.of(
      "@type", "cb:CargoBike",
      "cb:hasSku", "SKU-CB-002",
      "cb:modelName", "UrbanHauler 300",
      "cb:hasWeightKg", 34.0,
      "cb:hasMaxPayloadKg", 150,
      "cb:hasWheelCount", 2
    )
  );

  private static final List<Map<String, Object>> ORDERS = List.of(
    Map.of(
      "@type", "cb:Order",
      "cb:orderId", "ORD-001",
      "cb:orderedBy", Map.of(
        "@type", "cb:Customer",
        "cb:customerId", "CUST-123",
        "cb:email", "alex@example.com"
      ),
      "cb:hasStatus", "PROCESSING",
      "cb:hasItem", List.of(Map.of(
        "@type", "cb:OrderItem",
        "cb:hasSku", "SKU-ECB-900",
        "cb:quantity", 1,
        "cb:hasUnitPrice", Map.of("@type", "cb:Price", "cb:amount", 4299.0, "cb:currency", "EUR")
      )),
      "cb:hasTotalPrice", Map.of("@type", "cb:Price", "cb:amount", 4299.0, "cb:currency", "EUR")
    ),
    Map.of(
      "@type", "cb:Order",
      "cb:orderId", "ORD-002",
      "cb:orderedBy", Map.of(
        "@type", "cb:Customer",
        "cb:customerId", "CUST-456",
        "cb:email", "maria@example.com"
      ),
      "cb:hasStatus", "SHIPPED",
      "cb:hasItem", List.of(Map.of(
        "@type", "cb:OrderItem",
        "cb:hasSku", "SKU-CB-001",
        "cb:quantity", 2,
        "cb:hasUnitPrice", Map.of("@type", "cb:Price", "cb:amount", 1899.0, "cb:currency", "EUR")
      )),
      "cb:hasTotalPrice", Map.of("@type", "cb:Price", "cb:amount", 3798.0, "cb:currency", "EUR")
    ),
    Map.of(
      "@type", "cb:Order",
      "cb:orderId", "ORD-003",
      "cb:orderedBy", Map.of(
        "@type", "cb:Customer",
        "cb:customerId", "CUST-789",
        "cb:email", "lars@example.com"
      ),
      "cb:hasStatus", "PAID",
      "cb:hasItem", List.of(Map.of(
        "@type", "cb:OrderItem",
        "cb:hasSku", "SKU-CB-002",
        "cb:quantity", 1,
        "cb:hasUnitPrice", Map.of("@type", "cb:Price", "cb:amount", 1499.0, "cb:currency", "EUR")
      )),
      "cb:hasTotalPrice", Map.of("@type", "cb:Price", "cb:amount", 1499.0, "cb:currency", "EUR")
    )
  );

  private static final List<Map<String, Object>> INVENTORY = List.of(
    Map.of(
      "@type", "cb:InventoryItem",
      "cb:hasSku", "SKU-CB-001",
      "cb:hasQuantity", 7,
      "cb:warehouseCode", "NUE-01"
    ),
    Map.of(
      "@type", "cb:InventoryItem",
      "cb:hasSku", "SKU-ECB-900",
      "cb:hasQuantity", 3,
      "cb:warehouseCode", "NUE-01"
    ),
    Map.of(
      "@type", "cb:InventoryItem",
      "cb:hasSku", "SKU-CB-002",
      "cb:hasQuantity", 0,
      "cb:warehouseCode", "NUE-01"
    )
  );

  public Map<String, Object> list() {
    return Map.of(
      "tools", List.of(
        Map.of(
          "name", "listCargoBikes",
          "description", "Return the full catalog of available cargo bikes.",
          "x-semantic", getOntologyResult("cb:CargoBike", "cb:CargoBike"),
          "inputSchema", Map.of("type", "object", "properties", Map.of())
        ),
        Map.of(
          "name", "listOrders",
          "description", "Return the full list of orders.",
          "x-semantic", getOntologyResult("cb:Order", "cb:Order"),
          "inputSchema", Map.of("type", "object", "properties", Map.of())
        ),
        Map.of(
          "name", "getBikeBySku",
          "description", "Return catalog info for a single cargo bike by SKU.",
          "x-semantic", getOntologyResult("cb:CargoBike", "cb:CargoBike"),
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "sku", Map.of("type", "string", "x-semantic", getOntologyProperty("cb:hasSku"))),
            "required", List.of("sku")
          )
        ),
        Map.of(
          "name", "getCustomer",
          "description", "Return customer details by customer ID (format: CUST-{number}).",
          "x-semantic", getOntologyResult("cb:Customer", "cb:Customer"),
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "customerId", Map.of("type", "string", "x-semantic", getOntologyProperty("cb:customerId"))),
            "required", List.of("customerId")
          )
        ),
        Map.of(
          "name", "getInventoryBySku",
          "description", "Return stock/inventory levels for a cargo bike by SKU.",
          "x-semantic", getOntologyResult("cb:CargoBike", "cb:InventoryItem"),
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "sku", Map.of("type", "string", "x-semantic", getOntologyProperty("cb:hasSku"))),
            "required", List.of("sku")
          )
        ),
        Map.of(
          "name", "getOrder",
          "description", "Return order details by order ID (format: ORD-{number}).",
          "x-semantic", getOntologyResult("cb:Order", "cb:Order"),
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "orderId", Map.of("type", "string", "x-semantic", getOntologyProperty("cb:orderId"))),
            "required", List.of("orderId")
          )
        ),
        Map.of(
          "name", "getShipmentQuote",
          "description", "Return a shipping quote for a given weight and destination.",
          "x-semantic", getOntologyResult("cb:Address", "cb:ShipmentQuote"),
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "postalCode", Map.of("type", "string", "x-semantic", getOntologyProperty("cb:postalCode")),
              "countryCode", Map.of("type", "string", "x-semantic", getOntologyProperty("cb:countryCode")),
              "weightKg", Map.of("type", "number", "x-semantic", getOntologyProperty("cb:hasWeightKg"))
            ),
            "required", List.of("postalCode", "weightKg")
          )
        ),
        Map.of(
          "name", "queryOntology",
          "description", "Execute a SPARQL SELECT query against the cargo bike domain ontology "
            + "(Apache Jena, OWL-micro inference enabled). Use this to look up class hierarchies, "
            + "discover applicable properties for a concept, and verify subclass/range inferences "
            + "before choosing other tools. The prefix cb: is bound to " + ONTOLOGY_URL + ".",
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "sparql", Map.of(
                "type", "string",
                "description", "A SPARQL 1.1 SELECT query. Use PREFIX cb: <" + ONTOLOGY_URL + "> in the query."
              )
            ),
            "required", List.of("sparql")
          )
        )
      )
    );
  }

  private static Map<String, String> getOntologyProperty(String property) {
    return Map.of("ontology", ONTOLOGY_URL, "property", property);
  }

  private static Map<String, String> getOntologyResult(String operatesOn, String returns) {
    return Map.of("ontology", ONTOLOGY_URL, "operatesOn", operatesOn, "returns", returns);
  }

  public Object call(String name, JsonNode args) {
    Map<String, Object> ctx = Map.of("@vocab", ONTOLOGY_URL, "cb", ONTOLOGY_URL);
    return switch (name) {
      case "listOrders" -> Map.of(
        "@context", ctx,
        "@type", "Collection",
        "items", ORDERS
      );
      case "listCargoBikes" -> Map.of(
        "@context", ctx,
        "@type", "Collection",
        "items", CATALOG
      );
      case "getBikeBySku" -> {
        String sku = args.path("sku").asText();
        yield CATALOG.stream()
          .filter(b -> sku.equals(b.get("cb:hasSku")))
          .map(b -> {
            Map<String, Object> result = new LinkedHashMap<>(b);
            result.put("@context", ctx);
            return (Object) result;
          })
          .findFirst()
          .orElse(Map.of("error", "No bike found with SKU: " + sku));
      }
      case "getCustomer" -> Map.of(
        "@context", ctx,
        "@type", "cb:Customer",
        "cb:customerId", args.path("customerId").asText(),
        "cb:fullName", "Alex Müller",
        "cb:email", "alex@example.com",
        "cb:hasAddress", Map.of(
          "@type", "cb:Address",
          "cb:street", "Hauptstraße 1",
          "cb:city", "Nürnberg",
          "cb:postalCode", "90402",
          "cb:countryCode", "DE"
        )
      );
      case "getInventoryBySku" -> {
        String sku = args.path("sku").asText();
        yield INVENTORY.stream()
          .filter(i -> sku.equals(i.get("cb:hasSku")))
          .map(i -> {
            Map<String, Object> result = new LinkedHashMap<>(i);
            result.put("@context", ctx);
            return (Object) result;
          })
          .findFirst()
          .orElse(Map.of("error", "No inventory found for SKU: " + sku));
      }
      case "getOrder" -> {
        String orderId = args.path("orderId").asText();
        yield ORDERS.stream()
          .filter(o -> orderId.equals(o.get("cb:orderId")))
          .map(o -> {
            Map<String, Object> result = new LinkedHashMap<>(o);
            result.put("@context", ctx);
            return (Object) result;
          })
          .findFirst()
          .orElse(Map.of("error", "No order found with ID: " + orderId));
      }
      case "getShipmentQuote" -> {
        double weight = args.path("weightKg").asDouble(40.0);
        double price = 49.90 + Math.max(0, weight - 20) * 1.2;
        yield Map.of(
          "@context", ctx,
          "@type", "cb:ShipmentQuote",
          "cb:shipsTo", Map.of(
            "@type", "cb:Address",
            "cb:postalCode", args.path("postalCode").asText("00000"),
            "cb:countryCode", args.path("countryCode").asText("DE")
          ),
          "cb:totalWeightKg", weight,
          "cb:hasTotalPrice", Map.of("@type", "cb:Price", "cb:amount", round(price), "cb:currency", "EUR"),
          "cb:estimatedDays", 3
        );
      }
      case "queryOntology" -> {
        String sparql = args.path("sparql").asText();
        try {
          Query query = QueryFactory.create(sparql);
          try (QueryExecution qe = QueryExecutionFactory.create(query, ONTOLOGY_MODEL)) {
            ResultSet rs = qe.execSelect();
            List<String> vars = rs.getResultVars();
            List<Map<String, String>> rows = new ArrayList<>();
            while (rs.hasNext()) {
              QuerySolution sol = rs.nextSolution();
              Map<String, String> row = new LinkedHashMap<>();
              for (String v : vars) {
                RDFNode n = sol.get(v);
                row.put(v, n != null ? n.toString() : null);
              }
              rows.add(row);
            }
            yield Map.of("columns", vars, "rows", rows);
          }
        } catch (Exception e) {
          yield Map.of("error", "SPARQL error: " + e.getMessage());
        }
      }
      default -> Map.of("error", "Unknown tool: " + name);
    };
  }

  private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
