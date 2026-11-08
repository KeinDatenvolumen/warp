// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.scoring;

import de.warp.weather.api.dto.DepartureSlotResultDto;
import de.warp.weather.application.WeatherScoringService.ScoringActivityProfile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class OptimizationScoringRules {

    public double resolveProfileTau(ScoringActivityProfile profile) {
        if (profile == ScoringActivityProfile.MOTORCYCLE) {
            return 12.0;
        } else if (profile == ScoringActivityProfile.BICYCLE) {
            return 14.0;
        } else {
            return 15.0;
        }
    }

    public double calculatePessimisticSoftMinimum(
            List<Integer> segScores,
            double[] segmentWeights,
            int minSegScore,
            double tau
    ) {
        double expSum = 0.0;
        for (int i = 0; i < segScores.size(); i++) {
            int s = segScores.get(i);
            expSum += segmentWeights[i] * Math.exp(-(s - minSegScore) / tau);
        }
        return minSegScore - tau * Math.log(Math.max(1e-15, expSum));
    }

    public int calculateSlotRouteScore(
            double linearMean,
            double softMin,
            int minSegScore,
            boolean hasThunderCap
    ) {
        double bottleneckCap = minSegScore + 0.45 * Math.sqrt(minSegScore * (100.0 - minSegScore));
        double thunderCap = hasThunderCap ? 50.0 : 100.0;
        double routeRaw = Math.min(bottleneckCap, Math.min(thunderCap, 0.35 * linearMean + 0.65 * softMin));
        return (int) Math.round(Math.clamp(routeRaw, 0.0, 100.0));
    }

    public SmoothedSlotsResult applyGaussianSmoothingAndOptimaDetection(List<DepartureSlotResultDto> rawSlots) {
        int bestIdx = 0;
        double maxRobust = -1.0;
        int worstIdx = 0;
        int minScore = 101;

        List<DepartureSlotResultDto> smoothed = new ArrayList<>(rawSlots.size());

        for (int m = 0; m < rawSlots.size(); m++) {
            int prev = rawSlots.get(Math.max(0, m - 1)).routeScore();
            int curr = rawSlots.get(m).routeScore();
            int next = rawSlots.get(Math.min(rawSlots.size() - 1, m + 1)).routeScore();

            double robust = Math.round((0.20 * prev + 0.60 * curr + 0.20 * next) * 10.0) / 10.0;
            DepartureSlotResultDto existing = rawSlots.get(m);
            smoothed.add(new DepartureSlotResultDto(
                    existing.departureIso(),
                    existing.departureTime(),
                    existing.arrivalIso(),
                    existing.arrivalTime(),
                    existing.routeScore(),
                    robust,
                    existing.hasThunderCap(),
                    existing.reachesNightfall(),
                    existing.segmentScores(),
                    existing.segmentReasons(),
                    ""
            ));

            if (robust > maxRobust) {
                maxRobust = robust;
                bestIdx = m;
            }

            if (curr < minScore) {
                minScore = curr;
                worstIdx = m;
            }
        }

        return new SmoothedSlotsResult(smoothed, bestIdx, worstIdx);
    }

    public String calculateStableWindow(List<DepartureSlotResultDto> slots, int bestIdx) {
        DepartureSlotResultDto bestSlot = slots.get(bestIdx);
        int stableStartIdx = bestIdx;
        int stableEndIdx = bestIdx;
        int scoreThreshold = Math.max(50, bestSlot.routeScore() - 6);

        while (stableStartIdx > 0 && slots.get(stableStartIdx - 1).routeScore() >= scoreThreshold) {
            stableStartIdx--;
        }
        while (stableEndIdx < slots.size() - 1 && slots.get(stableEndIdx + 1).routeScore() >= scoreThreshold) {
            stableEndIdx++;
        }

        return String.format(
                Locale.ROOT,
                "%s – %s",
                slots.get(stableStartIdx).departureTime(),
                slots.get(stableEndIdx).departureTime()
        );
    }

    public String buildAdvantageExplanation(DepartureSlotResultDto bestSlot, DepartureSlotResultDto worstSlot) {
        int deltaPoints = bestSlot.routeScore() - worstSlot.routeScore();
        if (worstSlot.hasThunderCap() && !bestSlot.hasThunderCap()) {
            return String.format(
                    Locale.GERMAN,
                    "+%d Punkte ggü. %s Uhr (umgeht Gewitterfront)",
                    deltaPoints,
                    worstSlot.departureTime()
            );
        } else if (worstSlot.reachesNightfall() && !bestSlot.reachesNightfall()) {
            return String.format(
                    Locale.GERMAN,
                    "+%d Punkte ggü. %s Uhr (nutzt volles Tageslicht)",
                    deltaPoints,
                    worstSlot.departureTime()
            );
        } else {
            return String.format(
                    Locale.GERMAN,
                    "+%d Punkte ggü. %s Uhr (optimale Wetterkonditionen)",
                    deltaPoints,
                    worstSlot.departureTime()
            );
        }
    }

    public String buildSlotDeltaExplanation(int slotScore, DepartureSlotResultDto worstSlot) {
        int diffToWorst = slotScore - worstSlot.routeScore();
        return diffToWorst > 0
                ? String.format(Locale.GERMAN, "+%d Pkt. ggü. %s Uhr", diffToWorst, worstSlot.departureTime())
                : "Tiefstwert im Zeitfenster";
    }

    public record SmoothedSlotsResult(
            List<DepartureSlotResultDto> slots,
            int bestSlotIndex,
            int worstSlotIndex
    ) {
    }
}
