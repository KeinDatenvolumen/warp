// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.bff.application.service;

import de.warp.bff.api.dto.ServiceHealthResponse;
import de.warp.bff.api.dto.SystemHealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SystemHealthServiceTest {

    private MockRestServiceServer mockServer;
    private SystemHealthService healthService;

    private static final String IDENTITY_URL = "http://identity-service:8080/"; // tests ending slash normalization
    private static final String ROUTING_URL = "http://routing-service:8080";
    private static final String WEATHER_URL = "http://weather-service:8080";
    private static final String STORAGE_URL = "http://storage-service:8080";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        Executor directExecutor = Runnable::run; // direct synchronous executor for predictable tests

        healthService = new SystemHealthService(
                builder.build(),
                directExecutor,
                "identity-service", IDENTITY_URL,
                "routing-service", ROUTING_URL,
                "weather-service", WEATHER_URL,
                "storage-service", STORAGE_URL
        );
    }

    @Test
    void allServicesHealthyResultsInOverallUpStatus() {
        mockServer.expect(requestTo("http://identity-service:8080/internal/health/readiness"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://routing-service:8080/internal/health/readiness"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://weather-service:8080/internal/health/readiness"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://storage-service:8080/internal/health/readiness"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        SystemHealthResponse response = healthService.getSystemHealth();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.healthyServices()).isEqualTo(5); // 4 downstream + 1 BFF
        assertThat(response.totalServices()).isEqualTo(5);
        assertThat(response.services()).hasSize(5);

        ServiceHealthResponse bff = response.services().get(0);
        assertThat(bff.service()).isEqualTo("bff-service");
        assertThat(bff.status()).isEqualTo("UP");
        assertThat(bff.reachable()).isTrue();

        mockServer.verify();
    }

    @Test
    void singleServiceUnavailableResultsInDegradedStatus() {
        mockServer.expect(requestTo("http://identity-service:8080/internal/health/readiness"))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://routing-service:8080/internal/health/readiness"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        mockServer.expect(requestTo("http://weather-service:8080/internal/health/readiness"))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://storage-service:8080/internal/health/readiness"))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        SystemHealthResponse response = healthService.getSystemHealth();

        assertThat(response.status()).isEqualTo("DEGRADED");
        assertThat(response.healthyServices()).isEqualTo(4);
        assertThat(response.totalServices()).isEqualTo(5);

        ServiceHealthResponse routing = response.services().stream()
                .filter(s -> s.service().equals("routing-service"))
                .findFirst()
                .orElseThrow();
        assertThat(routing.reachable()).isFalse();
        assertThat(routing.status()).isEqualTo("UNAVAILABLE");
    }

    @Test
    void handlesTimeoutAsUnavailable() {
        mockServer.expect(requestTo("http://identity-service:8080/internal/health/readiness"))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://routing-service:8080/internal/health/readiness"))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://weather-service:8080/internal/health/readiness"))
                .andRespond(withException(new java.net.SocketTimeoutException("Read timed out")));

        mockServer.expect(requestTo("http://storage-service:8080/internal/health/readiness"))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        SystemHealthResponse response = healthService.getSystemHealth();

        assertThat(response.status()).isEqualTo("DEGRADED");
        assertThat(response.healthyServices()).isEqualTo(4);

        ServiceHealthResponse weather = response.services().stream()
                .filter(s -> s.service().equals("weather-service"))
                .findFirst()
                .orElseThrow();
        assertThat(weather.reachable()).isFalse();
        assertThat(weather.status()).isEqualTo("UNAVAILABLE");
    }

    @Test
    void handlesUnexpectedResponsesAndDownStatus() {
        // null body
        mockServer.expect(requestTo("http://identity-service:8080/internal/health/readiness"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        // null status field in body
        mockServer.expect(requestTo("http://routing-service:8080/internal/health/readiness"))
                .andRespond(withSuccess("{\"status\": null}", MediaType.APPLICATION_JSON));

        // explicit DOWN status
        mockServer.expect(requestTo("http://weather-service:8080/internal/health/readiness"))
                .andRespond(withSuccess("{\"status\":\"DOWN\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://storage-service:8080/internal/health/readiness"))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        SystemHealthResponse response = healthService.getSystemHealth();

        assertThat(response.status()).isEqualTo("DEGRADED");
        assertThat(response.healthyServices()).isEqualTo(2); // bff + storage

        ServiceHealthResponse identity = response.services().stream()
                .filter(s -> s.service().equals("identity-service"))
                .findFirst()
                .orElseThrow();
        assertThat(identity.reachable()).isFalse();
        assertThat(identity.status()).isEqualTo("UNAVAILABLE");

        ServiceHealthResponse routing = response.services().stream()
                .filter(s -> s.service().equals("routing-service"))
                .findFirst()
                .orElseThrow();
        assertThat(routing.reachable()).isFalse();
        assertThat(routing.status()).isEqualTo("UNAVAILABLE");

        ServiceHealthResponse weather = response.services().stream()
                .filter(s -> s.service().equals("weather-service"))
                .findFirst()
                .orElseThrow();
        assertThat(weather.reachable()).isTrue();
        assertThat(weather.status()).isEqualTo("DOWN");
    }

    @Test
    void handlesMultipleSimultaneousDownstreamFailures() {
        mockServer.expect(requestTo("http://identity-service:8080/internal/health/readiness"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        mockServer.expect(requestTo("http://routing-service:8080/internal/health/readiness"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        mockServer.expect(requestTo("http://weather-service:8080/internal/health/readiness"))
                .andRespond(withException(new java.net.ConnectException("Connection refused")));

        mockServer.expect(requestTo("http://storage-service:8080/internal/health/readiness"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        SystemHealthResponse response = healthService.getSystemHealth();

        assertThat(response.status()).isEqualTo("DEGRADED");
        assertThat(response.healthyServices()).isEqualTo(1); // Only BFF itself
        assertThat(response.totalServices()).isEqualTo(5);
    }
}
