package de.warp.routing.infrastructure.routing;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenRouteServiceResponse(
        String type,
        List<Feature> features
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(
            String type,
            Geometry geometry,
            Properties properties
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometry(
            String type,
            List<List<Double>> coordinates
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(
            Summary summary,
            List<Segment> segments
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            double distance,
            double duration
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(
            double distance,
            double duration
    ) {
    }
}