// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-223] (03c6e45) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement 101-step perceptual score scale, map casing, dynamic bottleneck badges and meteogram dock (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.weather.application;

import de.warp.weather.domain.model.WeatherCondition;
import de.warp.weather.domain.model.WeatherSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity Gemini 3.8 Flash High): 101-Stufen-Wetterscore & Gewitterkappung
@Service
public class WeatherScoringService {

    private static final double LN_2 = Math.log(2.0);

    private final WeatherScoringProperties properties;

    @Autowired
    public WeatherScoringService(WeatherScoringProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    public WeatherScoringService() {
        this(WeatherScoringProperties.defaults());
    }

    public int calculateSegmentScore(
            WeatherCondition condition,
            double movementBearingDegrees
    ) {
        return calculateSegmentScore(
                condition,
                movementBearingDegrees,
                ScoringActivityProfile.BICYCLE,
                ScoringActivityProfile.BICYCLE.defaultSpeedKmh()
        );
    }

    public int calculateSegmentScore(
            WeatherCondition condition,
            double movementBearingDegrees,
            String activityProfileCode,
            double segmentSpeedKmh
    ) {
        ScoringActivityProfile profile = ScoringActivityProfile.fromCode(activityProfileCode);
        return calculateSegmentScore(
                condition,
                movementBearingDegrees,
                profile,
                segmentSpeedKmh > 0.0 ? segmentSpeedKmh : profile.defaultSpeedKmh()
        );
    }

    public int calculateSegmentScore(
            WeatherCondition condition,
            double movementBearingDegrees,
            ScoringActivityProfile profile,
            double segmentSpeedKmh
    ) {
        return evaluateSegmentBreakdown(
                condition,
                movementBearingDegrees,
                profile,
                segmentSpeedKmh
        ).score();
    }

    public SegmentScoreBreakdown evaluateSegmentBreakdown(
            WeatherCondition condition,
            double movementBearingDegrees,
            ScoringActivityProfile profile,
            double segmentSpeedKmh
    ) {
        Objects.requireNonNull(condition, "condition must not be null");
        ScoringActivityProfile effectiveProfile = profile != null ? profile : ScoringActivityProfile.BICYCLE;
        double vSeg = segmentSpeedKmh > 0.0 ? segmentSpeedKmh : effectiveProfile.defaultSpeedKmh();

        // 1. Resolve physical weather variables with defensive defaults
        double t2m = condition.temperatureCelsius() != null ? condition.temperatureCelsius() : 18.0;
        double tApp = condition.apparentTemperatureCelsius() != null ? condition.apparentTemperatureCelsius() : t2m;
        double rh = condition.relativeHumidityPercent() != null
                ? clamp(condition.relativeHumidityPercent(), 0.0, 100.0)
                : 60.0;
        double td = condition.dewPointCelsius() != null
                ? condition.dewPointCelsius()
                : (t2m - (100.0 - rh) / 5.0);
        double rSw = condition.shortwaveRadiationWm2() != null
                ? Math.max(0.0, condition.shortwaveRadiationWm2())
                : 0.0;
        double tSoil = condition.soilTemperatureCelsius() != null
                ? condition.soilTemperatureCelsius()
                : t2m;

        double vWind = condition.windSpeedKmh() != null ? Math.max(0.0, condition.windSpeedKmh()) : 0.0;
        double vGust = condition.windGustsKmh() != null
                ? Math.max(vWind, condition.windGustsKmh())
                : vWind;

        double iPrecip = condition.precipitationMm() != null ? Math.max(0.0, condition.precipitationMm()) : 0.0;
        double pPrecipNorm = condition.precipitationProbabilityPercent() != null
                ? clamp(condition.precipitationProbabilityPercent() / 100.0, 0.0, 1.0)
                : 0.0;
        double pSnow = condition.snowfallCm() != null ? Math.max(0.0, condition.snowfallCm()) : 0.0;

        double wRoad = condition.wetRoadIndex() != null
                ? clamp(condition.wetRoadIndex(), 0.0, 1.0)
                : estimateFallbackWetRoadIndex(iPrecip, t2m, rSw, rh);

        double visibility = condition.visibilityMeters() != null
                ? Math.max(10.0, condition.visibilityMeters())
                : 24_000.0;
        double cape = condition.capeJkg() != null ? Math.max(0.0, condition.capeJkg()) : 0.0;
        double uv = condition.uvIndex() != null ? Math.max(0.0, condition.uvIndex()) : 2.0;
        int wmo = condition.weatherCode() != null ? condition.weatherCode() : 0;

        // 2. Aerodynamics & Vector Decomposition
        double deltaTheta = condition.windDirectionDegrees() != null
                ? angularDifferenceDegrees(condition.windDirectionDegrees(), movementBearingDegrees)
                : 90.0;
        double deltaThetaRad = Math.toRadians(deltaTheta);
        double cosTheta = Math.cos(deltaThetaRad);
        double sinTheta = Math.sin(deltaThetaRad);

        double vLong = vWind * cosTheta;
        double vHead = Math.max(0.0, vLong);
        double vTail = Math.max(0.0, -vLong);
        double deltaVGust = Math.max(0.0, vGust - vWind);

        double lateralAngleFactor = Math.max(
                sinTheta,
                0.30 * Math.abs(Math.sin(2.0 * deltaThetaRad))
        );
        double vCrossEff = (vWind + effectiveProfile.gustFactorGamma() * deltaVGust) * lateralAngleFactor;

        double vRel = Math.hypot(vSeg + vWind * cosTheta, vWind * sinTheta);

        // 3.1 Thermal Comfort Score (S_temp)
        double tEff = calculateEffectiveTemperature(
                effectiveProfile,
                t2m,
                tApp,
                td,
                rSw,
                vRel,
                wRoad
        );
        double sTemp = scoreTemperature(effectiveProfile, tEff);

        // 3.2 Precipitation & Road Condition Score (S_precip)
        double sPrecip = scorePrecipitation(effectiveProfile, iPrecip, pPrecipNorm, wRoad);

        // 3.3 Aerodynamics & Stability Score (S_wind) + Tailwind Boost
        double sWind = scoreWind(effectiveProfile, vHead, vCrossEff, vGust);
        double tailwindBoost = calculateTailwindBoost(effectiveProfile, vTail, vCrossEff, vGust);

        // 3.4 Visibility, Visor Fogging & Radiation Score (S_vis)
        double sVis = scoreVisibility(effectiveProfile, visibility, t2m, td, uv);

        // 4.1 & 4.2 Weighted Linear Score & Bottleneck-Damped Raw Score
        double sLin = effectiveProfile.wTemp() * sTemp
                + effectiveProfile.wPrecip() * sPrecip
                + effectiveProfile.wWind() * sWind
                + effectiveProfile.wVis() * sVis;

        double sMin = Math.min(Math.min(sTemp, sPrecip), Math.min(sWind, sVis));
        double bottleneckDamping = 0.45 + 0.55 * Math.pow(clamp(sMin / 100.0, 0.0, 1.0), 0.6);
        double sRaw = sLin * bottleneckDamping;

        // 4.3 Hard Safety & Abort Caps
        double cThunder = calculateThunderstormCap(
                effectiveProfile,
                wmo,
                cape,
                pPrecipNorm,
                condition.thunderstormProbabilityPercent()
        );
        double cIce = calculateIceAndSnowCap(
                effectiveProfile,
                wmo,
                wRoad,
                t2m,
                tSoil,
                pSnow,
                vGust
        );
        double cStorm = calculateStormCap(effectiveProfile, vGust);

        double combined = Math.min(
                100.0,
                Math.min(
                        sRaw + tailwindBoost,
                        Math.min(cThunder, Math.min(cIce, cStorm))
                )
        );
        int finalScore = clampInt((int) Math.round(Math.max(0.0, combined)), 0, 100);

        String limitingFactor;
        String reasonLabel;
        if (cThunder <= 50.0 && cThunder < sRaw) {
            limitingFactor = "THUNDER_CAP";
            reasonLabel = "Max " + (int) Math.round(cThunder) + " · Gewitterrisiko";
        } else if (cIce < 100.0 && cIce < sRaw) {
            limitingFactor = "ICE_CAP";
            reasonLabel = (wRoad > 0.2 && Math.min(t2m, tSoil) <= 0.5)
                    ? "Überfrierende Nässe"
                    : "Schnee / Glätte";
        } else if (cStorm < 100.0 && cStorm < sRaw) {
            limitingFactor = "STORM_CAP";
            reasonLabel = "Sturmböen (" + Math.round(vGust) + " km/h)";
        } else if (finalScore >= 85) {
            if (tailwindBoost >= 1.5) {
                limitingFactor = "TAILWIND_BOOST";
                reasonLabel = "Rückenwind-Boost (+" + Math.round(tailwindBoost) + " Pkt.)";
            } else {
                limitingFactor = "NONE";
                reasonLabel = "Optimale Bedingungen";
            }
        } else {
            double minSub = Math.min(Math.min(sTemp, sPrecip), Math.min(sWind, sVis));
            if (minSub == sWind) {
                limitingFactor = "WIND";
                if (deltaTheta <= 45.0) {
                    reasonLabel = "Gegenwind (" + Math.round(vWind) + " km/h)";
                } else if (deltaTheta >= 135.0) {
                    reasonLabel = "Böiger Wind (" + Math.round(vGust) + " km/h)";
                } else {
                    reasonLabel = "Seitenwind (" + Math.round(vCrossEff) + " km/h)";
                }
            } else if (minSub == sPrecip) {
                if (wRoad >= 0.4 && iPrecip < 0.5) {
                    limitingFactor = "WET_ROAD";
                    reasonLabel = "Nasse Fahrbahn (" + Math.round(wRoad * 100) + "%)";
                } else {
                    limitingFactor = "PRECIP";
                    reasonLabel = iPrecip >= 1.0
                            ? "Schauer (" + String.format(Locale.ROOT, "%.1f", iPrecip) + " mm/h)"
                            : "Regenrisiko (" + Math.round(pPrecipNorm * 100) + "%)";
                }
            } else if (minSub == sTemp) {
                limitingFactor = "TEMP";
                if (tEff < 12.0) {
                    reasonLabel = "Kälte / Windchill (" + Math.round(tEff) + "°C)";
                } else {
                    reasonLabel = "Hitze (" + Math.round(tEff) + "°C)";
                }
            } else {
                limitingFactor = "VISIBILITY";
                reasonLabel = visibility < 1000
                        ? "Nebel / Sicht (" + Math.round(visibility) + " m)"
                        : "Sicht & Blendung";
            }
        }

        return new SegmentScoreBreakdown(
                finalScore,
                sRaw,
                sTemp,
                sPrecip,
                sWind,
                sVis,
                tailwindBoost,
                cThunder,
                cIce,
                cStorm,
                deltaTheta,
                tEff,
                wRoad,
                limitingFactor,
                reasonLabel
        );
    }

    public OverallScoreResult calculateOverallScore(List<WeatherSegment> segments) {
        return calculateOverallScore(segments, ScoringActivityProfile.BICYCLE, null);
    }

    public OverallScoreResult calculateOverallScore(
            List<WeatherSegment> segments,
            String activityProfileCode,
            List<Double> segmentDurationsSeconds
    ) {
        return calculateOverallScore(
                segments,
                ScoringActivityProfile.fromCode(activityProfileCode),
                segmentDurationsSeconds
        );
    }

    public OverallScoreResult calculateOverallScore(
            List<WeatherSegment> segments,
            ScoringActivityProfile profile,
            List<Double> segmentDurationsSeconds
    ) {
        Objects.requireNonNull(segments, "segments must not be null");
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("segments must not be empty");
        }

        ScoringActivityProfile effectiveProfile = profile != null ? profile : ScoringActivityProfile.BICYCLE;
        int n = segments.size();
        double[] weights = resolveNormalizedTimeWeights(segments, segmentDurationsSeconds);

        // 1. Time-weighted linear mean mu_S
        double muS = 0.0;
        int minSegmentScore = 100;
        for (int i = 0; i < n; i++) {
            int s = clampInt(segments.get(i).score(), 0, 100);
            muS += weights[i] * s;
            if (s < minSegmentScore) {
                minSegmentScore = s;
            }
        }

        // 2. Pessimistic Soft-Minimum M_tau (Log-Sum-Exp shifted by minSegmentScore for numerical stability)
        double tau = effectiveProfile.tauRoute();
        double sumExp = 0.0;
        for (int i = 0; i < n; i++) {
            int s = clampInt(segments.get(i).score(), 0, 100);
            sumExp += weights[i] * Math.exp(-(s - minSegmentScore) / tau);
        }
        double mTau = minSegmentScore - tau * Math.log(Math.max(1e-15, sumExp));

        // 3. Root-damped Bottleneck Bound C_route
        double cRoute = minSegmentScore + 0.45 * Math.sqrt(minSegmentScore * (100.0 - minSegmentScore));

        // 4. Domain Invariant [FR-WETT-03] on Route Level
        boolean thunderstormRiskDetected = segments.stream()
                .map(WeatherSegment::condition)
                .anyMatch(this::isElevatedThunderstormRisk);

        double cFrWett03 = thunderstormRiskDetected
                ? properties.thunderstormMaxOverallScore()
                : 100.0;

        double blendedScore = 0.35 * muS + 0.65 * mTau;
        double routeRaw = Math.min(cRoute, Math.min(cFrWett03, blendedScore));
        int finalRouteScore = clampInt((int) Math.round(routeRaw), 0, 100);

        return new OverallScoreResult(
                finalRouteScore,
                thunderstormRiskDetected,
                properties.scoreVersion()
        );
    }

    public boolean isElevatedThunderstormRisk(WeatherCondition condition) {
        if (condition == null) {
            return false;
        }
        if (condition.weatherCode() != null
                && (condition.weatherCode() >= 95 || condition.weatherCode() == 82)) {
            return true;
        }
        double cape = condition.capeJkg() != null ? condition.capeJkg() : 0.0;
        double precipProb = condition.precipitationProbabilityPercent() != null
                ? condition.precipitationProbabilityPercent()
                : 0.0;
        if (cape >= 600.0 && precipProb > 30.0) {
            return true;
        }
        double thunderstormProb = condition.thunderstormProbabilityPercent() != null
                ? condition.thunderstormProbabilityPercent()
                : 0.0;
        return thunderstormProb >= properties.thunderstormRiskThresholdPercent();
    }

    private static final String[] SCORE_COLORS_101 = new String[] {
            "#2A0845", "#2F0A46", "#340C46", "#390D47", "#3E0E47", "#430F48", "#491048", "#4E1049", "#531149", "#59114A",
            "#5E114A", "#64114A", "#6A104A", "#6F104B", "#750E4B", "#7B0D4B", "#800F4A", "#851148", "#8A1247", "#8F1445",
            "#941543", "#991642", "#9E1740", "#A3183D", "#A8193B", "#AD1A38", "#B31B35", "#B81B32", "#BD1C2E", "#C31D2A",
            "#C81D25", "#CA2225", "#CC2725", "#CE2B24", "#D12F24", "#D33324", "#D53723", "#D73B23", "#D93E23", "#DB4222",
            "#DD4522", "#DF4821", "#E14C20", "#E34F20", "#E6521F", "#E8551E", "#EA581D", "#EC5B1C", "#EE5E1B", "#F0611A",
            "#F26419", "#F3691A", "#F36D1B", "#F3721C", "#F4761E", "#F47A1F", "#F57E20", "#F58321", "#F58722", "#F58B23",
            "#F68F24", "#F69325", "#F69726", "#F69B28", "#F69F29", "#F6A22A", "#F6A62B", "#F6AA2C", "#F6AE2D", "#EEAF38",
            "#E7B040", "#DFB148", "#D7B24E", "#CFB355", "#C6B45A", "#BEB55F", "#B5B564", "#ACB669", "#A3B66D", "#9AB672",
            "#90B776", "#86B77A", "#7AB77D", "#6EB781", "#61B785", "#52B788", "#4FB78E", "#4CB794", "#48B799", "#45B79F",
            "#41B7A4", "#3DB7AA", "#39B6AF", "#35B6B4", "#31B6B9", "#2CB6BF", "#26B5C4", "#20B5C9", "#19B5CE", "#0FB4D3",
            "#00B4D8"
    };

    public String resolveScoreColor(int score) {
        int index = clampInt(score, 0, 100);
        return SCORE_COLORS_101[index];
    }

    static double calculateEffectiveTemperature(
            ScoringActivityProfile profile,
            double t2m,
            double tApp,
            double td,
            double rSw,
            double vRel,
            double wRoad
    ) {
        double vRelSafe = Math.max(1.0, vRel);
        return switch (profile) {
            case HIKING -> tApp + 0.004 * rSw + 0.20 * Math.max(0.0, td - 15.0);
            case BICYCLE -> t2m
                    - 0.14 * Math.max(0.0, 18.0 - t2m) * Math.pow(vRelSafe / 20.0, 0.16)
                    + 0.25 * Math.max(0.0, td - 14.0)
                    + 0.0025 * rSw;
            case MOTORCYCLE -> {
                double vPow = Math.pow(vRelSafe, 0.16);
                double tWc = 13.12 + 0.6215 * t2m - 11.37 * vPow + 0.3965 * t2m * vPow;
                double tHeat = t2m + 0.35 * Math.max(0.0, td - 15.0) + 0.0055 * rSw;
                double blend = sigmoid(t2m - 20.0);
                yield (1.0 - blend) * (tWc - 4.0 * wRoad) + blend * tHeat;
            }
        };
    }

    static double scoreTemperature(ScoringActivityProfile profile, double tEff) {
        if (tEff < profile.tMin()) {
            double delta = profile.tMin() - tEff;
            return 100.0 * Math.exp(-(delta * delta) / (2.0 * profile.sigmaCold() * profile.sigmaCold()));
        }
        if (tEff > profile.tMax()) {
            double delta = tEff - profile.tMax();
            return 100.0 * Math.exp(-(delta * delta) / (2.0 * profile.sigmaHot() * profile.sigmaHot()));
        }
        return 100.0;
    }

    static double scorePrecipitation(
            ScoringActivityProfile profile,
            double iPrecipMmPerHour,
            double pPrecipNorm,
            double wRoad
    ) {
        double iEff = iPrecipMmPerHour * (0.35 + 0.65 * pPrecipNorm)
                + 0.60 * Math.pow(Math.max(0.0, pPrecipNorm - 0.50), 2.0);
        double roadFactor = Math.max(0.0, 1.0 - profile.alphaRoad() * clamp(wRoad, 0.0, 1.0));
        double rainDecay = iEff > 0.0
                ? Math.exp(-profile.lambdaRain() * Math.pow(iEff, profile.betaRain()))
                : 1.0;
        return clamp(100.0 * roadFactor * rainDecay, 0.0, 100.0);
    }

    static double scoreWind(
            ScoringActivityProfile profile,
            double vHead,
            double vCrossEff,
            double vGust
    ) {
        double headTerm = Math.pow(vHead / profile.vCritHead(), profile.mHead());
        double crossTerm = Math.pow(vCrossEff / profile.vCritCross(), profile.mCross());
        double gustTerm = Math.pow(
                Math.max(0.0, vGust - profile.vSafeGust()) / profile.vCritGust(),
                2.0
        );
        double omega = headTerm + crossTerm + gustTerm;
        return clamp(100.0 * Math.exp(-LN_2 * omega), 0.0, 100.0);
    }

    static double calculateTailwindBoost(
            ScoringActivityProfile profile,
            double vTail,
            double vCrossEff,
            double vGust
    ) {
        if (profile != ScoringActivityProfile.BICYCLE || vTail <= 0.0) {
            return 0.0;
        }
        double crossRatio = vCrossEff / 15.0;
        return 5.0
                * Math.tanh(vTail / 18.0)
                * Math.exp(-(crossRatio * crossRatio))
                * sigmoid(38.0 - vGust);
    }

    static double scoreVisibility(
            ScoringActivityProfile profile,
            double visibilityMeters,
            double t2m,
            double td,
            double uvIndex
    ) {
        double vSafe = Math.max(10.0, visibilityMeters);
        double fogRatio = profile.vCritVisibility() / vSafe;
        double phiFog = 1.0 / (1.0 + fogRatio * fogRatio * fogRatio);

        double dewSpread = t2m - td;
        double phiVisor = 1.0
                - profile.etaVisor()
                * Math.exp(-(dewSpread * dewSpread) / 2.5)
                * sigmoid(15.0 - t2m);

        double uvExcess = Math.max(0.0, uvIndex - 5.0);
        double phiUv = Math.exp(-profile.cUv() * uvExcess * uvExcess);

        return clamp(100.0 * phiFog * phiVisor * phiUv, 0.0, 100.0);
    }

    double calculateThunderstormCap(
            ScoringActivityProfile profile,
            int wmo,
            double cape,
            double pPrecipNorm,
            Double thunderstormProbabilityPercent
    ) {
        if (wmo == 96 || wmo == 99) {
            return 5.0;
        }
        if (wmo == 95) {
            return profile == ScoringActivityProfile.HIKING ? 10.0 : 15.0;
        }
        boolean latentThunderRisk = (cape >= 600.0 && pPrecipNorm > 0.30)
                || wmo == 82
                || (thunderstormProbabilityPercent != null
                && thunderstormProbabilityPercent >= properties.thunderstormRiskThresholdPercent());
        if (latentThunderRisk) {
            double capePenalty = 25.0 * Math.min(1.0, Math.max(0.0, cape - 600.0) / 1200.0);
            return Math.min(properties.thunderstormMaxOverallScore(), 50.0 - capePenalty);
        }
        return 100.0;
    }

    static double calculateIceAndSnowCap(
            ScoringActivityProfile profile,
            int wmo,
            double wRoad,
            double t2m,
            double tSoil,
            double pSnow,
            double vGust
    ) {
        boolean freezingRain = wmo == 56 || wmo == 57 || wmo == 66 || wmo == 67;
        boolean blackIceRisk = wRoad > 0.20 && Math.min(t2m, tSoil) <= 0.5;
        if (freezingRain || blackIceRisk) {
            return switch (profile) {
                case MOTORCYCLE -> 0.0;
                case BICYCLE -> 5.0;
                case HIKING -> 20.0;
            };
        }

        boolean snowCode = wmo == 71 || wmo == 73 || wmo == 75 || wmo == 77 || wmo == 85 || wmo == 86;
        if (snowCode || pSnow > 0.0) {
            return switch (profile) {
                case MOTORCYCLE -> 5.0;
                case BICYCLE -> wmo == 71 ? 15.0 : 5.0;
                case HIKING -> {
                    if (wmo == 71 && vGust < 35.0) {
                        yield 65.0;
                    }
                    if (wmo == 75 || wmo == 86) {
                        yield 15.0;
                    }
                    yield 35.0;
                }
            };
        }

        return 100.0;
    }

    static double calculateStormCap(ScoringActivityProfile profile, double vGust) {
        double excess = Math.max(0.0, vGust - profile.vLimitStorm()) / 15.0;
        return 100.0 * Math.exp(-(excess * excess));
    }

    public static double calculateDryingTimeConstantHours(
            double t2m,
            double shortwaveRadiationWm2,
            double relativeHumidityPercent
    ) {
        double tempRadiationFactor = 1.0
                + 0.06 * Math.max(0.0, t2m - 10.0)
                + 0.0025 * Math.max(0.0, shortwaveRadiationWm2);
        double humidityFactor = Math.max(0.15, 1.1 - 0.008 * clamp(relativeHumidityPercent, 0.0, 100.0));
        return 1.6 / (tempRadiationFactor * humidityFactor);
    }

    static double estimateFallbackWetRoadIndex(
            double iPrecipMmPerHour,
            double t2m,
            double rSw,
            double rh
    ) {
        if (iPrecipMmPerHour <= 0.0) {
            return 0.0;
        }
        double tauDry = calculateDryingTimeConstantHours(t2m, rSw, rh);
        double pAcc = 0.0;
        // 1 hour of steady rain at rate iPrecipMmPerHour corresponds to 4 quarter-hour steps
        for (int k = 0; k < 4; k++) {
            pAcc += (iPrecipMmPerHour / 4.0) * Math.exp(-(0.25 * k) / tauDry);
        }
        return clamp(1.0 - Math.exp(-pAcc / 1.2), 0.0, 1.0);
    }

    private static double[] resolveNormalizedTimeWeights(
            List<WeatherSegment> segments,
            List<Double> segmentDurationsSeconds
    ) {
        int n = segments.size();
        double[] weights = new double[n];
        if (segmentDurationsSeconds != null && segmentDurationsSeconds.size() == n) {
            double totalDuration = 0.0;
            for (int i = 0; i < n; i++) {
                double d = segmentDurationsSeconds.get(i) != null
                        ? Math.max(0.0, segmentDurationsSeconds.get(i))
                        : 0.0;
                weights[i] = d;
                totalDuration += d;
            }
            if (totalDuration > 0.0) {
                for (int i = 0; i < n; i++) {
                    weights[i] /= totalDuration;
                }
                return weights;
            }
        }

        double uniform = 1.0 / n;
        for (int i = 0; i < n; i++) {
            weights[i] = uniform;
        }
        return weights;
    }

    static double angularDifferenceDegrees(double angleA, double angleB) {
        double rawDiff = Math.abs(angleA - angleB) % 360.0;
        return 180.0 - Math.abs(rawDiff - 180.0);
    }

    static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum ScoringActivityProfile {
        MOTORCYCLE(
                60.0,
                1.25,
                14.0, 25.0, 8.0, 6.5,
                0.35, 0.58, 0.75,
                75.0, 2.0, 35.0, 2.5, 30.0, 25.0,
                600.0, 0.35, 0.002,
                0.40, 0.25, 0.20, 0.15,
                65.0, 12.0
        ),
        BICYCLE(
                20.0,
                0.95,
                15.0, 23.0, 8.5, 7.5,
                0.20, 0.50, 0.80,
                24.0, 2.2, 28.0, 2.2, 25.0, 25.0,
                250.0, 0.15, 0.012,
                0.35, 0.35, 0.20, 0.10,
                55.0, 14.0
        ),
        HIKING(
                5.0,
                0.40,
                10.0, 22.0, 11.0, 7.0,
                0.08, 0.42, 0.85,
                45.0, 1.8, 55.0, 2.0, 35.0, 30.0,
                150.0, 0.0, 0.016,
                0.35, 0.15, 0.30, 0.20,
                75.0, 15.0
        );

        private final double defaultSpeedKmh;
        private final double gustFactorGamma;
        private final double tMin;
        private final double tMax;
        private final double sigmaCold;
        private final double sigmaHot;
        private final double alphaRoad;
        private final double lambdaRain;
        private final double betaRain;
        private final double vCritHead;
        private final double mHead;
        private final double vCritCross;
        private final double mCross;
        private final double vSafeGust;
        private final double vCritGust;
        private final double vCritVisibility;
        private final double etaVisor;
        private final double cUv;
        private final double wPrecip;
        private final double wWind;
        private final double wTemp;
        private final double wVis;
        private final double vLimitStorm;
        private final double tauRoute;

        ScoringActivityProfile(
                double defaultSpeedKmh,
                double gustFactorGamma,
                double tMin,
                double tMax,
                double sigmaCold,
                double sigmaHot,
                double alphaRoad,
                double lambdaRain,
                double betaRain,
                double vCritHead,
                double mHead,
                double vCritCross,
                double mCross,
                double vSafeGust,
                double vCritGust,
                double vCritVisibility,
                double etaVisor,
                double cUv,
                double wPrecip,
                double wWind,
                double wTemp,
                double wVis,
                double vLimitStorm,
                double tauRoute
        ) {
            this.defaultSpeedKmh = defaultSpeedKmh;
            this.gustFactorGamma = gustFactorGamma;
            this.tMin = tMin;
            this.tMax = tMax;
            this.sigmaCold = sigmaCold;
            this.sigmaHot = sigmaHot;
            this.alphaRoad = alphaRoad;
            this.lambdaRain = lambdaRain;
            this.betaRain = betaRain;
            this.vCritHead = vCritHead;
            this.mHead = mHead;
            this.vCritCross = vCritCross;
            this.mCross = mCross;
            this.vSafeGust = vSafeGust;
            this.vCritGust = vCritGust;
            this.vCritVisibility = vCritVisibility;
            this.etaVisor = etaVisor;
            this.cUv = cUv;
            this.wPrecip = wPrecip;
            this.wWind = wWind;
            this.wTemp = wTemp;
            this.wVis = wVis;
            this.vLimitStorm = vLimitStorm;
            this.tauRoute = tauRoute;
        }

        public static ScoringActivityProfile fromCode(String code) {
            if (code == null || code.isBlank()) {
                return BICYCLE;
            }
            return switch (code.trim().toUpperCase(Locale.ROOT)) {
                case "MOTORCYCLE", "MOTO" -> MOTORCYCLE;
                case "HIKING", "HIKE", "WALKING", "FOOT" -> HIKING;
                default -> BICYCLE;
            };
        }

        public double defaultSpeedKmh() {
            return defaultSpeedKmh;
        }

        public double gustFactorGamma() {
            return gustFactorGamma;
        }

        public double tMin() {
            return tMin;
        }

        public double tMax() {
            return tMax;
        }

        public double sigmaCold() {
            return sigmaCold;
        }

        public double sigmaHot() {
            return sigmaHot;
        }

        public double alphaRoad() {
            return alphaRoad;
        }

        public double lambdaRain() {
            return lambdaRain;
        }

        public double betaRain() {
            return betaRain;
        }

        public double vCritHead() {
            return vCritHead;
        }

        public double mHead() {
            return mHead;
        }

        public double vCritCross() {
            return vCritCross;
        }

        public double mCross() {
            return mCross;
        }

        public double vSafeGust() {
            return vSafeGust;
        }

        public double vCritGust() {
            return vCritGust;
        }

        public double vCritVisibility() {
            return vCritVisibility;
        }

        public double etaVisor() {
            return etaVisor;
        }

        public double cUv() {
            return cUv;
        }

        public double wPrecip() {
            return wPrecip;
        }

        public double wWind() {
            return wWind;
        }

        public double wTemp() {
            return wTemp;
        }

        public double wVis() {
            return wVis;
        }

        public double vLimitStorm() {
            return vLimitStorm;
        }

        public double tauRoute() {
            return tauRoute;
        }
    }

    public record SegmentScoreBreakdown(
            int score,
            double rawScore,
            double tempSubScore,
            double precipSubScore,
            double windSubScore,
            double visSubScore,
            double tailwindBoost,
            double thunderstormCap,
            double iceCap,
            double stormCap,
            double relativeWindAngleDegrees,
            double effectiveTemperatureCelsius,
            double wetRoadIndex,
            String limitingFactor,
            String reasonLabel
    ) {
    }

    public record OverallScoreResult(
            int overallScore,
            boolean thunderstormCapApplied,
            String scoreVersion
    ) {
    }
}
