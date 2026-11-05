// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "saved_route_weather_segments", schema = "warp_storage")
public class SavedRouteWeatherSegmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saved_route_id", nullable = false, updatable = false)
    private SavedRouteEntity savedRoute;

    @Column(name = "segment_index", nullable = false)
    private int segmentIndex;

    @Column(name = "expected_arrival_time", nullable = false)
    private Instant expectedArrivalTime;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Column(name = "wind_speed_kmh")
    private Double windSpeedKmh;

    @Column(name = "wind_direction_degrees")
    private Double windDirectionDegrees;

    @Column(name = "precipitation_probability_percent")
    private Double precipitationProbabilityPercent;

    @Column(name = "precipitation_mm")
    private Double precipitationMm;

    @Column(name = "thunderstorm_probability_percent")
    private Double thunderstormProbabilityPercent;

    @Column(name = "geometry_json", columnDefinition = "text")
    private String geometryJson;

    protected SavedRouteWeatherSegmentEntity() {
        // Wird von JPA benötigt.
    }

    public SavedRouteWeatherSegmentEntity(
            int segmentIndex,
            Instant expectedArrivalTime,
            int score,
            String color,
            Double windSpeedKmh,
            Double windDirectionDegrees,
            Double precipitationProbabilityPercent,
            Double precipitationMm,
            Double thunderstormProbabilityPercent,
            String geometryJson
    ) {
        this.segmentIndex = segmentIndex;
        this.expectedArrivalTime = expectedArrivalTime;
        this.score = score;
        this.color = color;
        this.windSpeedKmh = windSpeedKmh;
        this.windDirectionDegrees = windDirectionDegrees;
        this.precipitationProbabilityPercent = precipitationProbabilityPercent;
        this.precipitationMm = precipitationMm;
        this.thunderstormProbabilityPercent = thunderstormProbabilityPercent;
        this.geometryJson = geometryJson;
    }

    void assignTo(SavedRouteEntity savedRoute) {
        this.savedRoute = savedRoute;
    }

    public int getSegmentIndex() { return segmentIndex; }
    public Instant getExpectedArrivalTime() { return expectedArrivalTime; }
    public int getScore() { return score; }
    public String getColor() { return color; }
    public Double getWindSpeedKmh() { return windSpeedKmh; }
    public Double getWindDirectionDegrees() { return windDirectionDegrees; }

    public Double getPrecipitationProbabilityPercent() {
        return precipitationProbabilityPercent;
    }

    public Double getPrecipitationMm() { return precipitationMm; }

    public Double getThunderstormProbabilityPercent() {
        return thunderstormProbabilityPercent;
    }

    public String getGeometryJson() { return geometryJson; }
}