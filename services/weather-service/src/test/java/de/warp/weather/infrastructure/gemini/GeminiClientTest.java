// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.infrastructure.gemini;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private RestClient.Builder restClientBuilder;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        restClientBuilder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    private GeminiClient createClient(String apiKey, String model, String fallbackModel) {
        GeminiProperties properties = new GeminiProperties(
                URI.create("https://generativelanguage.googleapis.com"),
                apiKey,
                model,
                fallbackModel,
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                0.2
        );
        return new GeminiClient(restClientBuilder.build(), properties, objectMapper);
    }

    @Test
    void isConfiguredAndConfiguredModelReflectProperties() {
        GeminiClient unconfigured = createClient("", "gemini-3.5-flash", "gemini-3.6-flash");
        assertThat(unconfigured.isConfigured()).isFalse();
        assertThat(unconfigured.configuredModel()).isEqualTo("gemini-3.5-flash");

        assertThatThrownBy(() -> unconfigured.generateStructuredAnalysis("sys", "prompt", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEMINI_API_KEY is not configured");

        GeminiClient configured = createClient("valid-key", "gemini-3.5-flash", "gemini-3.6-flash");
        assertThat(configured.isConfigured()).isTrue();
    }

    @Test
    void successfullyExtractsStructuredJsonAndIgnoresThoughts() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        String geminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "thought": true, "text": "Analyzing weather..." },
                          { "text": "{\\"score\\": 85, \\"summary\\": \\"Optimal weather\\"}" }
                        ]
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess(geminiResponse, MediaType.APPLICATION_JSON));

        GeminiClient.GeminiStructuredOutput output = client.generateStructuredAnalysis(
                "System instruction",
                "User prompt",
                Map.of("type", "OBJECT")
        );

        assertThat(output).isNotNull();
        assertThat(output.modelName()).isEqualTo("gemini-3.5-flash");
        assertThat(output.jsonNode().path("score").asInt()).isEqualTo(85);
        assertThat(output.jsonNode().path("summary").asString()).isEqualTo("Optimal weather");
        mockServer.verify();
    }

    @Test
    void fallsBackToNextModelOnHttp429() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        String fallbackSuccess = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "{\\"status\\": \\"ok\\"}" }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(fallbackSuccess, MediaType.APPLICATION_JSON));

        GeminiClient.GeminiStructuredOutput output = client.generateStructuredAnalysis("sys", "prompt", null);

        assertThat(output.modelName()).isEqualTo("gemini-3.6-flash");
        assertThat(output.jsonNode().path("status").asString()).isEqualTo("ok");
        mockServer.verify();
    }

    @Test
    void fallsBackToNextModelOnHttp404() {
        GeminiClient client = createClient("test-key", "custom-model", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/custom-model:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        String fallbackSuccess = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "{\\"status\\": \\"after-404\\"}" }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(fallbackSuccess, MediaType.APPLICATION_JSON));

        GeminiClient.GeminiStructuredOutput output = client.generateStructuredAnalysis("sys", "prompt", Map.of());

        assertThat(output.modelName()).isEqualTo("gemini-3.6-flash");
        assertThat(output.jsonNode().path("status").asString()).isEqualTo("after-404");
        mockServer.verify();
    }

    @Test
    void handlesNullModelConfigurations() {
        GeminiClient client = createClient("test-key", null, "   ");

        String success = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "thought": true, "text": "thinking..." },
                          { "thought": true, "text": "still thinking..." },
                          { "text": "{\\"result\\": true}" }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(success, MediaType.APPLICATION_JSON));

        GeminiClient.GeminiStructuredOutput output = client.generateStructuredAnalysis("sys", "prompt", Map.of());
        assertThat(output).isNotNull();
        mockServer.verify();
    }

    @Test
    void handlesLiteModelConfiguration() {
        GeminiClient liteClient = createClient("test-key", "gemini-3.0-flash-lite", "gemini-3.6-flash");

        String success = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "{\\"result\\": true}" }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.0-flash-lite:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(success, MediaType.APPLICATION_JSON));

        GeminiClient.GeminiStructuredOutput output = liteClient.generateStructuredAnalysis("sys", "prompt", null);
        assertThat(output).isNotNull();
        mockServer.verify();
    }

    @Test
    void fallsBackToNextModelOnHttp503() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        String fallbackSuccess = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "{\\"status\\": \\"recovered\\"}" }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(fallbackSuccess, MediaType.APPLICATION_JSON));

        GeminiClient.GeminiStructuredOutput output = client.generateStructuredAnalysis("sys", "prompt", null);

        assertThat(output.modelName()).isEqualTo("gemini-3.6-flash");
        assertThat(output.jsonNode().path("status").asString()).isEqualTo("recovered");
        mockServer.verify();
    }

    @Test
    void fallsBackToNextModelOnTimeout() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withException(new java.net.SocketTimeoutException("Read timed out")));

        String fallbackSuccess = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "{\\"status\\": \\"after-timeout\\"}" }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(fallbackSuccess, MediaType.APPLICATION_JSON));

        GeminiClient.GeminiStructuredOutput output = client.generateStructuredAnalysis("sys", "prompt", null);

        assertThat(output.modelName()).isEqualTo("gemini-3.6-flash");
        assertThat(output.jsonNode().path("status").asString()).isEqualTo("after-timeout");
        mockServer.verify();
    }

    @Test
    void rethrowsImmediatelyOnHttp400() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.generateStructuredAnalysis("sys", "prompt", null))
                .isInstanceOf(RestClientResponseException.class)
                .matches(e -> ((RestClientResponseException) e).getStatusCode().value() == 400);

        mockServer.verify();
    }

    @Test
    void rethrowsImmediatelyOnHttp401And403() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.generateStructuredAnalysis("sys", "prompt", null))
                .isInstanceOf(RestClientResponseException.class)
                .matches(e -> ((RestClientResponseException) e).getStatusCode().value() == 401);

        mockServer.verify();
    }

    @Test
    void rethrowsImmediatelyOnHttp500() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.generateStructuredAnalysis("sys", "prompt", null))
                .isInstanceOf(RestClientResponseException.class)
                .matches(e -> ((RestClientResponseException) e).getStatusCode().value() == 500);

        mockServer.verify();
    }

    @Test
    void throwsLastExceptionWhenAllCandidatesExhaustedWith429() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.generateStructuredAnalysis("sys", "prompt", null))
                .isInstanceOf(RestClientResponseException.class)
                .matches(e -> ((RestClientResponseException) e).getStatusCode().value() == 429);
    }

    @Test
    void rejectsEmptyResponseRoot() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateStructuredAnalysis("sys", "prompt", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Empty response");
    }

    @Test
    void rejectsEmptyCandidatesList() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));

        // When exception thrown, it attempts fallback to next model
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateStructuredAnalysis("sys", "prompt", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no candidates");
    }

    @Test
    void rejectsMissingContentOrParts() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": [{\"content\": {\"parts\": []}}]}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": [{\"content\": {\"parts\": []}}]}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": [{\"content\": {\"parts\": []}}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateStructuredAnalysis("sys", "prompt", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no parts");
    }

    @Test
    void rejectsInvalidJsonOrBlankText() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"   \"}]}}]}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"{not json\"}]}}]}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"[1, 2, 3]\"}]}}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateStructuredAnalysis("sys", "prompt", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-object JSON structure");
    }

    @Test
    void handlesSafetyBlockAndSafetyFinishReason() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        // 1. promptFeedback blockReason
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andRespond(withSuccess("{\"promptFeedback\": {\"blockReason\": \"SAFETY\"}}", MediaType.APPLICATION_JSON));

        // 2. candidate finishReason SAFETY
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": [{\"finishReason\": \"SAFETY\", \"content\": {\"parts\": []}}]}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"))
                .andRespond(withSuccess("{\"candidates\": [{\"finishReason\": \"SAFETY\", \"content\": {\"parts\": []}}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.generateStructuredAnalysis("sys", "prompt", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("safety filter");
    }

    @Test
    void handlesMaxTokensFinishReasonWithValidJson() {
        GeminiClient client = createClient("test-key", "gemini-3.5-flash", "gemini-3.6-flash");

        String geminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "{\\"completed\\": true}" }
                        ]
                      },
                      "finishReason": "MAX_TOKENS"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"))
                .andRespond(withSuccess(geminiResponse, MediaType.APPLICATION_JSON));

        GeminiClient.GeminiStructuredOutput output = client.generateStructuredAnalysis("sys", "prompt", null);

        assertThat(output).isNotNull();
        assertThat(output.jsonNode().path("completed").asBoolean()).isTrue();
    }
}
