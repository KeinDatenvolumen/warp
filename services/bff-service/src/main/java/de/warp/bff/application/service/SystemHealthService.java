package de.warp.bff.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import de.warp.bff.api.dto.ActuatorHealthResponse;
import de.warp.bff.api.dto.ServiceHealthResponse;
import de.warp.bff.api.dto.SystemHealthResponse;

@Service
public class SystemHealthService {

    private static final String READINESS_PATH =
            "/internal/health/readiness";

    private final RestClient restClient;
    private final ServiceDefinition identityService;
    private final ServiceDefinition routingService;
    private final ServiceDefinition weatherService;
    private final ServiceDefinition storageService;

    private final Executor healthCheckExecutor;

    public SystemHealthService(
            RestClient serviceHealthRestClient,
            Executor healthCheckExecutor,
            @Value("${warp.services.identity.name}") String identityName,
            @Value("${warp.services.identity.url}") String identityUrl,
            @Value("${warp.services.routing.name}") String routingName,
            @Value("${warp.services.routing.url}") String routingUrl,
            @Value("${warp.services.weather.name}") String weatherName,
            @Value("${warp.services.weather.url}") String weatherUrl,
            @Value("${warp.services.storage.name}") String storageName,
            @Value("${warp.services.storage.url}") String storageUrl
    ) {
        this.restClient = serviceHealthRestClient;
        this.healthCheckExecutor = healthCheckExecutor;
        this.identityService =
                new ServiceDefinition(identityName, identityUrl);
        this.routingService =
                new ServiceDefinition(routingName, routingUrl);
        this.weatherService =
                new ServiceDefinition(weatherName, weatherUrl);
        this.storageService =
                new ServiceDefinition(storageName, storageUrl);
    }

    public SystemHealthResponse getSystemHealth() {
        List<ServiceDefinition> serviceDefinitions = List.of(
                identityService,
                routingService,
                weatherService,
                storageService
        );

        List<CompletableFuture<ServiceHealthResponse>> futures =
                serviceDefinitions.stream()
                        .map(service -> CompletableFuture.supplyAsync(
                                () -> checkService(service),
                                healthCheckExecutor
                        ))
                        .toList();

        List<ServiceHealthResponse> services =
                new java.util.ArrayList<>();

        services.add(createBffHealth());
        services.addAll(
                futures.stream()
                        .map(CompletableFuture::join)
                        .toList()
        );

        int healthyServices = (int) services.stream()
                .filter(this::isHealthy)
                .count();

        String overallStatus = healthyServices == services.size()
                ? "UP"
                : "DEGRADED";

        return new SystemHealthResponse(
                overallStatus,
                Instant.now(),
                healthyServices,
                services.size(),
                List.copyOf(services)
        );
    }

    private ServiceHealthResponse createBffHealth() {
        return new ServiceHealthResponse(
                "bff-service",
                "UP",
                true,
                0,
                "/internal/health/readiness"
        );
    }

    private ServiceHealthResponse checkService(
            ServiceDefinition service
    ) {
        String healthUrl =
                normalizeBaseUrl(service.baseUrl()) + READINESS_PATH;

        long startedAt = System.nanoTime();

        try {
            ActuatorHealthResponse response = restClient
                    .get()
                    .uri(healthUrl)
                    .retrieve()
                    .body(ActuatorHealthResponse.class);

            long responseTimeMs = elapsedMilliseconds(startedAt);

            if (response == null || response.status() == null) {
                return unavailableResponse(
                        service,
                        healthUrl,
                        responseTimeMs
                );
            }

            String status = response.status()
                    .toUpperCase(Locale.ROOT);

            return new ServiceHealthResponse(
                    service.name(),
                    status,
                    true,
                    responseTimeMs,
                    healthUrl
            );
        } catch (RestClientException exception) {
            return unavailableResponse(
                    service,
                    healthUrl,
                    elapsedMilliseconds(startedAt)
            );
        }
    }

    private ServiceHealthResponse unavailableResponse(
            ServiceDefinition service,
            String healthUrl,
            long responseTimeMs
    ) {
        return new ServiceHealthResponse(
                service.name(),
                "UNAVAILABLE",
                false,
                responseTimeMs,
                healthUrl
        );
    }

    private boolean isHealthy(ServiceHealthResponse service) {
        return service.reachable()
                && "UP".equals(service.status());
    }

    private long elapsedMilliseconds(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl;
    }

    private record ServiceDefinition(
            String name,
            String baseUrl
    ) {
    }
}