package de.warp.routing.route.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteCalculationRepository
        extends JpaRepository<RouteCalculationEntity, String> {

    List<RouteCalculationEntity> findByRequestedByUserIdOrderByCreatedAtDesc(
            String requestedByUserId
    );
}