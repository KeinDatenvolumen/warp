// [KI-Log Nr. KI-101] (c1e1a5c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-133] (943d006) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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