// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.storage;

import de.warp.storage.dto.CreateSavedRouteRequest;
import de.warp.storage.dto.GeoJsonLineString;
import de.warp.storage.dto.PlaceDto;
import de.warp.storage.dto.RouteSnapshotDto;
import de.warp.storage.dto.SegmentWeatherDto;
import de.warp.storage.dto.WeatherSegmentDto;
import de.warp.storage.dto.WeatherSnapshotDto;

import java.time.OffsetDateTime;
import java.util.List;

public final class SavedRouteTestData {

    public static final OffsetDateTime DEPARTURE =
            OffsetDateTime.parse("2026-10-24T08:00:00+02:00");

    public static final OffsetDateTime ARRIVAL =
            OffsetDateTime.parse("2026-10-24T11:12:00+02:00");

    private SavedRouteTestData() {
    }

    public static CreateSavedRouteRequest sampleRequest() {
        return sampleRequest(DEPARTURE, ARRIVAL);
    }

    public static CreateSavedRouteRequest sampleRequest(
            OffsetDateTime departure,
            OffsetDateTime arrival
    ) {
        GeoJsonLineString geometry = new GeoJsonLineString(
                "LineString",
                List.of(
                        List.of(9.1511, 49.3536),
                        List.of(9.4000, 49.4000),
                        List.of(9.7736, 49.4924)
                )
        );

        RouteSnapshotDto route = new RouteSnapshotDto(
                "calc_123",
                new PlaceDto("Mosbach", 49.3536, 9.1511),
                new PlaceDto("Bad Mergentheim", 49.4924, 9.7736),
                "BICYCLE",
                departure,
                arrival,
                57600,
                11520,
                geometry
        );

        WeatherSnapshotDto weather = new WeatherSnapshotDto(
                "weather_123",
                78,
                "1.0",
                false,
                List.of(
                        new WeatherSegmentDto(
                                1,
                                departure.plusHours(1),
                                70,
                                "#AAAA40",
                                null,
                                null
                        ),
                        new WeatherSegmentDto(
                                0,
                                departure.plusMinutes(30),
                                86,
                                "#55AA40",
                                new SegmentWeatherDto(
                                        15.0, 230.0, 20.0, 0.2, 5.0
                                ),
                                new GeoJsonLineString(
                                        "LineString",
                                        List.of(
                                                List.of(9.1511, 49.3536),
                                                List.of(9.4000, 49.4000)
                                        )
                                )
                        )
                )
        );

        return new CreateSavedRouteRequest(
                "Fahrradtour nach Bad Mergentheim",
                false,
                route,
                weather
        );
    }
}