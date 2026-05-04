
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

  private static final String ONT_BASE = "https://example.com/ont/cargobike";
  private static final String NS_CAT   = ONT_BASE + "/catalog#";
  private static final String NS_CUS   = ONT_BASE + "/customers#";
  private static final String NS_INV   = ONT_BASE + "/inventory#";
  private static final String NS_ORD   = ONT_BASE + "/orders#";
  private static final String NS_SHP   = ONT_BASE + "/shipping#";

  private static final Map<String, Object> CONTEXT = Map.of(
    "cat", NS_CAT,
    "cus", NS_CUS,
    "inv", NS_INV,
    "ord", NS_ORD,
    "shp", NS_SHP
  );

  private static final OntModel ONTOLOGY_MODEL = buildOntologyModel();

  private static OntModel buildOntologyModel() {
    final OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    try (final InputStream is = ToolsRegistry.class.getResourceAsStream("/assets/ontology/cargobike.ttl")) {
      if (is != null) {
        m.read(is, ONT_BASE, "TTL");
      }
    } catch (Exception e) {
      // fall through – empty model
    }
    return m;
  }

  private static final List<Map<String, Object>> CATALOG = List.of(
    Map.of(
      "@type", "cat:CargoBike",
      "cat:hasSku", "SKU-CB-001",
      "cat:modelName", "CargoMaster 500",
      "cat:hasWeightKg", 38.5,
      "cat:hasMaxPayloadKg", 200,
      "cat:hasWheelCount", 2
    ),
    Map.of(
      "@type", "cat:EbikeCargoBike",
      "cat:hasSku", "SKU-ECB-900",
      "cat:modelName", "E-Cargo Pro",
      "cat:hasWeightKg", 42.0,
      "cat:hasMaxPayloadKg", 220,
      "cat:hasWheelCount", 3,
      "cat:hasBatteryCapacityWh", 750
    ),
    Map.of(
      "@type", "cat:CargoBike",
      "cat:hasSku", "SKU-CB-002",
      "cat:modelName", "UrbanHauler 300",
      "cat:hasWeightKg", 34.0,
      "cat:hasMaxPayloadKg", 150,
      "cat:hasWheelCount", 2
    )
  );

  private static final List<Map<String, Object>> ORDERS = List.of(
    Map.of(
      "@type", "ord:Order",
      "ord:orderId", "ORD-001",
      "ord:orderedBy", Map.of(
        "@type", "cus:Customer",
        "cus:customerId", "BLUBB-123",
        "cus:email", "alex@example.com"
      ),
      "ord:hasStatus", "PROCESSING",
      "ord:hasItem", List.of(Map.of(
        "@type", "ord:OrderItem",
        "cat:hasSku", "SKU-ECB-900",
        "ord:quantity", 1,
        "ord:hasUnitPrice", Map.of("@type", "ord:Price", "ord:amount", 4299.0, "ord:currency", "EUR")
      )),
      "ord:hasTotalPrice", Map.of("@type", "ord:Price", "ord:amount", 4299.0, "ord:currency", "EUR")
    ),
    Map.of(
      "@type", "ord:Order",
      "ord:orderId", "ORD-002",
      "ord:orderedBy", Map.of(
        "@type", "cus:Customer",
        "cus:customerId", "BLUBB-456",
        "cus:email", "maria@example.com"
      ),
      "ord:hasStatus", "SHIPPED",
      "ord:hasItem", List.of(Map.of(
        "@type", "ord:OrderItem",
        "cat:hasSku", "SKU-CB-001",
        "ord:quantity", 2,
        "ord:hasUnitPrice", Map.of("@type", "ord:Price", "ord:amount", 1899.0, "ord:currency", "EUR")
      )),
      "ord:hasTotalPrice", Map.of("@type", "ord:Price", "ord:amount", 3798.0, "ord:currency", "EUR")
    ),
    Map.of(
      "@type", "ord:Order",
      "ord:orderId", "ORD-003",
      "ord:orderedBy", Map.of(
        "@type", "cus:Customer",
        "cus:customerId", "BLUBB-789",
        "cus:email", "lars@example.com"
      ),
      "ord:hasStatus", "PAID",
      "ord:hasItem", List.of(Map.of(
        "@type", "ord:OrderItem",
        "cat:hasSku", "SKU-CB-002",
        "ord:quantity", 1,
        "ord:hasUnitPrice", Map.of("@type", "ord:Price", "ord:amount", 1499.0, "ord:currency", "EUR")
      )),
      "ord:hasTotalPrice", Map.of("@type", "ord:Price", "ord:amount", 1499.0, "ord:currency", "EUR")
    )
  );

  private static final List<Map<String, Object>> INVENTORY = List.of(
    Map.of(
      "@type", "inv:InventoryItem",
      "cat:hasSku", "SKU-CB-001",
      "inv:hasQuantity", 7,
      "inv:warehouseCode", "NUE-01"
    ),
    Map.of(
      "@type", "inv:InventoryItem",
      "cat:hasSku", "SKU-ECB-900",
      "inv:hasQuantity", 3,
      "inv:warehouseCode", "NUE-01"
    ),
    Map.of(
      "@type", "inv:InventoryItem",
      "cat:hasSku", "SKU-CB-002",
      "inv:hasQuantity", 0,
      "inv:warehouseCode", "NUE-01"
    )
  );

  public Map<String, Object> list() {
    return Map.of(
      "tools", List.of(
        Map.of(
          "name", "listCargoBikes",
          "x-semantic", getOntologyResult("cat:CargoBike", "cat:CargoBike"),
          "inputSchema", Map.of("type", "object", "properties", Map.of())
        ),
        Map.of(
          "name", "listOrders",
          "x-semantic", getOntologyResult("ord:Order", "ord:Order"),
          "inputSchema", Map.of("type", "object", "properties", Map.of())
        ),
        Map.of(
          "name", "getBikeBySku",
          "x-semantic", getOntologyResult("cat:CargoBike", "cat:CargoBike"),
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "sku", Map.of("type", "string", "x-semantic", getOntologyProperty("cat:hasSku"))),
            "required", List.of("sku")
          )
        ),
        Map.of(
          "name", "getCustomer",
          "x-semantic", getOntologyResult("cus:Customer", "cus:Customer"),
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "customerId", Map.of("type", "string", "x-semantic", getOntologyProperty("cus:customerId"))),
            "required", List.of("customerId")
          )
        ),
        Map.of(
          "name", "getInventoryBySku",
          "x-semantic", getOntologyResult("cat:CargoBike", "inv:InventoryItem"),
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "sku", Map.of("type", "string", "x-semantic", getOntologyProperty("cat:hasSku"))),
            "required", List.of("sku")
          )
        ),
        Map.of(
          "name", "getOrder",
          "x-semantic", getOntologyResult("ord:Order", "ord:Order"),
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "orderId", Map.of("type", "string", "x-semantic", getOntologyProperty("ord:orderId"))),
            "required", List.of("orderId")
          )
        ),
        Map.of(
          "name", "getShipmentQuote",
          "x-semantic", getOntologyResult("cus:Address", "shp:ShipmentQuote"),
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "postalCode", Map.of("type", "string", "x-semantic", getOntologyProperty("cus:postalCode")),
              "countryCode", Map.of("type", "string", "x-semantic", getOntologyProperty("cus:countryCode")),
              "weightKg", Map.of("type", "number", "x-semantic", getOntologyProperty("cat:hasWeightKg"))
            ),
            "required", List.of("postalCode", "weightKg")
          )
        ),
        Map.of(
          "name", "queryOntology",
          "description", "Execute a SPARQL SELECT query against the cargo bike domain ontology "
            + "(Apache Jena, OWL-micro inference enabled). Use this to look up class hierarchies, "
            + "discover applicable properties for a concept, and verify subclass/range inferences "
            + "before choosing other tools. "
            + "Available prefixes: "
            + "cat: <" + NS_CAT + ">, "
            + "cus: <" + NS_CUS + ">, "
            + "inv: <" + NS_INV + ">, "
            + "ord: <" + NS_ORD + ">, "
            + "shp: <" + NS_SHP + ">.",
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "sparql", Map.of(
                "type", "string",
                "description", "A SPARQL 1.1 SELECT query. Declare needed prefixes, e.g.: "
                  + "PREFIX cat: <" + NS_CAT + "> "
                  + "PREFIX cus: <" + NS_CUS + "> "
                  + "PREFIX inv: <" + NS_INV + "> "
                  + "PREFIX ord: <" + NS_ORD + "> "
                  + "PREFIX shp: <" + NS_SHP + ">"
              )
            ),
            "required", List.of("sparql")
          )
        )
      )
    );
  }

  private static Map<String, String> getOntologyProperty(String property) {
    return Map.of("ontology", ONT_BASE, "property", property);
  }

  private static Map<String, String> getOntologyResult(String operatesOn, String returns) {
    return Map.of("ontology", ONT_BASE, "operatesOn", operatesOn, "returns", returns);
  }

  public Object call(String name, JsonNode args) {
    return switch (name) {
      case "listOrders" -> Map.of(
        "@context", CONTEXT,
        "@type", "Collection",
        "items", ORDERS
      );
      case "listCargoBikes" -> Map.of(
        "@context", CONTEXT,
        "@type", "Collection",
        "items", CATALOG
      );
      case "getBikeBySku" -> {
        String sku = args.path("sku").asText();
        yield CATALOG.stream()
          .filter(b -> sku.equals(b.get("cat:hasSku")))
          .map(b -> {
            Map<String, Object> result = new LinkedHashMap<>(b);
            result.put("@context", CONTEXT);
            return (Object) result;
          })
          .findFirst()
          .orElse(Map.of("error", "No bike found with SKU: " + sku));
      }
      case "getCustomer" -> Map.of(
        "@context", CONTEXT,
        "@type", "cus:Customer",
        "cus:customerId", args.path("customerId").asText(),
        "cus:fullName", "Alex Müller",
        "cus:email", "alex@example.com",
        "cus:hasAddress", Map.of(
          "@type", "cus:Address",
          "cus:street", "Hauptstraße 1",
          "cus:city", "Nürnberg",
          "cus:postalCode", "90402",
          "cus:countryCode", "DE"
        )
      );
      case "getInventoryBySku" -> {
        String sku = args.path("sku").asText();
        yield INVENTORY.stream()
          .filter(i -> sku.equals(i.get("cat:hasSku")))
          .map(i -> {
            Map<String, Object> result = new LinkedHashMap<>(i);
            result.put("@context", CONTEXT);
            return (Object) result;
          })
          .findFirst()
          .orElse(Map.of("error", "No inventory found for SKU: " + sku));
      }
      case "getOrder" -> {
        String orderId = args.path("orderId").asText();
        yield ORDERS.stream()
          .filter(o -> orderId.equals(o.get("ord:orderId")))
          .map(o -> {
            Map<String, Object> result = new LinkedHashMap<>(o);
            result.put("@context", CONTEXT);
            return (Object) result;
          })
          .findFirst()
          .orElse(Map.of("error", "No order found with ID: " + orderId));
      }
      case "getShipmentQuote" -> {
        double weight = args.path("weightKg").asDouble(40.0);
        double price = 49.90 + Math.max(0, weight - 20) * 1.2;
        yield Map.of(
          "@context", CONTEXT,
          "@type", "shp:ShipmentQuote",
          "shp:shipsTo", Map.of(
            "@type", "cus:Address",
            "cus:postalCode", args.path("postalCode").asText("00000"),
            "cus:countryCode", args.path("countryCode").asText("DE")
          ),
          "cat:hasWeightKg", weight,
          "ord:hasTotalPrice", Map.of("@type", "ord:Price", "ord:amount", round(price), "ord:currency", "EUR"),
          "shp:estimatedDays", 3
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