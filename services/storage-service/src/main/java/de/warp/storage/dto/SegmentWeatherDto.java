// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
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