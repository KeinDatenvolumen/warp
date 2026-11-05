-- [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
CREATE TABLE warp_identity.users
(
    id             VARCHAR(40)              NOT NULL,
    name           VARCHAR(255)             NOT NULL,
    email          VARCHAR(320)             NOT NULL,
    password_hash  VARCHAR(255)             NOT NULL,
    email_verified BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),

    CONSTRAINT chk_users_name_not_blank
        CHECK (LENGTH(TRIM(name)) > 0),

    CONSTRAINT chk_users_email_not_blank
        CHECK (LENGTH(TRIM(email)) > 0)
);

CREATE UNIQUE INDEX uk_users_email_normalized
    ON warp_identity.users (LOWER(TRIM(email)));