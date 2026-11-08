// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.weather.infrastructure.persistence;

import de.warp.weather.domain.model.WeatherCondition;
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

    @Column(name = "wind_gusts_kmh")
    private Double windGustsKmh;

    @Column(name = "precipitation_probability")
    private Double precipitationProbabilityPercent;

    @Column(name = "precipitation_mm")
    private Double precipitationMm;

    @Column(name = "wet_road_index")
    private Double wetRoadIndex;

    @Column(name = "snowfall_cm")
    private Double snowfallCm;

    @Column(name = "thunderstorm_probability")
    private Double thunderstormProbabilityPercent;

    @Column(name = "temperature_celsius")
    private Double temperatureCelsius;

    @Column(name = "apparent_temperature_celsius")
    private Double apparentTemperatureCelsius;

    @Column(name = "relative_humidity_percent")
    private Double relativeHumidityPercent;

    @Column(name = "dew_point_celsius")
    private Double dewPointCelsius;

    @Column(name = "shortwave_radiation_wm2")
    private Double shortwaveRadiationWm2;

    @Column(name = "soil_temperature_celsius")
    private Double soilTemperatureCelsius;

    @Column(name = "visibility_meters")
    private Double visibilityMeters;

    @Column(name = "cape_jkg")
    private Double capeJkg;

    @Column(name = "uv_index")
    private Double uvIndex;

    @Column(name = "weather_code")
    private Integer weatherCode;

    @Column(name = "limiting_factor", length = 32)
    private String limitingFactor;

    @Column(name = "reason_label", length = 120)
    private String reasonLabel;

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
            WeatherCondition condition,
            String geometryGeojson,
            String limitingFactor,
            String reasonLabel
    ) {
        this.segmentIndex = segmentIndex;
        this.distanceFromStartMeters = distanceFromStartMeters;
        this.expectedArrivalTime = expectedArrivalTime;
        this.score = score;
        this.colorHex = colorHex;
        this.limitingFactor = limitingFactor;
        this.reasonLabel = reasonLabel;
        if (condition != null) {
            this.windSpeedKmh = condition.windSpeedKmh();
            this.windDirectionDegrees = condition.windDirectionDegrees();
            this.windGustsKmh = condition.windGustsKmh();
            this.precipitationProbabilityPercent = condition.precipitationProbabilityPercent();
            this.precipitationMm = condition.precipitationMm();
            this.wetRoadIndex = condition.wetRoadIndex();
            this.snowfallCm = condition.snowfallCm();
            this.thunderstormProbabilityPercent = condition.thunderstormProbabilityPercent();
            this.temperatureCelsius = condition.temperatureCelsius();
            this.apparentTemperatureCelsius = condition.apparentTemperatureCelsius();
            this.relativeHumidityPercent = condition.relativeHumidityPercent();
            this.dewPointCelsius = condition.dewPointCelsius();
            this.shortwaveRadiationWm2 = condition.shortwaveRadiationWm2();
            this.soilTemperatureCelsius = condition.soilTemperatureCelsius();
            this.visibilityMeters = condition.visibilityMeters();
            this.capeJkg = condition.capeJkg();
            this.uvIndex = condition.uvIndex();
            this.weatherCode = condition.weatherCode();
        }
        this.geometryGeojson = geometryGeojson;
    }

    public WeatherSegmentEntity(
            int segmentIndex,
            int distanceFromStartMeters,
            Instant expectedArrivalTime,
            int score,
            String colorHex,
            WeatherCondition condition,
            String geometryGeojson
    ) {
        this(
                segmentIndex,
                distanceFromStartMeters,
                expectedArrivalTime,
                score,
                colorHex,
                condition,
                geometryGeojson,
                null,
                null
        );
    }

    void setEvaluation(WeatherEvaluationEntity evaluation) {
        this.evaluation = evaluation;
    }

    public WeatherCondition toCondition() {
        return new WeatherCondition(
                windSpeedKmh,
                windDirectionDegrees,
                windGustsKmh,
                precipitationProbabilityPercent,
                precipitationMm,
                wetRoadIndex,
                snowfallCm,
                thunderstormProbabilityPercent,
                temperatureCelsius,
                apparentTemperatureCelsius,
                relativeHumidityPercent,
                dewPointCelsius,
                shortwaveRadiationWm2,
                soilTemperatureCelsius,
                visibilityMeters,
                capeJkg,
                uvIndex,
                weatherCode
        );
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

    public String getLimitingFactor() {
        return limitingFactor;
    }

    public String getReasonLabel() {
        return reasonLabel;
    }

    public String getGeometryGeojson() {
        return geometryGeojson;
    }
}
