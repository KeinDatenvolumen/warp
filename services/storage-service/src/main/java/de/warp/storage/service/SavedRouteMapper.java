// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.service;

import de.warp.storage.dto.GeoJsonLineString;
import de.warp.storage.dto.CreateSavedRouteRequest;
import de.warp.storage.dto.PlaceDto;
import de.warp.storage.dto.RouteSnapshotDto;
import de.warp.storage.dto.SavedRoutePageResponse;
import de.warp.storage.dto.SavedRouteResponse;
import de.warp.storage.dto.SavedRouteSummaryResponse;
import de.warp.storage.dto.SegmentWeatherDto;
import de.warp.storage.dto.WeatherSegmentDto;
import de.warp.storage.dto.WeatherSnapshotDto;
import de.warp.storage.entity.SavedRouteEntity;
import de.warp.storage.entity.SavedRouteWeatherSegmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class SavedRouteMapper {

    private final JsonMapper jsonMapper;

    public SavedRouteMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public SavedRouteEntity toNewEntity(
            String id,
            String userId,
            String idempotencyKey,
            CreateSavedRouteRequest request,
            Instant now
    ) {
        RouteSnapshotDto route = request.routeSnapshot();
        WeatherSnapshotDto weather = request.weatherSnapshot();

        SavedRouteEntity entity = new SavedRouteEntity(
                id,
                userId,
                request.name(),
                request.favorite(),
                route.start().label(),
                route.start().latitude(),
                route.start().longitude(),
                route.destination().label(),
                route.destination().latitude(),
                route.destination().longitude(),
                route.activityProfileCode(),
                route.departureTime().toInstant(),
                route.arrivalTime().toInstant(),
                route.distanceMeters(),
                route.durationSeconds(),
                writeGeometry(route.geometry()),
                route.routeCalculationId(),
                weather.weatherEvaluationId(),
                weather.overallScore(),
                weather.scoreVersion(),
                weather.thunderstormCapApplied(),
                idempotencyKey,
                now
        );

        for (WeatherSegmentDto segment : weather.segments()) {
            entity.addWeatherSegment(toSegmentEntity(segment));
        }

        return entity;
    }

    public SavedRouteResponse toResponse(SavedRouteEntity entity) {
        RouteSnapshotDto route = new RouteSnapshotDto(
                entity.getRouteCalculationId(),
                new PlaceDto(
                        entity.getStartLabel(),
                        entity.getStartLatitude(),
                        entity.getStartLongitude()
                ),
                new PlaceDto(
                        entity.getDestinationLabel(),
                        entity.getDestinationLatitude(),
                        entity.getDestinationLongitude()
                ),
                entity.getActivityProfileCode(),
                toOffset(entity.getDepartureTime()),
                toOffset(entity.getArrivalTime()),
                entity.getDistanceMeters(),
                entity.getDurationSeconds(),
                readGeometry(entity.getGeometryJson())
        );

        WeatherSnapshotDto weather = new WeatherSnapshotDto(
                entity.getWeatherEvaluationId(),
                entity.getWeatherOverallScore(),
                entity.getWeatherScoreVersion(),
                entity.isThunderstormCapApplied(),
                entity.getWeatherSegments().stream()
                        .map(this::toSegmentDto)
                        .toList()
        );

        return new SavedRouteResponse(
                entity.getId(),
                entity.getName(),
                entity.isFavorite(),
                route,
                weather,
                toOffset(entity.getCreatedAt()),
                toOffset(entity.getUpdatedAt())
        );
    }

    public SavedRouteSummaryResponse toSummary(SavedRouteEntity entity) {
        return new SavedRouteSummaryResponse(
                entity.getId(),
                entity.getName(),
                new PlaceDto(
                        entity.getStartLabel(),
                        entity.getStartLatitude(),
                        entity.getStartLongitude()
                ),
                new PlaceDto(
                        entity.getDestinationLabel(),
                        entity.getDestinationLatitude(),
                        entity.getDestinationLongitude()
                ),
                entity.getActivityProfileCode(),
                entity.getWeatherOverallScore(),
                entity.isFavorite(),
                toOffset(entity.getCreatedAt())
        );
    }

    public SavedRoutePageResponse toPageResponse(Page<SavedRouteEntity> page) {
        return new SavedRoutePageResponse(
                page.getContent().stream().map(this::toSummary).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private SavedRouteWeatherSegmentEntity toSegmentEntity(
            WeatherSegmentDto segment
    ) {
        SegmentWeatherDto weather = segment.weather();

        return new SavedRouteWeatherSegmentEntity(
                segment.segmentIndex(),
                segment.expectedArrivalTime().toInstant(),
                segment.score(),
                segment.color(),
                weather == null ? null : weather.windSpeedKmh(),
                weather == null ? null : weather.windDirectionDegrees(),
                weather == null ? null : weather.precipitationProbabilityPercent(),
                weather == null ? null : weather.precipitationMm(),
                weather == null ? null : weather.thunderstormProbabilityPercent(),
                segment.geometry() == null
                        ? null
                        : writeGeometry(segment.geometry())
        );
    }

    private WeatherSegmentDto toSegmentDto(
            SavedRouteWeatherSegmentEntity segment
    ) {
        boolean hasWeather = segment.getWindSpeedKmh() != null
                || segment.getWindDirectionDegrees() != null
                || segment.getPrecipitationProbabilityPercent() != null
                || segment.getPrecipitationMm() != null
                || segment.getThunderstormProbabilityPercent() != null;

        return new WeatherSegmentDto(
                segment.getSegmentIndex(),
                toOffset(segment.getExpectedArrivalTime()),
                segment.getScore(),
                segment.getColor(),
                hasWeather
                        ? new SegmentWeatherDto(
                        segment.getWindSpeedKmh(),
                        segment.getWindDirectionDegrees(),
                        segment.getPrecipitationProbabilityPercent(),
                        segment.getPrecipitationMm(),
                        segment.getThunderstormProbabilityPercent()
                )
                        : null,
                segment.getGeometryJson() == null
                        ? null
                        : readGeometry(segment.getGeometryJson())
        );
    }

    private String writeGeometry(GeoJsonLineString geometry) {
        return jsonMapper.writeValueAsString(geometry);
    }

    private GeoJsonLineString readGeometry(String json) {
        return jsonMapper.readValue(json, GeoJsonLineString.class);
    }

    private OffsetDateTime toOffset(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}