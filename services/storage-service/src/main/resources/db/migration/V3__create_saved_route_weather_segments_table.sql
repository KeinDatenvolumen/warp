CREATE TABLE warp_storage.saved_route_weather_segments
(
    id                                BIGINT GENERATED ALWAYS AS IDENTITY,
    saved_route_id                    VARCHAR(40)              NOT NULL,
    segment_index                     INTEGER                  NOT NULL,
    expected_arrival_time             TIMESTAMP WITH TIME ZONE NOT NULL,
    score                             INTEGER                  NOT NULL,
    color                             VARCHAR(7)               NOT NULL,

    wind_speed_kmh                    DOUBLE PRECISION,
    wind_direction_degrees            DOUBLE PRECISION,
    precipitation_probability_percent DOUBLE PRECISION,
    precipitation_mm                  DOUBLE PRECISION,
    thunderstorm_probability_percent  DOUBLE PRECISION,

    geometry_json                     TEXT,

    CONSTRAINT pk_saved_route_weather_segments PRIMARY KEY (id),

    CONSTRAINT fk_saved_route_weather_segments_route
        FOREIGN KEY (saved_route_id)
            REFERENCES warp_storage.saved_routes (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_saved_route_weather_segments_index
        UNIQUE (saved_route_id, segment_index),

    CONSTRAINT chk_saved_route_weather_segments_index
        CHECK (segment_index >= 0),
    CONSTRAINT chk_saved_route_weather_segments_score
        CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT chk_saved_route_weather_segments_color
        CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
    );