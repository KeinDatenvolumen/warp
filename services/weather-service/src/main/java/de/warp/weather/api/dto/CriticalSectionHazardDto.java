package de.warp.weather.api.dto;

public record CriticalSectionHazardDto(
        int segmentIndexStart,
        int segmentIndexEnd,
        String distanceRangeKm,
        String timeWindow,
        int minSegmentScore,
        String severity,
        String primaryFactor,
        String title,
        String description,
        String recommendation
) {
}
