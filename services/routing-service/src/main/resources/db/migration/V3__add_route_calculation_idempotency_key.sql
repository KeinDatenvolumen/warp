ALTER TABLE warp_routing.route_calculations
    ADD COLUMN idempotency_key VARCHAR(128);

UPDATE warp_routing.route_calculations
SET idempotency_key = 'legacy_' || id
WHERE idempotency_key IS NULL;

ALTER TABLE warp_routing.route_calculations
    ALTER COLUMN idempotency_key SET NOT NULL;

ALTER TABLE warp_routing.route_calculations
    ADD CONSTRAINT uk_route_calculations_idempotency_key
        UNIQUE (idempotency_key);