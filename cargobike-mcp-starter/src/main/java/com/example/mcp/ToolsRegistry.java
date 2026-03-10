
package com.example.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

public class ToolsRegistry {
  public Map<String, Object> list() {
    return Map.of(
      "tools", List.of(
        Map.of(
          "name", "getBikeBySku",
          "description", "Return catalog info for a cargo bike by SKU.",
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of("sku", Map.of("type", "string", "x-semantic", "cb:hasSku")),
            "required", List.of("sku")
          )
        ),
        Map.of(
          "name", "getShipmentQuote",
          "description", "Return shipping quote for weight and destination.",
          "inputSchema", Map.of(
            "type", "object",
            "properties", Map.of(
              "postalCode", Map.of("type", "string", "x-semantic", "cb:postalCode"),
              "countryCode", Map.of("type", "string", "x-semantic", "cb:countryCode"),
              "weightKg", Map.of("type", "number", "x-semantic", "cb:hasWeightKg")
            ),
            "required", List.of("postalCode", "weightKg")
          )
        )
      )
    );
  }

  public Object call(String name, JsonNode args) {
    if ("getBikeBySku".equals(name)) {
      String sku = args.path("sku").asText();
      return Map.of(
        "@context", Map.of("@vocab", "https://example.com/ont/cargobike#", "cb", "https://example.com/ont/cargobike#"),
        "@type", "cb:CargoBike",
        "cb:hasSku", sku,
        "cb:modelName", "Demo Bike",
        "cb:hasWeightKg", 40.0,
        "cb:hasMaxPayloadKg", 200,
        "cb:hasWheelCount", 2
      );
    }
    if ("getShipmentQuote".equals(name)) {
      double weight = args.path("weightKg").asDouble(40.0);
      double price = 49.90 + Math.max(0, weight - 20) * 1.2;
      return Map.of(
        "@context", Map.of("@vocab", "https://example.com/ont/cargobike#", "cb", "https://example.com/ont/cargobike#"),
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
    return Map.of("error", "Unknown tool: " + name);
  }

  private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
