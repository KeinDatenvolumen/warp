// [KI-Log Nr. KI-08] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Geocoding-Client & Autocomplete (WARP-010)
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