// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.storage.repository;

import de.warp.storage.entity.SavedRouteEntity;
import de.warp.storage.entity.SavedRouteWeatherSegmentEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class SavedRouteRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private SavedRouteRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistRouteWithOrderedSegments() {
        repository.saveAndFlush(newRoute("route_1", "usr_owner", false));
        entityManager.clear();

        SavedRouteEntity loaded = repository
                .findByIdAndUserId("route_1", "usr_owner")
                .orElseThrow();

        assertThat(loaded.getName()).isEqualTo("Testroute");
        assertThat(loaded.getWeatherOverallScore()).isEqualTo(78);
        assertThat(loaded.getWeatherSegments())
                .extracting(SavedRouteWeatherSegmentEntity::getSegmentIndex)
                .containsExactly(0, 1);
    }

    @Test
    void shouldNotFindRouteOfAnotherUser() {
        repository.saveAndFlush(newRoute("route_1", "usr_owner", false));
        entityManager.clear();

        assertThat(repository.findByIdAndUserId("route_1", "usr_other"))
                .isEmpty();
    }

    @Test
    void shouldOnlyListRoutesOfRequestedUserAndFilterFavorites() {
        repository.saveAndFlush(newRoute("route_1", "usr_owner", false));
        repository.saveAndFlush(newRoute("route_2", "usr_owner", true));
        repository.saveAndFlush(newRoute("route_3", "usr_other", true));

        assertThat(
                repository.findByUserId("usr_owner", PageRequest.of(0, 10))
                        .getTotalElements()
        ).isEqualTo(2);

        assertThat(
                repository.findByUserIdAndFavorite(
                        "usr_owner", true, PageRequest.of(0, 10)
                ).getContent()
        ).extracting(SavedRouteEntity::getId).containsExactly("route_2");
    }

    @Test
    void shouldDeleteOnlyOwnRouteAndCascadeSegments() {
        repository.saveAndFlush(newRoute("route_1", "usr_owner", false));

        assertThat(repository.deleteOwnedRoute("route_1", "usr_other"))
                .isZero();
        assertThat(repository.deleteOwnedRoute("route_1", "usr_owner"))
                .isEqualTo(1);

        Integer remainingSegments = jdbcTemplate.queryForObject(
                "select count(*) from warp_storage.saved_route_weather_segments",
                Integer.class
        );

        assertThat(remainingSegments).isZero();
    }

    private SavedRouteEntity newRoute(
            String id,
            String userId,
            boolean favorite
    ) {
        Instant now = Instant.parse("2026-10-21T10:00:00Z");

        SavedRouteEntity route = new SavedRouteEntity(
                id,
                userId,
                "Testroute",
                favorite,
                "Mosbach", 49.3536, 9.1511,
                "Bad Mergentheim", 49.4924, 9.7736,
                "BICYCLE",
                now.plusSeconds(3600),
                now.plusSeconds(15120),
                57600,
                11520,
                "{\"type\":\"LineString\",\"coordinates\":"
                        + "[[9.1511,49.3536],[9.7736,49.4924]]}",
                "calc_123",
                "weather_123",
                78,
                "1.0",
                false,
                null,
                now
        );

        route.addWeatherSegment(segment(1, 70));
        route.addWeatherSegment(segment(0, 86));

        return route;
    }

    private SavedRouteWeatherSegmentEntity segment(int index, int score) {
        return new SavedRouteWeatherSegmentEntity(
                index,
                Instant.parse("2026-10-21T11:00:00Z"),
                score,
                "#55AA40",
                15.0, 230.0, 20.0, 0.2, 5.0,
                null
        );
    }
}