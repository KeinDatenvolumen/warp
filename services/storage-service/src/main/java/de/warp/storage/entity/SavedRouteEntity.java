// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.storage.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saved_routes", schema = "warp_storage")
public class SavedRouteEntity {

    @Id
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "user_id", nullable = false, updatable = false, length = 40)
    private String userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "favorite", nullable = false)
    private boolean favorite;

    @Column(name = "start_label", nullable = false, length = 255)
    private String startLabel;

    @Column(name = "start_latitude", nullable = false)
    private double startLatitude;

    @Column(name = "start_longitude", nullable = false)
    private double startLongitude;

    @Column(name = "destination_label", nullable = false, length = 255)
    private String destinationLabel;

    @Column(name = "destination_latitude", nullable = false)
    private double destinationLatitude;

    @Column(name = "destination_longitude", nullable = false)
    private double destinationLongitude;

    @Column(name = "activity_profile_code", nullable = false, length = 20)
    private String activityProfileCode;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    @Column(name = "distance_meters", nullable = false)
    private int distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "geometry_json", nullable = false, columnDefinition = "text")
    private String geometryJson;

    @Column(name = "route_calculation_id", length = 40)
    private String routeCalculationId;

    @Column(name = "weather_evaluation_id", length = 40)
    private String weatherEvaluationId;

    @Column(name = "weather_overall_score", nullable = false)
    private int weatherOverallScore;

    @Column(name = "weather_score_version", nullable = false, length = 20)
    private String weatherScoreVersion;

    @Column(name = "thunderstorm_cap_applied", nullable = false)
    private boolean thunderstormCapApplied;

    @Column(name = "idempotency_key", updatable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "savedRoute",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("segmentIndex ASC")
    private List<SavedRouteWeatherSegmentEntity> weatherSegments =
            new ArrayList<>();

    protected SavedRouteEntity() {
        // Wird von JPA benötigt.
    }

    public SavedRouteEntity(
            String id,
            String userId,
            String name,
            boolean favorite,
            String startLabel,
            double startLatitude,
            double startLongitude,
            String destinationLabel,
            double destinationLatitude,
            double destinationLongitude,
            String activityProfileCode,
            Instant departureTime,
            Instant arrivalTime,
            int distanceMeters,
            int durationSeconds,
            String geometryJson,
            String routeCalculationId,
            String weatherEvaluationId,
            int weatherOverallScore,
            String weatherScoreVersion,
            boolean thunderstormCapApplied,
            String idempotencyKey,
            Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.favorite = favorite;
        this.startLabel = startLabel;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.destinationLabel = destinationLabel;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.activityProfileCode = activityProfileCode;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.geometryJson = geometryJson;
        this.routeCalculationId = routeCalculationId;
        this.weatherEvaluationId = weatherEvaluationId;
        this.weatherOverallScore = weatherOverallScore;
        this.weatherScoreVersion = weatherScoreVersion;
        this.thunderstormCapApplied = thunderstormCapApplied;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void addWeatherSegment(SavedRouteWeatherSegmentEntity segment) {
        segment.assignTo(this);
        weatherSegments.add(segment);
    }

    public void rename(String newName, Instant now) {
        this.name = newName;
        this.updatedAt = now;
    }

    public void changeFavorite(boolean newFavorite, Instant now) {
        this.favorite = newFavorite;
        this.updatedAt = now;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public boolean isFavorite() { return favorite; }
    public String getStartLabel() { return startLabel; }
    public double getStartLatitude() { return startLatitude; }
    public double getStartLongitude() { return startLongitude; }
    public String getDestinationLabel() { return destinationLabel; }
    public double getDestinationLatitude() { return destinationLatitude; }
    public double getDestinationLongitude() { return destinationLongitude; }
    public String getActivityProfileCode() { return activityProfileCode; }
    public Instant getDepartureTime() { return departureTime; }
    public Instant getArrivalTime() { return arrivalTime; }
    public int getDistanceMeters() { return distanceMeters; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getGeometryJson() { return geometryJson; }
    public String getRouteCalculationId() { return routeCalculationId; }
    public String getWeatherEvaluationId() { return weatherEvaluationId; }
    public int getWeatherOverallScore() { return weatherOverallScore; }
    public String getWeatherScoreVersion() { return weatherScoreVersion; }
    public boolean isThunderstormCapApplied() { return thunderstormCapApplied; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public List<SavedRouteWeatherSegmentEntity> getWeatherSegments() {
        return weatherSegments;
    }
}