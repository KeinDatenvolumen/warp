-- [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
CREATE TABLE warp_weather.weather_evaluation (
    evaluation_id VARCHAR(40) NOT NULL,
    route_calculation_id VARCHAR(40) NOT NULL,
    requested_by_user_id VARCHAR(40),
    status VARCHAR(20) NOT NULL,
    departure_time TIMESTAMP WITH TIME ZONE NOT NULL,
    overall_score INTEGER NOT NULL,
    score_version VARCHAR(20) NOT NULL,
    thunderstorm_cap_applied BOOLEAN NOT NULL DEFAULT FALSE,
    weather_data_generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    weather_data_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_weather_evaluation
        PRIMARY KEY (evaluation_id),

    CONSTRAINT chk_weather_evaluation_overall_score
        CHECK (overall_score BETWEEN 0 AND 100),

    CONSTRAINT chk_weather_evaluation_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_weather_evaluation_route_calculation_id
    ON warp_weather.weather_evaluation (route_calculation_id);

CREATE TABLE warp_weather.weather_segment (
    segment_id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    evaluation_id VARCHAR(40) NOT NULL,
    segment_index INTEGER NOT NULL,
    distance_from_start_meters INTEGER NOT NULL DEFAULT 0,
    expected_arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
    score INTEGER NOT NULL,
    color_hex VARCHAR(7) NOT NULL,
    wind_speed_kmh DOUBLE PRECISION,
    wind_direction_degrees DOUBLE PRECISION,
    wind_gusts_kmh DOUBLE PRECISION,
    precipitation_probability DOUBLE PRECISION,
    precipitation_mm DOUBLE PRECISION,
    wet_road_index DOUBLE PRECISION,
    snowfall_cm DOUBLE PRECISION,
    thunderstorm_probability DOUBLE PRECISION,
    temperature_celsius DOUBLE PRECISION,
    apparent_temperature_celsius DOUBLE PRECISION,
    relative_humidity_percent DOUBLE PRECISION,
    dew_point_celsius DOUBLE PRECISION,
    shortwave_radiation_wm2 DOUBLE PRECISION,
    soil_temperature_celsius DOUBLE PRECISION,
    visibility_meters DOUBLE PRECISION,
    cape_jkg DOUBLE PRECISION,
    uv_index DOUBLE PRECISION,
    weather_code INTEGER,
    limiting_factor VARCHAR(32),
    reason_label VARCHAR(120),
    geometry_geojson TEXT,

    CONSTRAINT pk_weather_segment
        PRIMARY KEY (segment_id),

    CONSTRAINT fk_weather_segment_evaluation
        FOREIGN KEY (evaluation_id)
        REFERENCES warp_weather.weather_evaluation (evaluation_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_weather_segment_index
        CHECK (segment_index >= 0),

    CONSTRAINT chk_weather_segment_distance
        CHECK (distance_from_start_meters >= 0),

    CONSTRAINT chk_weather_segment_score
        CHECK (score BETWEEN 0 AND 100)
);

CREATE INDEX idx_weather_segment_evaluation_id
    ON warp_weather.weather_segment (evaluation_id, segment_index);

CREATE TABLE warp_weather.weather_cache (
    cache_key VARCHAR(120) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    forecast_time TIMESTAMP WITH TIME ZONE NOT NULL,
    weather_model VARCHAR(32) NOT NULL,
    temperature_celsius DOUBLE PRECISION,
    apparent_temperature_celsius DOUBLE PRECISION,
    relative_humidity_percent DOUBLE PRECISION,
    dew_point_celsius DOUBLE PRECISION,
    shortwave_radiation_wm2 DOUBLE PRECISION,
    soil_temperature_celsius DOUBLE PRECISION,
    wind_speed_kmh DOUBLE PRECISION,
    wind_direction_degrees DOUBLE PRECISION,
    wind_gusts_kmh DOUBLE PRECISION,
    precipitation_probability DOUBLE PRECISION,
    precipitation_mm DOUBLE PRECISION,
    wet_road_index DOUBLE PRECISION,
    snowfall_cm DOUBLE PRECISION,
    thunderstorm_probability DOUBLE PRECISION,
    visibility_meters DOUBLE PRECISION,
    cape_jkg DOUBLE PRECISION,
    uv_index DOUBLE PRECISION,
    weather_code INTEGER,
    cached_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_weather_cache
        PRIMARY KEY (cache_key),

    CONSTRAINT chk_weather_cache_expiry
        CHECK (expires_at > cached_at)
);

CREATE INDEX idx_weather_cache_expires_at
    ON warp_weather.weather_cache (expires_at);

CREATE TABLE warp_weather.departure_search (
    search_id VARCHAR(40) NOT NULL,
    route_calculation_id VARCHAR(40) NOT NULL,
    requested_by_user_id VARCHAR(40),
    time_from TIMESTAMP WITH TIME ZONE NOT NULL,
    time_to TIMESTAMP WITH TIME ZONE NOT NULL,
    interval_minutes INTEGER NOT NULL,
    best_departure_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_departure_search
        PRIMARY KEY (search_id)
);

CREATE TABLE warp_weather.departure_alternative (
    alternative_id BIGINT GENERATED ALWAYS AS IDENTITY NOT NULL,
    search_id VARCHAR(40) NOT NULL,
    departure_time TIMESTAMP WITH TIME ZONE NOT NULL,
    arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
    weather_score INTEGER NOT NULL,
    is_best_departure BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_departure_alternative
        PRIMARY KEY (alternative_id),

    CONSTRAINT fk_departure_alternative_search
        FOREIGN KEY (search_id)
        REFERENCES warp_weather.departure_search (search_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_departure_alternative_score
        CHECK (weather_score BETWEEN 0 AND 100)
);
