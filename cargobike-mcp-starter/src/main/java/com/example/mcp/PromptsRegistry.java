
package com.example.mcp;

import java.util.List;
import java.util.Map;

public class PromptsRegistry {
  public Map<String, Object> list() {
    return Map.of(
      "prompts", List.of(
        Map.of("name", "summarizeOrder", "description", "Summarize an order for a customer."),
        Map.of("name", "recommendBike", "description", "Recommend a cargo bike based on payload and wheel count.")
      )
    );
  }

  public Map<String, Object> get(String name) {
    if ("summarizeOrder".equals(name)) {
      return Map.of(
        "name", name,
        "template",
        """
        You are given an Order. Summarize it in 2 sentences for the customer. Be concise and neutral.
        Input JSON:
        {{order_jsonld}}
        """
      );
    }
    if ("recommendBike".equals(name)) {
      return Map.of(
        "name", name,
        "template",
        """
        Recommend a cargo bike from the candidates below.
        Candidate bikes:
        {{bikes_jsonld}}
        Requirement: payload={{payloadKg}}, wheels={{wheelCount}}
        Return: the best sku and reason.
        """
      );
    }
    return Map.of("error", "Unknown prompt: " + name);
  }
}
