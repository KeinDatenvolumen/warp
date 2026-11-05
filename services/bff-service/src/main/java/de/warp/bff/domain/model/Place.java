// [KI-Log Nr. KI-08] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Geocoding-Client & Autocomplete (WARP-010)
package de.warp.bff.domain.model;

public record Place(
        String placeId,
        String displayName,
        double latitude,
        double longitude
) {
}