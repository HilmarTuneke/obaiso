
package com.example.mcp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

public class ResourcesRegistry {
    private static final String ASSETS_ROOT = "assets/";

    public Map<String, Object> list() throws IOException {
        List<Map<String, String>> items = Stream.of(
            "ontology/cargobike.ttl",
            "openapi/catalog.yaml",
            "openapi/orders.yaml",
            "openapi/customers.yaml",
            "openapi/inventory.yaml",
            "openapi/shipping.yaml",
            "wiremock/mappings/catalog-get-cargo-bikes.json",
            "wiremock/mappings/orders-get-order.json",
            "wiremock/mappings/customers-get-customer.json",
            "wiremock/mappings/inventory-get-by-sku.json",
            "wiremock/mappings/shipping-post-quote.json")
            .map(p -> Map.of(
                "id", p,
                "name", p.substring(p.lastIndexOf('/') + 1),
                "uri", "classpath:" + ASSETS_ROOT + p))
            .toList();

        return Map.of("resources", items);
    }

    public Map<String, Object> read(String idOrPath) throws IOException {
        String path = ASSETS_ROOT
            + (idOrPath.startsWith(ASSETS_ROOT) ? idOrPath.substring(ASSETS_ROOT.length()) : idOrPath);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null)
                return Map.of("error", "Resource not found: " + idOrPath);
            String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Map.of("id", idOrPath, "text", text);
        }
    }
}
