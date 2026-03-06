
package com.example.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

public class ToolsRegistry {

  private static final String ONTOLOGY_URL = "https://example.com/ont/cargobike#";

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
    )
  );

  public Map<String, Object> list() {
    return Map.of(
      "tools", List.of(
        Map.of(
          "name", "listCargoBikes",
          "description", "Return the full catalog of available cargo bikes.",
          "inputSchema", Map.of("type", "object", "properties", Map.of())
        ),
        Map.of(
          "name", "getBikeBySku",
          "description", "Return catalog info for a single cargo bike by SKU.",
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "sku", Map.of("type", "string", "x-semantic", getOntology("cb:hasSku"))),
            "required", List.of("sku")
          )
        ),
        Map.of(
          "name", "getCustomer",
          "description", "Return customer details by customer ID (format: CUST-{number}).",
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "customerId", Map.of("type", "string", "x-semantic", getOntology("cb:customerId"))),
            "required", List.of("customerId")
          )
        ),
        Map.of(
          "name", "getInventoryBySku",
          "description", "Return stock/inventory levels for a cargo bike by SKU.",
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "sku", Map.of("type", "string", "x-semantic", getOntology("cb:hasSku"))),
            "required", List.of("sku")
          )
        ),
        Map.of(
          "name", "getOrder",
          "description", "Return order details by order ID (format: ORD-{number}).",
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "orderId", Map.of("type", "string", "x-semantic", getOntology("cb:orderId"))),
            "required", List.of("orderId")
          )
        ),
        Map.of(
          "name", "getShipmentQuote",
          "description", "Return a shipping quote for a given weight and destination.",
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "postalCode", Map.of("type", "string", "x-semantic", getOntology("cb:postalCode")),
              "countryCode", Map.of("type", "string", "x-semantic", getOntology("cb:countryCode")),
              "weightKg", Map.of("type", "number", "x-semantic", getOntology("cb:hasWeightKg"))
            ),
            "required", List.of("postalCode", "weightKg")
          )
        )
      )
    );
  }

  private static Map<String, String> getOntology(String property) {
    return Map.of("ontology", ONTOLOGY_URL, "property", property);
  }

  public Object call(String name, JsonNode args) {
    Map<String, Object> ctx = Map.of("@vocab", ONTOLOGY_URL, "cb", ONTOLOGY_URL);
    return switch (name) {
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
      case "getInventoryBySku" -> Map.of(
        "@context", ctx,
        "@type", "cb:InventoryItem",
        "cb:hasSku", args.path("sku").asText(),
        "cb:hasQuantity", 7,
        "cb:warehouseCode", "NUE-01"
      );
      case "getOrder" -> Map.of(
        "@context", ctx,
        "@type", "cb:Order",
        "cb:orderId", args.path("orderId").asText(),
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
      );
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
      default -> Map.of("error", "Unknown tool: " + name);
    };
  }

  private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
