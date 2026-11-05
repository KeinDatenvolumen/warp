-- [KI-Log Nr. KI-14] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Passwort-Reset-Workflow (WARP-008)
﻿CREATE TABLE warp_identity.password_reset_tokens (
    id VARCHAR(40) NOT NULL,
    user_id VARCHAR(40) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_password_reset_tokens
        PRIMARY KEY (id),

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES warp_identity.users (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_password_reset_tokens_token_hash
        UNIQUE (token_hash),

    CONSTRAINT chk_password_reset_tokens_id
        CHECK (id LIKE 'prt\_%' ESCAPE '\'),

    CONSTRAINT chk_password_reset_tokens_hash_length
        CHECK (CHAR_LENGTH(token_hash) = 64),

    CONSTRAINT chk_password_reset_tokens_expiration
        CHECK (expires_at > created_at),

    CONSTRAINT chk_password_reset_tokens_used_at
        CHECK (used_at IS NULL OR used_at >= created_at)
);

CREATE INDEX idx_password_reset_tokens_user_id
    ON warp_identity.password_reset_tokens (user_id);

CREATE INDEX idx_password_reset_tokens_open
    ON warp_identity.password_reset_tokens (user_id, expires_at)
    WHERE used_at IS NULL;
