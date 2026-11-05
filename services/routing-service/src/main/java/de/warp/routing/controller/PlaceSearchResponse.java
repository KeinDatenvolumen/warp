// [KI-Log Nr. KI-08] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Geocoding-Client & Autocomplete (WARP-010)
package de.warp.routing.controller;

import java.util.List;

import de.warp.routing.domain.model.Place;

public record PlaceSearchResponse(
        List < Place > items
) {
}