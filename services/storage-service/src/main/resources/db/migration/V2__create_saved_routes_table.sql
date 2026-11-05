-- [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
CREATE TABLE warp_storage.saved_routes
(
    id                       VARCHAR(40)              NOT NULL,
    user_id                  VARCHAR(40)              NOT NULL,
    name                     VARCHAR(100)             NOT NULL,
    favorite                 BOOLEAN                  NOT NULL DEFAULT FALSE,

    start_label              VARCHAR(255)             NOT NULL,
    start_latitude           DOUBLE PRECISION         NOT NULL,
    start_longitude          DOUBLE PRECISION         NOT NULL,
    destination_label        VARCHAR(255)             NOT NULL,
    destination_latitude     DOUBLE PRECISION         NOT NULL,
    destination_longitude    DOUBLE PRECISION         NOT NULL,

    activity_profile_code    VARCHAR(20)              NOT NULL,
    departure_time           TIMESTAMP WITH TIME ZONE NOT NULL,
    arrival_time             TIMESTAMP WITH TIME ZONE NOT NULL,
    distance_meters          INTEGER                  NOT NULL,
    duration_seconds         INTEGER                  NOT NULL,
    geometry_json            TEXT                     NOT NULL,

    route_calculation_id     VARCHAR(40),
    weather_evaluation_id    VARCHAR(40),
    weather_overall_score    INTEGER                  NOT NULL,
    weather_score_version    VARCHAR(20)              NOT NULL,
    thunderstorm_cap_applied BOOLEAN                  NOT NULL DEFAULT FALSE,

    idempotency_key          VARCHAR(100),
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_saved_routes PRIMARY KEY (id),

    CONSTRAINT chk_saved_routes_name_not_blank
        CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_saved_routes_profile
        CHECK (activity_profile_code IN ('BICYCLE', 'MOTORCYCLE', 'HIKING')),
    CONSTRAINT chk_saved_routes_times
        CHECK (arrival_time >= departure_time),
    CONSTRAINT chk_saved_routes_distance
        CHECK (distance_meters >= 0),
    CONSTRAINT chk_saved_routes_duration
        CHECK (duration_seconds >= 0),
    CONSTRAINT chk_saved_routes_score
        CHECK (weather_overall_score BETWEEN 0 AND 100),
    CONSTRAINT chk_saved_routes_start_coordinates
        CHECK (start_latitude BETWEEN -90 AND 90
            AND start_longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_saved_routes_destination_coordinates
        CHECK (destination_latitude BETWEEN -90 AND 90
            AND destination_longitude BETWEEN -180 AND 180)
);

-- Alle Listen- und Detailzugriffe laufen über user_id.
CREATE INDEX idx_saved_routes_user_created
    ON warp_storage.saved_routes (user_id, created_at DESC);

CREATE INDEX idx_saved_routes_user_favorite
    ON warp_storage.saved_routes (user_id, favorite);

-- Idempotenz: gleicher Key pro Benutzer nur einmal (NULL-Werte sind erlaubt).
CREATE UNIQUE INDEX uk_saved_routes_user_idempotency
    ON warp_storage.saved_routes (user_id, idempotency_key);