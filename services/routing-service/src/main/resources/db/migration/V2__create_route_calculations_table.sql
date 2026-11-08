CREATE TABLE warp_routing.route_calculations
(
    id                       VARCHAR(40)              NOT NULL,
    requested_by_user_id     VARCHAR(40)              NOT NULL,

    start_label              VARCHAR(255)             NOT NULL,
    start_latitude           DOUBLE PRECISION         NOT NULL,
    start_longitude          DOUBLE PRECISION         NOT NULL,

    destination_label        VARCHAR(255)             NOT NULL,
    destination_latitude     DOUBLE PRECISION         NOT NULL,
    destination_longitude    DOUBLE PRECISION         NOT NULL,

    departure_time           TIMESTAMP WITH TIME ZONE NOT NULL,

    activity_profile_code    VARCHAR(32)              NOT NULL,
    routing_profile          VARCHAR(64)              NOT NULL,

    status                   VARCHAR(20)              NOT NULL,

    arrival_time             TIMESTAMP WITH TIME ZONE,
    distance_meters          DOUBLE PRECISION,
    duration_seconds         DOUBLE PRECISION,

    geometry                 JSONB,
    segments                 JSONB,

    error_code               VARCHAR(64),
    error_message            VARCHAR(500),

    created_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_route_calculations
        PRIMARY KEY (id),

    CONSTRAINT chk_route_calculations_status
        CHECK (
            status IN (
                       'PENDING',
                       'PROCESSING',
                       'COMPLETED',
                       'FAILED'
                )
            ),

    CONSTRAINT chk_route_calculations_start_label
        CHECK (LENGTH(TRIM(start_label)) > 0),

    CONSTRAINT chk_route_calculations_destination_label
        CHECK (LENGTH(TRIM(destination_label)) > 0),

    CONSTRAINT chk_route_calculations_start_latitude
        CHECK (
            start_latitude >= -90
                AND start_latitude <= 90
            ),

    CONSTRAINT chk_route_calculations_start_longitude
        CHECK (
            start_longitude >= -180
                AND start_longitude <= 180
            ),

    CONSTRAINT chk_route_calculations_destination_latitude
        CHECK (
            destination_latitude >= -90
                AND destination_latitude <= 90
            ),

    CONSTRAINT chk_route_calculations_destination_longitude
        CHECK (
            destination_longitude >= -180
                AND destination_longitude <= 180
            ),

    CONSTRAINT chk_route_calculations_distance
        CHECK (
            distance_meters IS NULL
                OR distance_meters >= 0
            ),

    CONSTRAINT chk_route_calculations_duration
        CHECK (
            duration_seconds IS NULL
                OR duration_seconds >= 0
            ),

    CONSTRAINT chk_route_calculations_error
        CHECK (
            (
                status = 'FAILED'
                    AND error_code IS NOT NULL
                    AND error_message IS NOT NULL
                )
                OR
            (
                status <> 'FAILED'
                    AND error_code IS NULL
                    AND error_message IS NULL
                )
            )
);

CREATE INDEX idx_route_calculations_requested_by_user
    ON warp_routing.route_calculations (
                                        requested_by_user_id
        );

CREATE INDEX idx_route_calculations_status
    ON warp_routing.route_calculations (
                                        status
        );

CREATE INDEX idx_route_calculations_created_at
    ON warp_routing.route_calculations (
                                        created_at
        );