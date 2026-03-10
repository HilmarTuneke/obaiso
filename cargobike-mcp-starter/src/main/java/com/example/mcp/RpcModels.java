
package com.example.mcp;

import java.util.Map;

public class RpcModels {
  public static Map<String, Object> success(Object id, Object result) {
    return Map.of(
      "jsonrpc", "2.0",
      "id", id,
      "result", result
    );
  }

  public static Map<String, Object> error(Object id, int code, String message) {
    return Map.of(
      "jsonrpc", "2.0",
      "id", id,
      "error", Map.of("code", code, "message", message)
    );
  }
}
