package de.warp.storage.repository;

import de.warp.storage.entity.SavedRouteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Alle Zugriffe auf Routen erfolgen ausschließlich über die Kombination
 * aus Route und Benutzer. Es gibt bewusst keine Methode, die eine Route
 * nur anhand der ID lädt oder löscht.
 */
public interface SavedRouteRepository
        extends JpaRepository<SavedRouteEntity, String> {

    @EntityGraph(attributePaths = "weatherSegments")
    Optional<SavedRouteEntity> findByIdAndUserId(String id, String userId);

    @EntityGraph(attributePaths = "weatherSegments")
    Optional<SavedRouteEntity> findByUserIdAndIdempotencyKey(
            String userId,
            String idempotencyKey
    );

    Page<SavedRouteEntity> findByUserId(String userId, Pageable pageable);

    Page<SavedRouteEntity> findByUserIdAndFavorite(
            String userId,
            boolean favorite,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from SavedRouteEntity r
            where r.id = :id and r.userId = :userId
            """)
    int deleteOwnedRoute(
            @Param("id") String id,
            @Param("userId") String userId
    );
}