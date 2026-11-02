package de.warp.weather.api.dto;

import java.util.List;

public record DepartureSlotResultDto(
        String departureIso,
        String departureTime,
        String arrivalIso,
        String arrivalTime,
        int routeScore,
        double robustScore,
        boolean hasThunderCap,
        boolean reachesNightfall,
        List<Integer> segmentScores,
        List<String> segmentReasons,
        String summaryDeltaText
) {
}
