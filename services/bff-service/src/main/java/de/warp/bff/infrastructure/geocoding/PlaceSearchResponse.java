package de.warp.bff.infrastructure.geocoding;

import java.util.List;

import de.warp.bff.domain.model.Place;

public record PlaceSearchResponse(
        List < Place > items
) {
}