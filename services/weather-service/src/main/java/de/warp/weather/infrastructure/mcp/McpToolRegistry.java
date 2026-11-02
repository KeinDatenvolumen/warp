package de.warp.weather.infrastructure.mcp;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class McpToolRegistry {

    private final Map<String, RegisteredTool> tools = new LinkedHashMap<>();
    private final ObjectMapper objectMapper;

    public McpToolRegistry(
            WeatherMcpTools weatherMcpTools,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        registerBean(Objects.requireNonNull(weatherMcpTools));
    }

    public final void registerBean(Object bean) {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            McpTool annotation = method.getAnnotation(McpTool.class);
            if (annotation == null) {
                continue;
            }
            method.setAccessible(true);
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();

            for (Parameter param : method.getParameters()) {
                McpParam paramAnn = param.getAnnotation(McpParam.class);
                String paramName = paramAnn != null ? paramAnn.name() : param.getName();
                String paramDesc = paramAnn != null ? paramAnn.description() : "";
                boolean isReq = paramAnn == null || paramAnn.required();

                Map<String, Object> propSchema = new LinkedHashMap<>();
                propSchema.put("type", resolveJsonSchemaType(param.getType()));
                propSchema.put("description", paramDesc);
                properties.put(paramName, propSchema);
                if (isReq) {
                    required.add(paramName);
                }
            }

            Map<String, Object> inputSchema = new LinkedHashMap<>();
            inputSchema.put("type", "object");
            inputSchema.put("properties", properties);
            inputSchema.put("required", required);

            McpToolDefinition definition = new McpToolDefinition(
                    annotation.name(),
                    annotation.description(),
                    inputSchema
            );
            tools.put(annotation.name(), new RegisteredTool(bean, method, definition));
        }
    }

    public List<McpToolDefinition> listTools() {
        return tools.values().stream()
                .map(RegisteredTool::definition)
                .toList();
    }

    public Object callTool(String toolName, Map<String, Object> arguments) {
        RegisteredTool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown MCP tool: " + toolName);
        }

        Map<String, Object> safeArgs = arguments != null ? arguments : Map.of();
        Parameter[] parameters = tool.method().getParameters();
        Object[] invokeArgs = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            McpParam paramAnn = param.getAnnotation(McpParam.class);
            String paramName = paramAnn != null ? paramAnn.name() : param.getName();
            Object rawValue = safeArgs.get(paramName);
            invokeArgs[i] = convertArgument(rawValue, param.getType());
        }

        try {
            return tool.method().invoke(tool.target(), invokeArgs);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("Failed to execute MCP tool '" + toolName + "': " + cause.getMessage(), cause);
        }
    }

    private Object convertArgument(Object rawValue, Class<?> targetType) {
        if (rawValue == null) {
            return null;
        }
        if (targetType.isInstance(rawValue)) {
            return rawValue;
        }
        if (targetType == String.class) {
            return String.valueOf(rawValue);
        }
        return objectMapper.convertValue(rawValue, targetType);
    }

    private String resolveJsonSchemaType(Class<?> javaType) {
        if (javaType == String.class) {
            return "string";
        }
        if (javaType == int.class || javaType == Integer.class || javaType == long.class || javaType == Long.class) {
            return "integer";
        }
        if (Number.class.isAssignableFrom(javaType) || javaType == double.class || javaType == float.class) {
            return "number";
        }
        if (javaType == boolean.class || javaType == Boolean.class) {
            return "boolean";
        }
        if (List.class.isAssignableFrom(javaType)) {
            return "array";
        }
        return "object";
    }

    private record RegisteredTool(
            Object target,
            Method method,
            McpToolDefinition definition
    ) {
    }
}
