// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.infrastructure.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("McpToolRegistry Unit Tests")
class McpToolRegistryTest {

    private ObjectMapper objectMapper;
    private WeatherMcpTools weatherMcpTools;
    private McpToolRegistry registry;

    static class SampleTestBean {

        public void unannotatedMethod() {
            // Should be ignored by registry
        }

        @McpTool(name = "all_types_tool", description = "Tool with all parameter types")
        public String allTypesTool(
                @McpParam(name = "strParam", description = "string description", required = true)
                String strParam,
                @McpParam(name = "intParam", description = "int description", required = false)
                int intParam,
                @McpParam(name = "boxedIntParam", description = "boxed int")
                Integer boxedIntParam,
                @McpParam(name = "longParam", description = "long description")
                long longParam,
                @McpParam(name = "boxedLongParam", description = "boxed long")
                Long boxedLongParam,
                @McpParam(name = "doubleParam", description = "double description")
                double doubleParam,
                @McpParam(name = "floatParam", description = "float description")
                float floatParam,
                @McpParam(name = "bigDecimalParam", description = "big decimal")
                BigDecimal bigDecimalParam,
                @McpParam(name = "boolParam", description = "bool description")
                boolean boolParam,
                @McpParam(name = "boxedBoolParam", description = "boxed bool")
                Boolean boxedBoolParam,
                @McpParam(name = "listParam", description = "list description")
                List<String> listParam,
                @McpParam(name = "objectParam", description = "object description")
                Map<String, Object> objectParam,
                String unannotatedParam
        ) {
            return "strParam=" + strParam + ", unannotated=" + unannotatedParam;
        }

        @McpTool(name = "throwing_tool", description = "Throws exception on execution")
        public String throwingTool() {
            throw new IllegalArgumentException("Deliberate execution exception");
        }

        @McpTool(name = "string_convert_tool", description = "Converts object to string")
        public String stringConvertTool(@McpParam(name = "value", description = "value to convert") String value) {
            return "converted=" + value;
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        weatherMcpTools = mock(WeatherMcpTools.class);
        registry = new McpToolRegistry(weatherMcpTools, objectMapper);
    }

    @Test
    @DisplayName("Constructor requires non-null arguments")
    void constructorRejectsNulls() {
        assertThatThrownBy(() -> new McpToolRegistry(null, objectMapper))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new McpToolRegistry(weatherMcpTools, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Registers bean with various parameter types and generates correct JSON schema")
    @SuppressWarnings("unchecked")
    void registersBeanWithVariousTypes() {
        SampleTestBean sampleBean = new SampleTestBean();
        registry.registerBean(sampleBean);

        List<McpToolDefinition> tools = registry.listTools();
        McpToolDefinition toolDef = tools.stream()
                .filter(t -> t.name().equals("all_types_tool"))
                .findFirst()
                .orElseThrow();

        assertThat(toolDef.description()).isEqualTo("Tool with all parameter types");

        Map<String, Object> inputSchema = toolDef.inputSchema();
        assertThat(inputSchema.get("type")).isEqualTo("object");

        Map<String, Object> properties = (Map<String, Object>) inputSchema.get("properties");
        List<String> required = (List<String>) inputSchema.get("required");

        // Schema types
        assertThat(((Map<String, Object>) properties.get("strParam")).get("type")).isEqualTo("string");
        assertThat(((Map<String, Object>) properties.get("intParam")).get("type")).isEqualTo("integer");
        assertThat(((Map<String, Object>) properties.get("boxedIntParam")).get("type")).isEqualTo("integer");
        assertThat(((Map<String, Object>) properties.get("longParam")).get("type")).isEqualTo("integer");
        assertThat(((Map<String, Object>) properties.get("boxedLongParam")).get("type")).isEqualTo("integer");
        assertThat(((Map<String, Object>) properties.get("doubleParam")).get("type")).isEqualTo("number");
        assertThat(((Map<String, Object>) properties.get("floatParam")).get("type")).isEqualTo("number");
        assertThat(((Map<String, Object>) properties.get("bigDecimalParam")).get("type")).isEqualTo("number");
        assertThat(((Map<String, Object>) properties.get("boolParam")).get("type")).isEqualTo("boolean");
        assertThat(((Map<String, Object>) properties.get("boxedBoolParam")).get("type")).isEqualTo("boolean");
        assertThat(((Map<String, Object>) properties.get("listParam")).get("type")).isEqualTo("array");
        assertThat(((Map<String, Object>) properties.get("objectParam")).get("type")).isEqualTo("object");

        // Unannotated parameter should have param name, empty description, and be required
        assertThat(properties).containsKey("unannotatedParam");
        assertThat(((Map<String, Object>) properties.get("unannotatedParam")).get("description")).isEqualTo("");

        // Required check: strParam (required=true) and unannotatedParam (default required=true) must be in required
        assertThat(required).contains("strParam", "unannotatedParam");
        // intParam (required=false) must NOT be in required
        assertThat(required).doesNotContain("intParam");
    }

    @Test
    @DisplayName("callTool executes successfully with exact and converted types")
    void callToolExecutesSuccessfully() {
        registry.registerBean(new SampleTestBean());

        Map<String, Object> args = new HashMap<>();
        args.put("strParam", "hello");
        args.put("intParam", 42);
        args.put("boxedIntParam", 100);
        args.put("longParam", 5000L);
        args.put("boxedLongParam", 6000L);
        args.put("doubleParam", 12.5);
        args.put("floatParam", 3.14f);
        args.put("bigDecimalParam", "45.67"); // converted via ObjectMapper
        args.put("boolParam", true);
        args.put("boxedBoolParam", Boolean.FALSE);
        args.put("listParam", List.of("a", "b"));
        args.put("objectParam", Map.of("k", "v"));
        args.put("unannotatedParam", "world");

        Object result = registry.callTool("all_types_tool", args);

        assertThat(result).isEqualTo("strParam=hello, unannotated=world");
    }

    @Test
    @DisplayName("callTool converts non-string raw value to String when target type is String")
    void callToolConvertsNonStringToString() {
        registry.registerBean(new SampleTestBean());

        Map<String, Object> args = Map.of("value", 12345);
        Object result = registry.callTool("string_convert_tool", args);

        assertThat(result).isEqualTo("converted=12345");
    }

    @Test
    @DisplayName("callTool handles null arguments map gracefully")
    void callToolWithNullArguments() {
        registry.registerBean(new SampleTestBean());

        // Calling with null args -> arguments default to empty map, all params receive null or zero
        Object result = registry.callTool("string_convert_tool", null);

        assertThat(result).isEqualTo("converted=null");
    }

    @Test
    @DisplayName("callTool throws IllegalArgumentException for unknown tool name")
    void callToolThrowsOnUnknownTool() {
        assertThatThrownBy(() -> registry.callTool("non_existent_tool", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown MCP tool: non_existent_tool");
    }

    @Test
    @DisplayName("callTool wraps invocation target exception in IllegalStateException")
    void callToolWrapsInvocationException() {
        registry.registerBean(new SampleTestBean());

        assertThatThrownBy(() -> registry.callTool("throwing_tool", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to execute MCP tool 'throwing_tool'")
                .hasMessageContaining("Deliberate execution exception");
    }
}
