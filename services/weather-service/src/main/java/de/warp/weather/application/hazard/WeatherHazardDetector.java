// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.hazard;

import de.warp.weather.api.dto.CriticalSectionHazardDto;
import de.warp.weather.api.dto.HiddenParameterInsightDto;
import de.warp.weather.application.WeatherScoringService;
import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherEvaluation;
import de.warp.weather.domain.model.WeatherForecastSeries;
import de.warp.weather.domain.model.WeatherForecastTimeStep;
import de.warp.weather.domain.model.WeatherSegment;
import de.warp.weather.domain.model.WeatherValues;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class WeatherHazardDetector {

    private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(BERLIN_ZONE);
    private static final int WET_ROAD_INTERVALS = 8;

    public double computeWetRoadIndex(
            WeatherForecastSeries series,
            Instant slotTime,
            WeatherValues currentValues
    ) {
        if (series == null) return 0.0;

        double temp = currentValues.temperatureCelsius() != null ? currentValues.temperatureCelsius() : 18.0;
        double rad = currentValues.shortwaveRadiationWattsPerSquareMeter() != null ? currentValues.shortwaveRadiationWattsPerSquareMeter() : 150.0;
        double rh = currentValues.relativeHumidityPercent() != null ? currentValues.relativeHumidityPercent() : 60.0;
        double tauDry = WeatherScoringService.calculateDryingTimeConstantHours(temp, rad, rh);

        List<WeatherForecastTimeStep> steps = series.minutely15() != null && !series.minutely15().isEmpty()
                ? series.minutely15()
                : series.hourly();

        if (steps == null || steps.isEmpty()) return 0.0;

        boolean isHourly = steps == series.hourly();
        double pAcc = 0.0;
        for (int k = 0; k <= WET_ROAD_INTERVALS; k++) {
            Instant target = slotTime.minus(Duration.ofMinutes(15L * k));
            WeatherForecastTimeStep closest = selectClosest(steps, target);
            long diffMin = Math.abs(Duration.between(closest.time(), target).toMinutes());
            if (diffMin <= (isHourly ? 45 : 15) && closest.values().precipitationMillimeters() != null) {
                double p15 = closest.values().precipitationMillimeters();
                if (isHourly) p15 /= 4.0;
                pAcc += Math.max(0.0, p15) * Math.exp(-(0.25 * k) / tauDry);
            }
        }

        return Math.clamp(1.0 - Math.exp(-pAcc / 1.2), 0.0, 1.0);
    }

    public double deriveThunderstormProbability(
            Integer weatherCode,
            Double cape,
            Double liftedIndex,
            double precipitationProbabilityPercent
    ) {
        double probability = 0.0;

        if (weatherCode != null) {
            if (weatherCode == 99) {
                probability = 100.0;
            } else if (weatherCode == 96) {
                probability = 90.0;
            } else if (weatherCode == 95) {
                probability = Math.max(80.0, precipitationProbabilityPercent);
            } else if (weatherCode == 82) {
                probability = 55.0;
            }
        }

        if (cape != null && cape > 100.0) {
            double capeRisk;
            if (cape >= 2000.0) {
                capeRisk = 85.0;
            } else if (cape >= 600.0) {
                if (precipitationProbabilityPercent > 30.0) {
                    capeRisk = 50.0 + Math.min(35.0, ((cape - 600.0) / 1400.0) * 35.0);
                } else {
                    capeRisk = 35.0 + ((cape - 600.0) / 1400.0) * 25.0;
                }
            } else {
                capeRisk = ((cape - 100.0) / 500.0) * 30.0;
            }
            probability = Math.max(probability, capeRisk);
        }

        if (liftedIndex != null && liftedIndex <= -4.0) {
            probability = Math.min(100.0, probability + 15.0);
        }

        return Math.clamp(Math.round(probability * 10.0) / 10.0, 0.0, 100.0);
    }

    public List<HiddenParameterInsightDto> buildDeterministicHiddenInsights(
            WeatherEvaluation evaluation,
            String profileLabel
    ) {
        List<WeatherSegment> segments = evaluation.segments();

        double maxWetRoad = 0.0;
        double minDewSpread = 99.0;
        double maxHumidity = 0.0;
        double minSoilTemp = 99.0;
        double maxGust = 0.0;
        double maxWind = 0.0;
        double maxCape = 0.0;
        double minVis = 24000.0;
        double maxUv = 0.0;
        double maxRadiation = 0.0;
        double minAppTemp = 99.0;
        double maxAppTemp = -99.0;

        for (WeatherSegment seg : segments) {
            WeatherCondition c = seg.condition();
            if (c == null) continue;

            double t2m = c.temperatureCelsius() != null ? c.temperatureCelsius() : 18.0;
            double tApp = c.apparentTemperatureCelsius() != null ? c.apparentTemperatureCelsius() : t2m;
            double rh = c.relativeHumidityPercent() != null ? c.relativeHumidityPercent() : 60.0;
            double td = c.dewPointCelsius() != null ? c.dewPointCelsius() : (t2m - (100.0 - rh) / 5.0);
            double spread = Math.max(0.0, t2m - td);
            double wet = c.wetRoadIndex() != null ? c.wetRoadIndex() * 100.0 : 0.0;
            double soil = c.soilTemperatureCelsius() != null ? c.soilTemperatureCelsius() : t2m;
            double wind = c.windSpeedKmh() != null ? c.windSpeedKmh() : 0.0;
            double gust = c.windGustsKmh() != null ? Math.max(wind, c.windGustsKmh()) : wind;
            double cape = c.capeJkg() != null ? c.capeJkg() : 0.0;
            double vis = c.visibilityMeters() != null ? c.visibilityMeters() : 24000.0;
            double uv = c.uvIndex() != null ? c.uvIndex() : 2.0;
            double rad = c.shortwaveRadiationWm2() != null ? c.shortwaveRadiationWm2() : 0.0;

            maxWetRoad = Math.max(maxWetRoad, wet);
            minDewSpread = Math.min(minDewSpread, spread);
            maxHumidity = Math.max(maxHumidity, rh);
            minSoilTemp = Math.min(minSoilTemp, soil);
            maxGust = Math.max(maxGust, gust);
            maxWind = Math.max(maxWind, wind);
            maxCape = Math.max(maxCape, cape);
            minVis = Math.min(minVis, vis);
            maxUv = Math.max(maxUv, uv);
            maxRadiation = Math.max(maxRadiation, rad);
            minAppTemp = Math.min(minAppTemp, tApp);
            maxAppTemp = Math.max(maxAppTemp, tApp);
        }

        if (segments.isEmpty()) {
            minDewSpread = 5.0;
            minSoilTemp = 14.0;
            minAppTemp = 16.0;
            maxAppTemp = 18.0;
        }

        List<HiddenParameterInsightDto> insights = new ArrayList<>();

        String wetStatus = maxWetRoad >= 60 ? "WARNING" : maxWetRoad >= 25 ? "NOTICE" : "OPTIMAL";
        insights.add(new HiddenParameterInsightDto(
                "WET_ROAD_INDEX",
                "Fahrbahnnässe-Index (2h-Rückblick)",
                "ROAD_SURFACE",
                String.format(Locale.GERMAN, "Max. %.0f %% Oberflächennässe", maxWetRoad),
                wetStatus,
                maxWetRoad >= 25
                        ? String.format(Locale.GERMAN,
                        "Durch vorangegangene Niederschläge und Verdunstungsträgheit bleibt der Untergrund bis zu %.0f %% nass – für %s bedeutet das reduzierten Reifengrip und längere Bremswege selbst in regenfreien Phasen.",
                        maxWetRoad, profileLabel)
                        : "Durchgehend trockene Straßen- und Wegoberfläche ohne versteckte Restnässe aus den Vorstunden."
        ));

        String dewStatus = minDewSpread <= 2.0 ? "WARNING" : minDewSpread <= 4.0 ? "NOTICE" : "OPTIMAL";
        insights.add(new HiddenParameterInsightDto(
                "DEW_POINT_SPREAD",
                "Taupunkt-Spread & Beschlagneigung",
                "VISIBILITY_RADIATION",
                String.format(Locale.GERMAN, "Spread %.1f °C (max. %.0f %% Feuchte)", minDewSpread, maxHumidity),
                dewStatus,
                minDewSpread <= 3.0
                        ? String.format(Locale.GERMAN,
                        "Lufttemperatur und Taupunkt liegen zeitweise nur %.1f °C auseinander. Hohes Risiko für beschlagene Brillen/Visiere und feuchte Fahrbahnfilme in Senken.",
                        minDewSpread)
                        : String.format(Locale.GERMAN,
                        "Komfortabler Taupunkt-Abstand (%.1f °C) – kein Beschlagen von Visier oder Sportbrille zu erwarten.",
                        minDewSpread)
        ));

        double gustDelta = Math.max(0.0, maxGust - maxWind);
        String gustStatus = maxGust >= 45 ? "CRITICAL" : maxGust >= 30 || gustDelta >= 15 ? "WARNING" : "OPTIMAL";
        insights.add(new HiddenParameterInsightDto(
                "WIND_GUST_SHEAR",
                "Böenscherung & Seitenwind-Impuls",
                "AERODYNAMICS",
                String.format(Locale.GERMAN, "Böen bis %.0f km/h (Δ +%.0f km/h)", maxGust, gustDelta),
                gustStatus,
                maxGust >= 30
                        ? String.format(Locale.GERMAN,
                        "Während der mittlere Wind moderat wirkt, treten Böenspitzen bis %.0f km/h auf. Beim Profil %s führt dies an Waldschneisen und Brücken zu plötzlichem Seitenversatz.",
                        maxGust, profileLabel)
                        : String.format(Locale.GERMAN,
                        "Geringe Böenscherung (Spitzen bis %.0f km/h) – ruhiges und spurstabiles Verhalten auf der gesamten Strecke.",
                        maxGust)
        ));

        String soilStatus = minSoilTemp <= 1.0 ? "CRITICAL" : minSoilTemp <= 4.0 ? "WARNING" : "OPTIMAL";
        insights.add(new HiddenParameterInsightDto(
                "SOIL_AND_THERMAL",
                "Bodentemperatur (0 cm) & Effektivklima",
                "THERMAL",
                String.format(Locale.GERMAN, "Boden min. %.1f °C · Gefühlt %.1f–%.1f °C", minSoilTemp, minAppTemp, maxAppTemp),
                soilStatus,
                minSoilTemp <= 3.5
                        ? String.format(Locale.GERMAN,
                        "Die unmittelbare Bodentemperatur sinkt auf %.1f °C. In schattigen Lagen droht Reif- oder Bodenfrostglätte, obwohl die 2m-Lufttemperatur höher liegt.",
                        minSoilTemp)
                        : String.format(Locale.GERMAN,
                        "Thermisch stabiler Untergrund (min. %.1f °C) bei einer gefühlten Temperaturspanne von %.1f bis %.1f °C unter Berücksichtigung von Fahrtwind und Strahlung.",
                        minSoilTemp, minAppTemp, maxAppTemp)
        ));

        String capeStatus = maxCape >= 600 ? "CRITICAL" : maxCape >= 250 || minVis < 3000 ? "WARNING" : "OPTIMAL";
        insights.add(new HiddenParameterInsightDto(
                "CAPE_AND_VISIBILITY",
                "Konvektionsenergie (CAPE) & Sicht/UV",
                "ATMOSPHERIC",
                String.format(Locale.GERMAN, "CAPE %.0f J/kg · Sicht %.1f km · UV %.1f", maxCape, minVis / 1000.0, maxUv),
                capeStatus,
                maxCape >= 300
                        ? String.format(Locale.GERMAN,
                        "Erhöhte atmosphärische Labilität (%.0f J/kg CAPE) deutet auf rasch aufquellende Schauer- oder Gewitterzellen und thermische Fallböen hin.",
                        maxCape)
                        : String.format(Locale.GERMAN,
                        "Atmosphärisch stabile Schichtung (%.0f J/kg CAPE) mit guter Fernsicht (min. %.1f km) und max. %.0f W/m² Globalstrahlung.",
                        maxCape, minVis / 1000.0, maxRadiation)
        ));

        return insights;
    }

    public List<CriticalSectionHazardDto> buildDeterministicCriticalSections(
            WeatherEvaluation evaluation,
            int totalDistanceMeters,
            String profileLabel
    ) {
        List<WeatherSegment> segments = evaluation.segments();
        if (segments.isEmpty()) {
            return List.of();
        }

        List<CriticalSectionHazardDto> hazards = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            WeatherSegment seg = segments.get(i);
            int nextDist = (i + 1 < segments.size())
                    ? segments.get(i + 1).distanceFromStartMeters()
                    : Math.max(totalDistanceMeters, seg.distanceFromStartMeters() + 5000);
            Instant endTime = (i + 1 < segments.size())
                    ? segments.get(i + 1).expectedArrivalTime()
                    : seg.expectedArrivalTime().plus(Duration.ofMinutes(15));

            String distRange = String.format(
                    Locale.GERMAN,
                    "km %.1f – %.1f",
                    seg.distanceFromStartMeters() / 1000.0,
                    nextDist / 1000.0
            );
            String timeWin = TIME_FORMATTER.format(seg.expectedArrivalTime())
                    + " – " + TIME_FORMATTER.format(endTime) + " Uhr";

            WeatherCondition c = seg.condition();
            int score = seg.score();
            String severity = score < 50 ? "DANGER" : score < 75 ? "WARNING" : "INFO";

            if (score < 80 || hazards.isEmpty()) {
                double gust = c != null && c.windGustsKmh() != null ? c.windGustsKmh() : 0.0;
                double wetPct = c != null && c.wetRoadIndex() != null ? c.wetRoadIndex() * 100.0 : 0.0;
                double tApp = c != null && c.apparentTemperatureCelsius() != null
                        ? c.apparentTemperatureCelsius()
                        : (c != null && c.temperatureCelsius() != null ? c.temperatureCelsius() : 18.0);

                String title = seg.reasonLabel() != null && !seg.reasonLabel().isBlank()
                        ? "Abschnitt " + (i + 1) + ": " + seg.reasonLabel()
                        : "Abschnitt " + (i + 1) + " (" + distRange + ")";

                String description = String.format(
                        Locale.GERMAN,
                        "Score %d/100 für %s. Gefühlte Temperatur %.1f °C, Böenspitzen bis %.0f km/h und %.0f %% berechnete Fahrbahnnässe.",
                        score, profileLabel, tApp, gust, wetPct
                );

                String recommendation = switch (seg.limitingFactor() != null ? seg.limitingFactor() : "") {
                    case "THUNDER_CAP" -> "Exponierte Höhenlagen meiden und Schutzmöglichkeit einplanen (Gewitter-Kappung aktiv).";
                    case "ICE_CAP" -> "Akute Glättegefahr durch niedrige Bodentemperatur – Geschwindigkeit stark reduzieren.";
                    case "WIND", "STORM_CAP" -> "Seitenwind- und Böenimpulse beachten, Lenker locker führen und Überholabstand vergrößern.";
                    case "PRECIP", "WET_ROAD" -> "Erhöhte Rutschgefahr auf nassem Belag – Schräglage in Kurven reduzieren und früher bremsen.";
                    default -> "Gleichmäßiges Tempo wählen und Sicht-/Wetterentwicklung im Blick behalten.";
                };

                hazards.add(new CriticalSectionHazardDto(
                        seg.segmentIndex(),
                        seg.segmentIndex(),
                        distRange,
                        timeWin,
                        score,
                        severity,
                        seg.limitingFactor() != null ? seg.limitingFactor() : "NONE",
                        title,
                        description,
                        recommendation
                ));
            }
        }

        return hazards.stream()
                .sorted(Comparator.comparingInt(CriticalSectionHazardDto::minSegmentScore))
                .limit(4)
                .toList();
    }

    private WeatherForecastTimeStep selectClosest(List<WeatherForecastTimeStep> steps, Instant target) {
        return steps.stream()
                .min(Comparator.comparingLong(s -> Math.abs(Duration.between(s.time(), target).toSeconds())))
                .orElseThrow();
    }
}
