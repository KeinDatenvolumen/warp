package de.warp.bff.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import de.warp.bff.application.service.GeocodingService;
import de.warp.bff.domain.model.Place;

@Controller
public class PlaceGraphQlController {

    private final GeocodingService geocodingService;

    public PlaceGraphQlController(
            GeocodingService geocodingService
    ) {
        this.geocodingService = geocodingService;
    }

    @QueryMapping
    public List<Place> searchPlaces(
            @Argument String query,
            @Argument Integer limit
    ) {
        return geocodingService.searchPlaces(query, limit);
    }
}