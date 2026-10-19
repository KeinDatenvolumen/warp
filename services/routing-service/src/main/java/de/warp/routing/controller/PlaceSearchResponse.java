package de.warp.routing.controller;

import java.util.List;

import de.warp.routing.domain.model.Place;

public record PlaceSearchResponse(
        List < Place > items
) {
}