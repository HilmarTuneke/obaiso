
package com.example.mcp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

public class MCPServer implements QuarkusApplication {
    private final ObjectMapper mapper = new ObjectMapper();

    private final ToolsRegistry tools = new ToolsRegistry();

    private final ResourcesRegistry resources = new ResourcesRegistry();

    private final PromptsRegistry prompts = new PromptsRegistry();

    @Override
    public int run(String... args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));

        String line;
        while ((line = in.readLine()) != null) {
            if (line.isBlank())
                continue;
            try {
                JsonNode req = mapper.readTree(line);
                String method = req.path("method").asText(null);
                JsonNode id = req.get("id");
                JsonNode params = req.get("params");

                // Notifications have no "id" — must not be answered
                if (id == null)
                    continue;

                Object result = switch (method) {
                case "initialize" -> Map.of(
                    "protocolVersion", "2024-11-05",
                    "serverInfo", Map.of("name", "quarkus-mcp", "version", "1.0.0"),
                    "capabilities", Map.of(
                        "tools", Map.of(),
                        "resources", Map.of(),
                        "prompts", Map.of()));
                case "tools/list" -> tools.list();
                case "tools/call" -> {
                    String name = optText(params, "name");
                    JsonNode arguments = params != null ? params.get("arguments") : null;
                    Object toolResult = tools.call(name, arguments);
                    yield Map.of("content", List.of(
                        Map.of("type", "text", "text", mapper.writeValueAsString(toolResult))
                    ));
                }
                case "resources/list" -> resources.list();
                case "resources/read" -> {
                    String idOrPath = optText(params, "id");
                    if (idOrPath == null)
                        idOrPath = optText(params, "path");
                    yield resources.read(idOrPath);
                }
                case "prompts/list" -> prompts.list();
                case "prompts/get" -> {
                    String name = optText(params, "name");
                    yield prompts.get(name);
                }
                default -> Map.of("warning", "Unknown method: " + method);
                };

                Map<String, Object> resp = RpcModels.success(id, result);
                out.write(mapper.writeValueAsString(resp));
                out.newLine();
                out.flush();
            } catch (Exception e) {
                Map<String, Object> err = RpcModels.error(null, -32603, "Internal error: " + e.getMessage());
                out.write(mapper.writeValueAsString(err));
                out.newLine();
                out.flush();
            }
        }
        Quarkus.waitForExit();
        return 0;
    }

    private static String optText(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asText(null) : null;
    }
}
