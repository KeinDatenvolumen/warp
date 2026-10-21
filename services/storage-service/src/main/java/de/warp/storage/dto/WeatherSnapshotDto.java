package de.warp.storage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

        @NotEmpty(message = "Es werden Abschnittsscores benötigt.")
        @Size(max = 2000)
        List<@Valid WeatherSegmentDto> segments
) {
}