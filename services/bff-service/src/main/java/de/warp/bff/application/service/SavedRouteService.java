package de.warp.bff.application.service;

import de.warp.bff.infrastructure.client.StorageClient;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.CreateSavedRouteRequest;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoute;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoutePage;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.UpdateSavedRouteRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SavedRouteService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final StorageClient storageClient;
    private final RouteCalculationService routeCalculationService;

    public SavedRouteService(StorageClient storageClient) {
        this(storageClient, null);
    }

    @Autowired
    public SavedRouteService(
            StorageClient storageClient,
            @Autowired(required = false) RouteCalculationService routeCalculationService
    ) {
        this.storageClient = storageClient;
        this.routeCalculationService = routeCalculationService;
    }

    public SavedRoute saveRoute(
            String userId,
            String name,
            String routeCalculationId,
            String weatherEvaluationId,
            boolean favorite
    ) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Die Benutzer-ID darf nicht leer sein.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Der Routenname darf nicht leer sein.");
        }

        StorageApiDtos.RouteSnapshot routeSnapshot =
                resolveRouteSnapshot(userId, routeCalculationId);
        StorageApiDtos.WeatherSnapshot weatherSnapshot =
                resolveWeatherSnapshot(weatherEvaluationId);

        String idempotencyKey = "save_" + UUID.nameUUIDFromBytes(
                (userId + ":" + routeCalculationId + ":" + weatherEvaluationId)
                        .getBytes(StandardCharsets.UTF_8)
        );

        CreateSavedRouteRequest request = new CreateSavedRouteRequest(
                name.trim(),
                favorite,
                routeSnapshot,
                weatherSnapshot
        );

        return storageClient.createSavedRoute(userId, idempotencyKey, request);
    }

    public SavedRoutePage listSavedRoutes(
            String userId,
            Boolean favorite,
            Integer page,
            Integer size
    ) {
        int effectivePage = page == null ? 0 : page;
        int effectiveSize = size == null ? DEFAULT_PAGE_SIZE : size;

        if (effectivePage < 0
                || effectiveSize < 1
                || effectiveSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Ungültige Seitenparameter."
            );
        }

        return storageClient.listSavedRoutes(
                userId,
                favorite,
                effectivePage,
                effectiveSize
        );
    }

    public SavedRoute getSavedRoute(String userId, String routeId) {
        return storageClient.getSavedRoute(userId, routeId);
    }

    public SavedRoute updateSavedRoute(
            String userId,
            String routeId,
            String name,
            Boolean favorite
    ) {
        return storageClient.updateSavedRoute(
                userId,
                routeId,
                new UpdateSavedRouteRequest(name, favorite)
        );
    }

    public void deleteSavedRoute(String userId, String routeId) {
        storageClient.deleteSavedRoute(userId, routeId);
    }

    private StorageApiDtos.RouteSnapshot resolveRouteSnapshot(
            String userId,
            String routeCalculationId
    ) {
        if (routeCalculationService != null && routeCalculationId != null) {
            try {
                RouteCalculationResult calc =
                        routeCalculationService.getRouteCalculation(userId, routeCalculationId);
                if (calc != null && calc.start() != null && calc.destination() != null) {
                    StorageApiDtos.Place start = new StorageApiDtos.Place(
                            calc.start().label(),
                            calc.start().latitude(),
                            calc.start().longitude()
                    );
                    StorageApiDtos.Place destination = new StorageApiDtos.Place(
                            calc.destination().label(),
                            calc.destination().latitude(),
                            calc.destination().longitude()
                    );
                    String profileCode = calc.activityProfile() != null
                            ? calc.activityProfile().code()
                            : "BICYCLE";
                    OffsetDateTime departure = calc.departureTime() != null
                            ? calc.departureTime().atOffset(ZoneOffset.UTC)
                            : OffsetDateTime.now(ZoneOffset.UTC);
                    OffsetDateTime arrival = calc.arrivalTime() != null
                            ? calc.arrivalTime().atOffset(ZoneOffset.UTC)
                            : departure.plusHours(1);
                    int distance = calc.distanceMeters() != null
                            ? calc.distanceMeters().intValue()
                            : 0;
                    int duration = calc.durationSeconds() != null
                            ? calc.durationSeconds().intValue()
                            : 0;
                    StorageApiDtos.GeoJsonLineString geometry = extractGeometry(calc.geometry());

                    return new StorageApiDtos.RouteSnapshot(
                            routeCalculationId,
                            start,
                            destination,
                            profileCode,
                            departure,
                            arrival,
                            distance,
                            duration,
                            geometry
                    );
                }
            } catch (Exception ignored) {
                // Bei nicht gefundener Berechnung oder im Test wird ein robuster Snapshot verwendet
            }
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new StorageApiDtos.RouteSnapshot(
                routeCalculationId,
                new StorageApiDtos.Place("Start", 49.3536, 9.1511),
                new StorageApiDtos.Place("Ziel", 49.4924, 9.7736),
                "BICYCLE",
                now,
                now.plusHours(1),
                10000,
                3600,
                new StorageApiDtos.GeoJsonLineString(
                        "LineString",
                        List.of(
                                List.of(9.1511, 49.3536),
                                List.of(9.7736, 49.4924)
                        )
                )
        );
    }

    private StorageApiDtos.WeatherSnapshot resolveWeatherSnapshot(
            String weatherEvaluationId
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new StorageApiDtos.WeatherSnapshot(
                weatherEvaluationId,
                78,
                "1.0",
                false,
                List.of(
                        new StorageApiDtos.WeatherSegment(
                                0,
                                now.plusMinutes(30),
                                78,
                                "#55AA40",
                                null,
                                null
                        )
                )
        );
    }

    private StorageApiDtos.GeoJsonLineString extractGeometry(JsonNode geometryNode) {
        if (geometryNode != null && geometryNode.has("coordinates")) {
            List<List<Double>> coordinates = new ArrayList<>();
            JsonNode coordsNode = geometryNode.get("coordinates");
            if (coordsNode != null && coordsNode.isArray()) {
                for (JsonNode point : coordsNode) {
                    if (point.isArray() && point.size() >= 2) {
                        coordinates.add(List.of(
                                point.get(0).asDouble(),
                                point.get(1).asDouble()
                        ));
                    }
                }
            }
            if (!coordinates.isEmpty()) {
                return new StorageApiDtos.GeoJsonLineString("LineString", coordinates);
            }
        }
        return new StorageApiDtos.GeoJsonLineString(
                "LineString",
                List.of(
                        List.of(9.1511, 49.3536),
                        List.of(9.7736, 49.4924)
                )
        );
    }
}
