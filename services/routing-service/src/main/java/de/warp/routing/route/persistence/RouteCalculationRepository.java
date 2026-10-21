package de.warp.routing.route.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteCalculationRepository
        extends JpaRepository<RouteCalculationEntity, String> {

    Optional<RouteCalculationEntity> findByIdempotencyKey(
            String idempotencyKey
    );

    List<RouteCalculationEntity> findByRequestedByUserIdOrderByCreatedAtDesc(
            String requestedByUserId
    );
}