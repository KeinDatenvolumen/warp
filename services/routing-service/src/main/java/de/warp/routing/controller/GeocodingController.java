// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-123] (00e2e64) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): test geocoding service integration (WARP-010)
// [KI-Log Nr. KI-136] (b99ef77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-137] (15d143f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): test geocoding service integration (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.warp.routing.application.service.GeocodingService;
import de.warp.routing.domain.model.Place;

@RestController
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(
            GeocodingService geocodingService
    ) {
        this.geocodingService = geocodingService;
    }

    @GetMapping("/internal/v1/places")
    public PlaceSearchResponse searchPlaces(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") Integer limit
    ) {
        return new PlaceSearchResponse(
                geocodingService.searchPlaces(query, limit)
        );
    }
}