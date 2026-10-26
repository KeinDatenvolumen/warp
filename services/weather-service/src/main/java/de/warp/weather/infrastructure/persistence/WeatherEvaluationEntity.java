package de.warp.weather.infrastructure.persistence;

import de.warp.weather.domain.model.EvaluationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weather_evaluation", schema = "warp_weather")
public class WeatherEvaluationEntity {

    @Id
    @Column(name = "evaluation_id", nullable = false, length = 40)
    private String evaluationId;

    @Column(name = "route_calculation_id", nullable = false, length = 40)
    private String routeCalculationId;

    @Column(name = "requested_by_user_id", length = 40)
    private String requestedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EvaluationStatus status;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "score_version", nullable = false, length = 20)
    private String scoreVersion;

    @Column(name = "thunderstorm_cap_applied", nullable = false)
    private boolean thunderstormCapApplied;

    @Column(name = "weather_data_generated_at", nullable = false)
    private Instant weatherDataGeneratedAt;

    @Column(name = "weather_data_expires_at", nullable = false)
    private Instant weatherDataExpiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "evaluation",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("segmentIndex ASC")
    private List<WeatherSegmentEntity> segments = new ArrayList<>();

    protected WeatherEvaluationEntity() {
    }

    public WeatherEvaluationEntity(
            String evaluationId,
            String routeCalculationId,
            String requestedByUserId,
            EvaluationStatus status,
            Instant departureTime,
            int overallScore,
            String scoreVersion,
            boolean thunderstormCapApplied,
            Instant weatherDataGeneratedAt,
            Instant weatherDataExpiresAt,
            Instant createdAt
    ) {
        this.evaluationId = evaluationId;
        this.routeCalculationId = routeCalculationId;
        this.requestedByUserId = requestedByUserId;
        this.status = status;
        this.departureTime = departureTime;
        this.overallScore = overallScore;
        this.scoreVersion = scoreVersion;
        this.thunderstormCapApplied = thunderstormCapApplied;
        this.weatherDataGeneratedAt = weatherDataGeneratedAt;
        this.weatherDataExpiresAt = weatherDataExpiresAt;
        this.createdAt = createdAt;
    }

    public void addSegment(WeatherSegmentEntity segment) {
        segment.setEvaluation(this);
        this.segments.add(segment);
    }

    public String getEvaluationId() {
        return evaluationId;
    }

    public String getRouteCalculationId() {
        return routeCalculationId;
    }

    public String getRequestedByUserId() {
        return requestedByUserId;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public String getScoreVersion() {
        return scoreVersion;
    }

    public boolean isThunderstormCapApplied() {
        return thunderstormCapApplied;
    }

    public Instant getWeatherDataGeneratedAt() {
        return weatherDataGeneratedAt;
    }

    public Instant getWeatherDataExpiresAt() {
        return weatherDataExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<WeatherSegmentEntity> getSegments() {
        return segments;
    }
}
