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
    public List<Place> searchPlaces(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") Integer limit
    ) {
        return geocodingService.searchPlaces(query, limit);
    }
}