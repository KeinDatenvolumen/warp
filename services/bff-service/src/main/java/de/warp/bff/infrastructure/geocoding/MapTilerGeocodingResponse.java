package de.warp.bff.infrastructure.geocoding;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MapTilerGeocodingResponse(
        List<Feature> features
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(
            String id,

            @JsonProperty("place_name")
            String placeName,

            List<Double> center
    ) {
    }
}