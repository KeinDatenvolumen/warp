// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record SegmentWeatherDto(
        @DecimalMin("0.0") Double windSpeedKmh,
        @DecimalMin("0.0") @DecimalMax("360.0") Double windDirectionDegrees,
        @DecimalMin("0.0") @DecimalMax("100.0")
        Double precipitationProbabilityPercent,
        @DecimalMin("0.0") Double precipitationMm,
        @DecimalMin("0.0") @DecimalMax("100.0")
        Double thunderstormProbabilityPercent
) {
}