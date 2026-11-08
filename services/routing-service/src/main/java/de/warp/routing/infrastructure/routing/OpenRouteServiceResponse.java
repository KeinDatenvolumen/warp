// [KI-Log Nr. KI-154] (2dcf6bf) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement OpenRouteService client
// [KI-Log Nr. KI-170] (3b5d8a2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement OpenRouteService client
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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