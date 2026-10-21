package de.warp.storage.service;

import de.warp.storage.SavedRouteTestData;
import de.warp.storage.dto.CreateSavedRouteRequest;
import de.warp.storage.dto.UpdateSavedRouteRequest;
import de.warp.storage.entity.SavedRouteEntity;
import de.warp.storage.exceptions.InvalidSavedRouteRequestException;
import de.warp.storage.exceptions.SavedRouteNotFoundException;
import de.warp.storage.repository.SavedRouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedRouteServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-21T10:00:00Z");

    @Mock
    private SavedRouteRepository repository;

    private SavedRouteMapper mapper;
    private SavedRouteService service;

    @BeforeEach
    void setUp() {
        mapper = new SavedRouteMapper(JsonMapper.builder().build());
        service = new SavedRouteService(
                repository,
                mapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldPersistRouteForGivenUser() {
        when(repository.saveAndFlush(any(SavedRouteEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SavedRouteCreationResult result = service.create(
                "usr_1",
                null,
                SavedRouteTestData.sampleRequest()
        );

        ArgumentCaptor<SavedRouteEntity> captor =
                ArgumentCaptor.forClass(SavedRouteEntity.class);
        verify(repository).saveAndFlush(captor.capture());

        SavedRouteEntity saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo("usr_1");
        assertThat(saved.getId()).startsWith("route_");
        assertThat(saved.getWeatherOverallScore()).isEqualTo(78);
        assertThat(saved.getWeatherSegments()).hasSize(2);
        assertThat(result.created()).isTrue();
        assertThat(result.route().name())
                .isEqualTo("Fahrradtour nach Bad Mergentheim");
        assertThat(result.route().routeSnapshot().geometry().coordinates())
                .hasSize(3);
    }

    @Test
    void shouldReturnExistingRouteForKnownIdempotencyKey() {
        SavedRouteEntity existing = mapper.toNewEntity(
                "route_1",
                "usr_1",
                "key-1",
                SavedRouteTestData.sampleRequest(),
                NOW
        );

        when(repository.findByUserIdAndIdempotencyKey("usr_1", "key-1"))
                .thenReturn(Optional.of(existing));

        SavedRouteCreationResult result = service.create(
                "usr_1",
                "key-1",
                SavedRouteTestData.sampleRequest()
        );

        assertThat(result.created()).isFalse();
        assertThat(result.route().id()).isEqualTo("route_1");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectArrivalBeforeDeparture() {
        CreateSavedRouteRequest request =
                SavedRouteTestData.sampleRequest(
                        SavedRouteTestData.ARRIVAL,
                        SavedRouteTestData.DEPARTURE
                );

        assertThatThrownBy(
                () -> service.create("usr_1", null, request)
        ).isInstanceOf(InvalidSavedRouteRequestException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void shouldNotRevealRoutesOfOtherUsers() {
        when(repository.findByIdAndUserId("route_1", "usr_other"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("usr_other", "route_1"))
                .isInstanceOf(SavedRouteNotFoundException.class);
    }

    @Test
    void shouldNotDeleteRoutesOfOtherUsers() {
        when(repository.deleteOwnedRoute("route_1", "usr_other"))
                .thenReturn(0);

        assertThatThrownBy(() -> service.delete("usr_other", "route_1"))
                .isInstanceOf(SavedRouteNotFoundException.class);
    }

    @Test
    void shouldRejectUpdateWithoutAnyField() {
        assertThatThrownBy(
                () -> service.update(
                        "usr_1",
                        "route_1",
                        new UpdateSavedRouteRequest(null, null)
                )
        ).isInstanceOf(InvalidSavedRouteRequestException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void shouldUpdateNameAndFavorite() {
        SavedRouteEntity existing = mapper.toNewEntity(
                "route_1",
                "usr_1",
                null,
                SavedRouteTestData.sampleRequest(),
                Instant.parse("2026-10-20T10:00:00Z")
        );

        when(repository.findByIdAndUserId("route_1", "usr_1"))
                .thenReturn(Optional.of(existing));

        var response = service.update(
                "usr_1",
                "route_1",
                new UpdateSavedRouteRequest("Wochenendtour", true)
        );

        assertThat(response.name()).isEqualTo("Wochenendtour");
        assertThat(response.favorite()).isTrue();
        assertThat(existing.getUpdatedAt()).isEqualTo(NOW);
    }
}