package de.warp.weather.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WeatherCacheRepository extends JpaRepository<WeatherCacheEntity, String> {

    List<WeatherCacheEntity> findByCacheKeyIn(Collection<String> cacheKeys);
}
