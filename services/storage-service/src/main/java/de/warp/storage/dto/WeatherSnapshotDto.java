// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WeatherSnapshotDto(
        @Size(max = 40) String weatherEvaluationId,

        @NotNull(message = "Der Gesamtwetterscore muss angegeben werden.")
        @Min(0) @Max(100)
        Integer overallScore,

        @NotBlank @Size(max = 20) String scoreVersion,

        boolean thunderstormCapApplied,

        @NotNull(message = "Die Abschnittsscores müssen angegeben werden.")
        @Size(max = 2000)
        List<@Valid WeatherSegmentDto> segments
) {
}