// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
package de.warp.weather.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WeatherCacheRepository extends JpaRepository<WeatherCacheEntity, String> {

    List<WeatherCacheEntity> findByCacheKeyIn(Collection<String> cacheKeys);
}
