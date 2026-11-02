package de.warp.weather.api.dto;

public record HiddenParameterInsightDto(
        String parameterKey,
        String parameterLabel,
        String category,
        String observedValue,
        String status,
        String interpretation
) {
}
