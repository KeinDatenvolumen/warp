// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.infrastructure.mcp;

import de.warp.weather.application.WeatherScoringService;
import de.warp.weather.application.WeatherScoringService.ScoringActivityProfile;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherEvaluation;
import de.warp.weather.domain.model.WeatherSegment;
import de.warp.weather.infrastructure.persistence.WeatherEvaluationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// [KI-Log Nr. KI-19] KI-generiert (Google Antigravity Gemini 3.8 Flash High): Deterministische MCP-Tools (Ereigniskarte 12)
@Component
public class WeatherMcpTools {

    private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(BERLIN_ZONE);

    private final WeatherScoringService weatherScoringService;
    private final WeatherEvaluationRepository evaluationRepository;
    private final Map<String, WeatherEvaluation> ephemeralEvaluations = new ConcurrentHashMap<>();

    @Autowired
    public WeatherMcpTools(
            WeatherScoringService weatherScoringService,
            @Autowired(required = false) WeatherEvaluationRepository evaluationRepository
    ) {
        this.weatherScoringService = Objects.requireNonNull(weatherScoringService);
        this.evaluationRepository = evaluationRepository;
    }

    public void registerEphemeralEvaluation(String key, WeatherEvaluation evaluation) {
        if (key != null && evaluation != null) {
            ephemeralEvaluations.put(key, evaluation);
        }
    }

    @McpTool(
            name = "get_route_weather_telemetry",
            description = "Liefert die vollständige Segment-Telemetrie einer Route unterteilt in bekannte Standardparameter (Temperatur, Wind, Niederschlag, Gewitter, Score) und versteckte Tiefenparameter (Gefühlte Effektivtemperatur, Taupunkt-Spread, Fahrbahnnässe-Index, Böendifferenz, Bodentemperatur 0cm, Sichtweite, CAPE-Konvektionsenergie, Globalstrahlung, UV-Index)."
    )
    public Map<String, Object> getRouteWeatherTelemetry(
            @McpParam(name = "routeCalculationId", description = "ID der Routenberechnung")
            String routeCalculationId,
            @McpParam(name = "activityProfileCode", description = "Aktivitätsprofil (BICYCLE, MOTORCYCLE, HIKING)")
            String activityProfileCode
    ) {
        WeatherEvaluation evaluation = resolveEvaluation(routeCalculationId);
        ScoringActivityProfile profile = ScoringActivityProfile.fromCode(activityProfileCode);

        List<Map<String, Object>> segmentTelemetry = new ArrayList<>();
        if (evaluation != null && evaluation.segments() != null) {
            List<WeatherSegment> segments = evaluation.segments();
            for (int i = 0; i < segments.size(); i++) {
                WeatherSegment seg = segments.get(i);
                WeatherCondition cond = seg.condition();
                double nextDistMeters = (i + 1 < segments.size())
                        ? segments.get(i + 1).distanceFromStartMeters()
                        : seg.distanceFromStartMeters() + 5000;

                Map<String, Object> known = new LinkedHashMap<>();
                known.put("temperatureCelsius", round1(cond != null ? cond.temperatureCelsius() : null));
                known.put("windSpeedKmh", round1(cond != null ? cond.windSpeedKmh() : null));
                known.put("windDirectionDegrees", round1(cond != null ? cond.windDirectionDegrees() : null));
                known.put("precipitationProbabilityPercent", round1(cond != null ? cond.precipitationProbabilityPercent() : null));
                known.put("precipitationMm", round1(cond != null ? cond.precipitationMm() : null));
                known.put("thunderstormProbabilityPercent", round1(cond != null ? cond.thunderstormProbabilityPercent() : null));
                known.put("weatherCode", cond != null ? cond.weatherCode() : 0);
                known.put("segmentScore", seg.score());
                known.put("limitingFactor", seg.limitingFactor());
                known.put("reasonLabel", seg.reasonLabel());

                double t2m = cond != null && cond.temperatureCelsius() != null ? cond.temperatureCelsius() : 18.0;
                double rh = cond != null && cond.relativeHumidityPercent() != null ? cond.relativeHumidityPercent() : 60.0;
                double dewPoint = cond != null && cond.dewPointCelsius() != null
                        ? cond.dewPointCelsius()
                        : (t2m - (100.0 - rh) / 5.0);
                double dewSpread = t2m - dewPoint;
                double windSpeed = cond != null && cond.windSpeedKmh() != null ? cond.windSpeedKmh() : 0.0;
                double windGusts = cond != null && cond.windGustsKmh() != null ? cond.windGustsKmh() : windSpeed;
                double wetRoadPct = cond != null && cond.wetRoadIndex() != null ? cond.wetRoadIndex() * 100.0 : 0.0;

                Map<String, Object> hidden = new LinkedHashMap<>();
                hidden.put("apparentTemperatureCelsius", round1(cond != null && cond.apparentTemperatureCelsius() != null ? cond.apparentTemperatureCelsius() : t2m));
                hidden.put("relativeHumidityPercent", round1(rh));
                hidden.put("dewPointCelsius", round1(dewPoint));
                hidden.put("dewPointSpreadCelsius", round1(dewSpread));
                hidden.put("wetRoadIndexPercent", round1(wetRoadPct));
                hidden.put("windGustsKmh", round1(windGusts));
                hidden.put("gustDeltaKmh", round1(Math.max(0.0, windGusts - windSpeed)));
                hidden.put("soilTemperature0cmCelsius", round1(cond != null && cond.soilTemperatureCelsius() != null ? cond.soilTemperatureCelsius() : t2m));
                hidden.put("visibilityMeters", round1(cond != null && cond.visibilityMeters() != null ? cond.visibilityMeters() : 24000.0));
                hidden.put("capeJoulesPerKg", round1(cond != null && cond.capeJkg() != null ? cond.capeJkg() : 0.0));
                hidden.put("shortwaveRadiationWm2", round1(cond != null && cond.shortwaveRadiationWm2() != null ? cond.shortwaveRadiationWm2() : 0.0));
                hidden.put("uvIndex", round1(cond != null && cond.uvIndex() != null ? cond.uvIndex() : 2.0));
                hidden.put("snowfallCm", round1(cond != null && cond.snowfallCm() != null ? cond.snowfallCm() : 0.0));

                Map<String, Object> segEntry = new LinkedHashMap<>();
                segEntry.put("segmentIndex", seg.segmentIndex());
                segEntry.put("distanceStartKm", round1(seg.distanceFromStartMeters() / 1000.0));
                segEntry.put("distanceEndKm", round1(nextDistMeters / 1000.0));
                segEntry.put("expectedArrivalTimeLocal", TIME_FORMATTER.format(seg.expectedArrivalTime()));
                segEntry.put("knownParameters", known);
                segEntry.put("hiddenParameters", hidden);
                segmentTelemetry.add(segEntry);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("routeCalculationId", routeCalculationId);
        result.put("activityProfileCode", profile.name());
        result.put("overallScore", evaluation != null ? evaluation.overallScore() : 0);
        result.put("thunderstormCapApplied", evaluation != null && evaluation.thunderstormCapApplied());
        result.put("segmentCount", segmentTelemetry.size());
        result.put("segments", segmentTelemetry);
        return result;
    }

    @McpTool(
            name = "detect_hidden_meteorological_anomalies",
            description = "Untersucht alle Segmente auf versteckte meteorologische Gefahren (Restnässe trotz Trockenheit, Taupunkt-Spread/Visierbeschlag, Bodenfrost 0cm, Böenscherung, CAPE-Gewitterlabilität, Strahlungs-/Windchill-Belastung)."
    )
    public Map<String, Object> detectHiddenMeteorologicalAnomalies(
            @McpParam(name = "routeCalculationId", description = "ID der Routenberechnung")
            String routeCalculationId,
            @McpParam(name = "activityProfileCode", description = "Aktivitätsprofil (BICYCLE, MOTORCYCLE, HIKING)")
            String activityProfileCode
    ) {
        WeatherEvaluation evaluation = resolveEvaluation(routeCalculationId);
        ScoringActivityProfile profile = ScoringActivityProfile.fromCode(activityProfileCode);
        List<WeatherSegment> segments = evaluation != null && evaluation.segments() != null
                ? evaluation.segments()
                : List.of();

        double maxWetRoadPct = 0.0;
        double minPrecipDuringWetRoad = 0.0;
        double minDewSpread = 99.0;
        double maxHumidity = 0.0;
        double minSoilTemp = 99.0;
        double airTempAtMinSoil = 18.0;
        double maxCape = 0.0;
        double maxGustKmh = 0.0;
        double maxGustDelta = 0.0;
        double minVisibilityMeters = 24000.0;
        double maxRadiationWm2 = 0.0;
        double maxUvIndex = 0.0;
        double minApparentTemp = 99.0;
        double maxApparentTemp = -99.0;

        for (WeatherSegment seg : segments) {
            WeatherCondition c = seg.condition();
            if (c == null) {
                continue;
            }
            double t2m = c.temperatureCelsius() != null ? c.temperatureCelsius() : 18.0;
            double tApp = c.apparentTemperatureCelsius() != null ? c.apparentTemperatureCelsius() : t2m;
            double rh = c.relativeHumidityPercent() != null ? c.relativeHumidityPercent() : 60.0;
            double td = c.dewPointCelsius() != null ? c.dewPointCelsius() : (t2m - (100.0 - rh) / 5.0);
            double spread = Math.max(0.0, t2m - td);
            double wetPct = c.wetRoadIndex() != null ? c.wetRoadIndex() * 100.0 : 0.0;
            double precip = c.precipitationMm() != null ? c.precipitationMm() : 0.0;
            double soil = c.soilTemperatureCelsius() != null ? c.soilTemperatureCelsius() : t2m;
            double cape = c.capeJkg() != null ? c.capeJkg() : 0.0;
            double wind = c.windSpeedKmh() != null ? c.windSpeedKmh() : 0.0;
            double gust = c.windGustsKmh() != null ? Math.max(wind, c.windGustsKmh()) : wind;
            double vis = c.visibilityMeters() != null ? c.visibilityMeters() : 24000.0;
            double rad = c.shortwaveRadiationWm2() != null ? c.shortwaveRadiationWm2() : 0.0;
            double uv = c.uvIndex() != null ? c.uvIndex() : 2.0;

            if (wetPct > maxWetRoadPct) {
                maxWetRoadPct = wetPct;
                minPrecipDuringWetRoad = precip;
            }
            if (spread < minDewSpread) {
                minDewSpread = spread;
            }
            if (rh > maxHumidity) {
                maxHumidity = rh;
            }
            if (soil < minSoilTemp) {
                minSoilTemp = soil;
                airTempAtMinSoil = t2m;
            }
            if (cape > maxCape) {
                maxCape = cape;
            }
            if (gust > maxGustKmh) {
                maxGustKmh = gust;
            }
            if (gust - wind > maxGustDelta) {
                maxGustDelta = gust - wind;
            }
            if (vis < minVisibilityMeters) {
                minVisibilityMeters = vis;
            }
            if (rad > maxRadiationWm2) {
                maxRadiationWm2 = rad;
            }
            if (uv > maxUvIndex) {
                maxUvIndex = uv;
            }
            if (tApp < minApparentTemp) {
                minApparentTemp = tApp;
            }
            if (tApp > maxApparentTemp) {
                maxApparentTemp = tApp;
            }
        }

        if (segments.isEmpty()) {
            minDewSpread = 6.0;
            minSoilTemp = 15.0;
            minApparentTemp = 18.0;
            maxApparentTemp = 18.0;
        }

        Map<String, Object> anomalies = new LinkedHashMap<>();
        anomalies.put("activityProfile", profile.name());
        anomalies.put("maxWetRoadIndexPercent", round1(maxWetRoadPct));
        anomalies.put("precipAtMaxWetRoadMm", round1(minPrecipDuringWetRoad));
        anomalies.put("minDewPointSpreadCelsius", round1(minDewSpread));
        anomalies.put("maxRelativeHumidityPercent", round1(maxHumidity));
        anomalies.put("minSoilTemperature0cmCelsius", round1(minSoilTemp));
        anomalies.put("airTempAtMinSoilCelsius", round1(airTempAtMinSoil));
        anomalies.put("maxCapeJoulesPerKg", round1(maxCape));
        anomalies.put("maxWindGustsKmh", round1(maxGustKmh));
        anomalies.put("maxGustShearDeltaKmh", round1(maxGustDelta));
        anomalies.put("minVisibilityMeters", round1(minVisibilityMeters));
        anomalies.put("maxShortwaveRadiationWm2", round1(maxRadiationWm2));
        anomalies.put("maxUvIndex", round1(maxUvIndex));
        anomalies.put("apparentTemperatureRangeCelsius",
                String.format(Locale.ROOT, "%.1f bis %.1f", minApparentTemp, maxApparentTemp));
        return anomalies;
    }

    @McpTool(
            name = "get_activity_profile_vulnerability_matrix",
            description = "Liefert die physikalischen Grenzwerte, Gewichtungen und Sicherheitsrisiken des Aktivitätsprofils (BICYCLE, MOTORCYCLE, HIKING)."
    )
    public Map<String, Object> getActivityProfileVulnerabilityMatrix(
            @McpParam(name = "activityProfileCode", description = "Aktivitätsprofil (BICYCLE, MOTORCYCLE, HIKING)")
            String activityProfileCode
    ) {
        ScoringActivityProfile profile = ScoringActivityProfile.fromCode(activityProfileCode);
        Map<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("profileCode", profile.name());
        matrix.put("displayName", switch (profile) {
            case MOTORCYCLE -> "Motorrad";
            case HIKING -> "Wandern";
            case BICYCLE -> "Fahrrad";
        });
        matrix.put("defaultSpeedKmh", profile.defaultSpeedKmh());
        matrix.put("optimalTemperatureBandCelsius",
                String.format(Locale.ROOT, "%.0f°C – %.0f°C", profile.tMin(), profile.tMax()));
        matrix.put("criticalHeadwindKmh", profile.vCritHead());
        matrix.put("criticalCrosswindKmh", profile.vCritCross());
        matrix.put("safeGustThresholdKmh", profile.vSafeGust());
        matrix.put("criticalStormGustLimitKmh", profile.vLimitStorm());
        matrix.put("wetRoadGripPenaltyFactor", profile.alphaRoad());
        matrix.put("visorFoggingSensitivity", profile.etaVisor());
        matrix.put("specificSensitivities", switch (profile) {
            case MOTORCYCLE -> List.of(
                    "Hohe Empfindlichkeit gegenüber Restnässe (wetRoadIndex) und Bitumen-/Laubglätte in Kurven bei ~80 km/h",
                    "Akutes Visierbeschlag-Risiko bei geringem Taupunkt-Spread (< 2,5°C) und hoher Luftfeuchte",
                    "Starker Fahrtwind-Windchill bei kühlen Temperaturen sowie Seitenwind-Versatz durch Böen (windGusts)",
                    "Überfrierende Nässe / Bodenfrost (soilTemperature0cm <= 1°C) extrem kritisch für Zweirad-Traktion"
            );
            case HIKING -> List.of(
                    "Lange Expositionszeit gegenüber Gewitterzellen (CAPE), Starkregen und UV-Strahlung (uvIndex / shortwaveRadiation)",
                    "Schwüle- und Hitzestress bei hohem Taupunkt (> 16°C) und direkter Sonneneinstrahlung",
                    "Rutschgefahr auf aufgeweichten Pfaden bei erhöhtem wetRoadIndex oder Schneefall",
                    "Orientierungsverlust bei geringer Sichtweite (visibilityMeters < 1000 m)"
            );
            case BICYCLE -> List.of(
                    "Hoher aerodynamischer Kraftverlust bei Gegenwind sowie Spurversatz durch böigen Seitenwind (windGusts)",
                    "Auskühlung durch Fahrtwind-Windchill (apparentTemperature) vs. Überhitzung an Anstiegen",
                    "Verlängerter Bremsweg und Spritzwasser bei erhöhter Fahrbahnnässe (wetRoadIndex)",
                    "Bodenfrost- und Reifglättegefahr bei niedriger Bodentemperatur (soilTemperature0cm)"
            );
        });
        return matrix;
    }

    public WeatherEvaluation resolveEvaluation(String routeCalculationId) {
        if (routeCalculationId == null || routeCalculationId.isBlank()) {
            return null;
        }
        WeatherEvaluation ephemeral = ephemeralEvaluations.get(routeCalculationId);
        if (ephemeral != null) {
            return ephemeral;
        }
        if (evaluationRepository != null) {
            return evaluationRepository
                    .findFirstByRouteCalculationIdOrderByCreatedAtDesc(routeCalculationId)
                    .map(entity -> new WeatherEvaluation(
                            entity.getEvaluationId(),
                            entity.getRouteCalculationId(),
                            entity.getRequestedByUserId(),
                            entity.getStatus(),
                            entity.getDepartureTime(),
                            entity.getOverallScore(),
                            entity.getScoreVersion(),
                            entity.isThunderstormCapApplied(),
                            entity.getWeatherDataGeneratedAt(),
                            entity.getWeatherDataExpiresAt(),
                            entity.getCreatedAt(),
                            entity.getSegments().stream()
                                    .map(seg -> new WeatherSegment(
                                            seg.getSegmentIndex(),
                                            seg.getExpectedArrivalTime(),
                                            seg.getScore(),
                                            seg.getColorHex(),
                                            seg.getDistanceFromStartMeters(),
                                            seg.toCondition(),
                                            List.of(),
                                            seg.getLimitingFactor(),
                                            seg.getReasonLabel()
                                    ))
                                    .toList()
                    ))
                    .orElse(null);
        }
        return null;
    }

    private Double round1(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return null;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}
