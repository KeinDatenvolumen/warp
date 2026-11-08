// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.weather.application.scoring;

import de.warp.weather.api.dto.DepartureSlotResultDto;
import de.warp.weather.application.WeatherScoringService.ScoringActivityProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OptimizationScoringRulesTest {

    private OptimizationScoringRules scoringRules;

    @BeforeEach
    void setUp() {
        scoringRules = new OptimizationScoringRules();
    }

    @Test
    void resolvesTauPerActivityProfile() {
        assertThat(scoringRules.resolveProfileTau(ScoringActivityProfile.MOTORCYCLE)).isEqualTo(12.0);
        assertThat(scoringRules.resolveProfileTau(ScoringActivityProfile.BICYCLE)).isEqualTo(14.0);
        assertThat(scoringRules.resolveProfileTau(ScoringActivityProfile.HIKING)).isEqualTo(15.0);
    }

    @Test
    void calculatesSoftMinimumAndFinalScore() {
        List<Integer> scores = List.of(80, 85, 90);
        double[] weights = new double[]{0.33, 0.33, 0.34};

        double softMin = scoringRules.calculatePessimisticSoftMinimum(scores, weights, 80, 14.0);
        assertThat(softMin).isLessThanOrEqualTo(85.0);

        int normalScore = scoringRules.calculateSlotRouteScore(85.0, softMin, 80, false);
        assertThat(normalScore).isBetween(70, 95);

        int thunderScore = scoringRules.calculateSlotRouteScore(85.0, softMin, 80, true);
        assertThat(thunderScore).isLessThanOrEqualTo(50);
    }

    @Test
    void appliesGaussianSmoothingAndStableWindow() {
        DepartureSlotResultDto slot0 = new DepartureSlotResultDto("2026-05-16T08:00:00Z", "08:00", "2026-05-16T09:00:00Z", "09:00", 90, 0.0, false, false, List.of(), List.of(), "");
        DepartureSlotResultDto slot1 = new DepartureSlotResultDto("2026-05-16T08:15:00Z", "08:15", "2026-05-16T09:15:00Z", "09:15", 92, 0.0, false, false, List.of(), List.of(), "");
        DepartureSlotResultDto slot2 = new DepartureSlotResultDto("2026-05-16T08:30:00Z", "08:30", "2026-05-16T09:30:00Z", "09:30", 50, 0.0, true, false, List.of(), List.of(), "");

        OptimizationScoringRules.SmoothedSlotsResult smoothed =
                scoringRules.applyGaussianSmoothingAndOptimaDetection(List.of(slot0, slot1, slot2));

        assertThat(smoothed.bestSlotIndex()).isIn(0, 1);
        assertThat(smoothed.worstSlotIndex()).isEqualTo(2);

        String stableWindow = scoringRules.calculateStableWindow(smoothed.slots(), smoothed.bestSlotIndex());
        assertThat(stableWindow).isNotEmpty();

        String thunderAdvantage = scoringRules.buildAdvantageExplanation(slot0, slot2);
        assertThat(thunderAdvantage).contains("Gewitterfront");

        String deltaText = scoringRules.buildSlotDeltaExplanation(90, slot2);
        assertThat(deltaText).contains("+40 Pkt.");

        String worstDeltaText = scoringRules.buildSlotDeltaExplanation(50, slot2);
        assertThat(worstDeltaText).isEqualTo("Tiefstwert im Zeitfenster");
    }
}
