package de.warp.weather.infrastructure.persistence;

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
@Table(name = "weather_segment", schema = "warp_weather")
public class WeatherSegmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "segment_id", nullable = false)
    private Long segmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private WeatherEvaluationEntity evaluation;

    @Column(name = "segment_index", nullable = false)
    private int segmentIndex;

    @Column(name = "distance_from_start_meters", nullable = false)
    private int distanceFromStartMeters;

    @Column(name = "expected_arrival_time", nullable = false)
    private Instant expectedArrivalTime;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @Column(name = "wind_speed_kmh")
    private Double windSpeedKmh;

    @Column(name = "wind_direction_degrees")
    private Double windDirectionDegrees;

    @Column(name = "precipitation_probability")
    private Double precipitationProbabilityPercent;

    @Column(name = "precipitation_mm")
    private Double precipitationMm;

    @Column(name = "thunderstorm_probability")
    private Double thunderstormProbabilityPercent;

    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "weather_code")
    private Integer weatherCode;

    @Column(name = "geometry_geojson", columnDefinition = "TEXT")
    private String geometryGeojson;

    protected WeatherSegmentEntity() {
    }

    public WeatherSegmentEntity(
            int segmentIndex,
            int distanceFromStartMeters,
            Instant expectedArrivalTime,
            int score,
            String colorHex,
            Double windSpeedKmh,
            Double windDirectionDegrees,
            Double precipitationProbabilityPercent,
            Double precipitationMm,
            Double thunderstormProbabilityPercent,
            Double temperatureCelsius,
            Integer weatherCode,
            String geometryGeojson
    ) {
        this.segmentIndex = segmentIndex;
        this.distanceFromStartMeters = distanceFromStartMeters;
        this.expectedArrivalTime = expectedArrivalTime;
        this.score = score;
        this.colorHex = colorHex;
        this.windSpeedKmh = windSpeedKmh;
        this.windDirectionDegrees = windDirectionDegrees;
        this.precipitationProbabilityPercent = precipitationProbabilityPercent;
        this.precipitationMm = precipitationMm;
        this.thunderstormProbabilityPercent = thunderstormProbabilityPercent;
        this.temperatureCelsius = temperatureCelsius;
        this.weatherCode = weatherCode;
        this.geometryGeojson = geometryGeojson;
    }

    void setEvaluation(WeatherEvaluationEntity evaluation) {
        this.evaluation = evaluation;
    }

    public Long getSegmentId() {
        return segmentId;
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public int getDistanceFromStartMeters() {
        return distanceFromStartMeters;
    }

    public Instant getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public int getScore() {
        return score;
    }

    public String getColorHex() {
        return colorHex;
    }

    public Double getWindSpeedKmh() {
        return windSpeedKmh;
    }

    public Double getWindDirectionDegrees() {
        return windDirectionDegrees;
    }

    public Double getPrecipitationProbabilityPercent() {
        return precipitationProbabilityPercent;
    }

    public Double getPrecipitationMm() {
        return precipitationMm;
    }

    public Double getThunderstormProbabilityPercent() {
        return thunderstormProbabilityPercent;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public Integer getWeatherCode() {
        return weatherCode;
    }

    public String getGeometryGeojson() {
        return geometryGeojson;
    }
}
