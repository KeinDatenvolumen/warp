// [KI-Log Nr. KI-08] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Geocoding-Client & Autocomplete (WARP-010)
package de.warp.bff.infrastructure.geocoding;

import java.util.List;

import de.warp.bff.domain.model.Place;

public record PlaceSearchResponse(
        List < Place > items
) {
}