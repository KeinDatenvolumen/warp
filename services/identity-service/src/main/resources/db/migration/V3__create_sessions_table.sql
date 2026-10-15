CREATE TABLE warp_identity.sessions
(
    id            VARCHAR(40)              NOT NULL,
    token_hash    VARCHAR(64)                 NOT NULL,
    user_id       VARCHAR(40)              NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    invalidated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_sessions
        PRIMARY KEY (id),

    CONSTRAINT uk_sessions_token_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_sessions_user
        FOREIGN KEY (user_id)
            REFERENCES warp_identity.users (id),

    CONSTRAINT chk_sessions_expiration
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_sessions_user_id
    ON warp_identity.sessions (user_id);

CREATE INDEX idx_sessions_expires_at
    ON warp_identity.sessions (expires_at);