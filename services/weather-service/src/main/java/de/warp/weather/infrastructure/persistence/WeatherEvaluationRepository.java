package de.warp.weather.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WeatherEvaluationRepository extends JpaRepository<WeatherEvaluationEntity, String> {

    Optional<WeatherEvaluationEntity> findFirstByRouteCalculationIdOrderByCreatedAtDesc(
            String routeCalculationId
    );
}
