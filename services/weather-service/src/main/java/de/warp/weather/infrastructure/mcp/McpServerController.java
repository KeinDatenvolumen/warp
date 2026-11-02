package de.warp.weather.infrastructure.mcp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Standardkonformer Model Context Protocol (MCP) JSON-RPC 2.0 Server-Endpunkt
 * für den WaRP Weather-Service. Unterstützt initialize, tools/list und tools/call.
 */
@RestController
@RequestMapping({"/internal/v1/weather/mcp", "/internal/api/v1/weather/mcp"})
public class McpServerController {

    private static final String JSONRPC_VERSION = "2.0";
    private static final String MCP_PROTOCOL_VERSION = "2024-11-05";

    private final McpToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public McpServerController(
            McpToolRegistry toolRegistry,
            ObjectMapper objectMapper
    ) {
        this.toolRegistry = Objects.requireNonNull(toolRegistry);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleMcpJsonRpc(
            @RequestBody Map<String, Object> request
    ) {
        Object id = request != null ? request.get("id") : null;
        String method = request != null && request.get("method") instanceof String s ? s : "";

        try {
            return switch (method) {
                case "initialize" -> ResponseEntity.ok(jsonRpcSuccess(id, Map.of(
                        "protocolVersion", MCP_PROTOCOL_VERSION,
                        "capabilities", Map.of("tools", Map.of("listChanged", false)),
                        "serverInfo", Map.of(
                                "name", "warp-weather-mcp-server",
                                "version", "1.0.0"
                        )
                )));
                case "tools/list" -> ResponseEntity.ok(jsonRpcSuccess(id, Map.of(
                        "tools", toolRegistry.listTools()
                )));
                case "tools/call" -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = request.get("params") instanceof Map<?, ?> m
                            ? (Map<String, Object>) m
                            : Map.of();
                    String toolName = params.get("name") instanceof String s ? s : "";
                    @SuppressWarnings("unchecked")
                    Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?> a
                            ? (Map<String, Object>) a
                            : Map.of();

                    Object toolOutput = toolRegistry.callTool(toolName, arguments);
                    String serialized = objectMapper.writeValueAsString(toolOutput);

                    yield ResponseEntity.ok(jsonRpcSuccess(id, Map.of(
                            "content", List.of(Map.of(
                                    "type", "text",
                                    "text", serialized
                            )),
                            "isError", false
                    )));
                }
                default -> ResponseEntity.ok(jsonRpcError(
                        id,
                        -32601,
                        "Method not found: " + method
                ));
            };
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(jsonRpcError(id, -32602, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(jsonRpcError(id, -32603, "Internal MCP error: " + e.getMessage()));
        }
    }

    private Map<String, Object> jsonRpcSuccess(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> jsonRpcError(Object id, int code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("error", error);
        return response;
    }
}
