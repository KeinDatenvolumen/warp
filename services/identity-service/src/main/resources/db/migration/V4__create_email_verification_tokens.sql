-- [KI-Log Nr. KI-11] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): E-Mail-Verifikations-Flow (WARP-007)
CREATE TABLE warp_identity.email_verification_tokens
(
    id          VARCHAR(40)             NOT NULL,
    user_id     VARCHAR(40)             NOT NULL,
    token_hash  VARCHAR(64)             NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_email_verification_tokens
        PRIMARY KEY (id),

    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id)
            REFERENCES warp_identity.users (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_email_verification_tokens_token_hash
        UNIQUE (token_hash),

    CONSTRAINT chk_email_verification_tokens_id_format
        CHECK (id LIKE 'evt\_%' ESCAPE '\'),

    CONSTRAINT chk_email_verification_tokens_hash_length
        CHECK (CHAR_LENGTH(token_hash) = 64),

    CONSTRAINT chk_email_verification_tokens_expiration
        CHECK (expires_at > created_at),

    CONSTRAINT chk_email_verification_tokens_usage
        CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE INDEX idx_email_verification_tokens_user_id
    ON warp_identity.email_verification_tokens (user_id);

CREATE INDEX idx_email_verification_tokens_open
    ON warp_identity.email_verification_tokens (user_id, expires_at)
    WHERE used_at IS NULL;