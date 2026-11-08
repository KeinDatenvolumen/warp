ALTER TABLE warp_identity.users
    ADD COLUMN IF NOT EXISTS pending_email VARCHAR(320);
