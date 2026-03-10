
package com.example.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.logging.Logger;

public class ToolsRegistry {

  private static final Logger LOG = Logger.getLogger(ToolsRegistry.class.getName());

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
  private static final String TOOL_LIST_CARGO_BIKES = "lcb";
  private static final String TOOL_LIST_ORDERS = "lo";
  private static final String TOOL_GET_BIKE_BY_SKU = "gbbs";
  private static final String TOOL_GET_CUSTOMER = "gc";
  private static final String TOOL_GET_INVENTORY_BY_SKU = "giby";
  private static final String TOOL_GET_ORDER = "go";
  private static final String TOOL_GET_SHIPMENT_QUOTE = "gsq";

  public Map<String, Object> list() {
    return Map.of(
      "tools", List.of(
        Map.of(
          "name", TOOL_LIST_CARGO_BIKES,
          "inputSchema", Map.of("type", "object", "properties", Map.of())
        ),
        Map.of(
          "name", TOOL_LIST_ORDERS,
          "inputSchema", Map.of("type", "object", "properties", Map.of())
        ),
        Map.of(
          "name", TOOL_GET_BIKE_BY_SKU,
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "sku", Map.of("type", "string")),
            "required", List.of("sku")
          )
        ),
        Map.of(
          "name", TOOL_GET_CUSTOMER,
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "customerId", Map.of("type", "string")),
            "required", List.of("customerId")
          )
        ),
        Map.of(
          "name", TOOL_GET_INVENTORY_BY_SKU,
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "sku", Map.of("type", "string")),
            "required", List.of("sku")
          )
        ),
        Map.of(
          "name", TOOL_GET_ORDER,
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "orderId", Map.of("type", "string")),
            "required", List.of("orderId")
          )
        ),
        Map.of(
          "name", TOOL_GET_SHIPMENT_QUOTE,
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "postalCode", Map.of("type", "string"),
              "countryCode", Map.of("type", "string"),
              "weightKg", Map.of("type", "number")
            ),
            "required", List.of("postalCode", "weightKg")
          )
        )
      )
    );
  }

  public Object call(String name, JsonNode args) {
    LOG.info("Tool called: " + name + " args=" + args);
    return switch (name) {
      case TOOL_LIST_ORDERS -> Map.of(
        "@type", "Collection",
        "items", ORDERS
      );
      case TOOL_LIST_CARGO_BIKES -> Map.of(
        "@type", "Collection",
        "items", CATALOG
      );
      case TOOL_GET_BIKE_BY_SKU -> {
        String sku = args.path("sku").asText();
        yield CATALOG.stream()
          .filter(b -> sku.equals(b.get("cb:hasSku")))
          .findFirst()
          .orElse(Map.of("error", "No bike found with SKU: " + sku));
      }
      case TOOL_GET_CUSTOMER -> Map.of(
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
      case TOOL_GET_INVENTORY_BY_SKU -> {
        String sku = args.path("sku").asText();
        yield INVENTORY.stream()
          .filter(i -> sku.equals(i.get("cb:hasSku")))
          .findFirst()
          .orElse(Map.of("error", "No inventory found for SKU: " + sku));
      }
      case TOOL_GET_ORDER -> {
        String orderId = args.path("orderId").asText();
        yield ORDERS.stream()
          .filter(o -> orderId.equals(o.get("cb:orderId")))
          .findFirst()
          .orElse(Map.of("error", "No order found with ID: " + orderId));
      }
      case TOOL_GET_SHIPMENT_QUOTE -> {
        double weight = args.path("weightKg").asDouble(40.0);
        double price = 49.90 + Math.max(0, weight - 20) * 1.2;
        yield Map.of(
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
