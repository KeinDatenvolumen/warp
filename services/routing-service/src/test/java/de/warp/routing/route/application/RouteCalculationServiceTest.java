package de.warp.routing.route.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import de.warp.routing.profile.domain.ActivityProfileCode;
import de.warp.routing.route.api.ActivityProfileSnapshotRequest;
import de.warp.routing.route.api.RouteCalculationAcceptedResponse;
import de.warp.routing.route.api.RoutePointRequest;
import de.warp.routing.route.api.StartRouteCalculationRequest;
import de.warp.routing.route.domain.RouteCalculationStatus;
import de.warp.routing.route.persistence.RouteCalculationEntity;
import de.warp.routing.route.persistence.RouteCalculationRepository;

@ExtendWith(MockitoExtension.class)
class RouteCalculationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-21T18:00:00Z");

    @Mock
    private RouteCalculationRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RouteCalculationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                NOW,
                ZoneOffset.UTC
        );

        service = new RouteCalculationService(
                repository,
                clock,
                eventPublisher
        );
    }

    @Test
    void createsPendingRouteCalculation() {
        when(
                repository.findByIdempotencyKey(
                        "idempotency-key-123"
                )
        ).thenReturn(Optional.empty());

        when(repository.save(any(RouteCalculationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StartRouteCalculationRequest request =
                validRequest();

        RouteCalculationAcceptedResponse response =
                service.startCalculation(
                        "idempotency-key-123",
                        request
                );

        ArgumentCaptor<RouteCalculationEntity> captor =
                ArgumentCaptor.forClass(
                        RouteCalculationEntity.class
                );

        verify(repository).save(captor.capture());

        RouteCalculationEntity savedCalculation =
                captor.getValue();

        assertThat(savedCalculation.getId())
                .startsWith("calc_")
                .hasSize(37);

        assertThat(savedCalculation.getRequestedByUserId())
                .isEqualTo("usr_123");

        assertThat(savedCalculation.getIdempotencyKey())
                .isEqualTo("idempotency-key-123");

        assertThat(savedCalculation.getRoutingProfile())
                .isEqualTo("cycling-regular");

        assertThat(savedCalculation.getStatus())
                .isEqualTo(RouteCalculationStatus.PENDING);

        assertThat(savedCalculation.getCreatedAt())
                .isEqualTo(NOW);

        assertThat(response.id())
                .isEqualTo(savedCalculation.getId());

        assertThat(response.status())
                .isEqualTo(RouteCalculationStatus.PENDING);

        assertThat(response.createdAt())
                .isEqualTo(NOW);

        ArgumentCaptor<Object> eventCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(eventPublisher).publishEvent(
                eventCaptor.capture()
        );

        assertThat(eventCaptor.getValue())
                .isInstanceOf(RouteCalculationCreatedEvent.class);

        RouteCalculationCreatedEvent publishedEvent =
                (RouteCalculationCreatedEvent) eventCaptor.getValue();

        assertThat(publishedEvent.calculationId())
                .isEqualTo(savedCalculation.getId());
    }

    @Test
    void returnsExistingRouteCalculation() {
        RouteCalculationEntity calculation =
                createCalculation(
                        "calc_123"
                );

        when(repository.findById("calc_123"))
                .thenReturn(Optional.of(calculation));

        RouteCalculationEntity result =
                service.getCalculation("calc_123");

        assertThat(result).isSameAs(calculation);
    }

    @Test
    void returnsExistingRouteCalculationForOwner() {
        RouteCalculationEntity calculation =
                createCalculation(
                        "calc_123"
                );

        when(repository.findByIdAndRequestedByUserId("calc_123", "usr_123"))
                .thenReturn(Optional.of(calculation));

        RouteCalculationEntity result =
                service.getCalculation("calc_123", "usr_123");

        assertThat(result).isSameAs(calculation);
    }

    @Test
    void rejectsCalculationWhenRequestedByDifferentUser() {
        when(repository.findByIdAndRequestedByUserId("calc_123", "usr_other"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.getCalculation("calc_123", "usr_other")
        ).isInstanceOf(RouteCalculationNotFoundException.class);
    }

    @Test
    void rejectsUnknownCalculationId() {
        when(repository.findById("calc_missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.getCalculation(
                        "calc_missing"
                )
        )
                .isInstanceOf(
                        RouteCalculationNotFoundException.class
                )
                .hasMessageContaining("calc_missing");
    }

    @Test
    void returnsExistingCalculationForSameIdempotencyKey() {
        RouteCalculationEntity existingCalculation =
                createCalculation(
                        "calc_existing"
                );

        when(
                repository.findByIdempotencyKey(
                        "idempotency-key-123"
                )
        ).thenReturn(Optional.of(existingCalculation));

        RouteCalculationAcceptedResponse response =
                service.startCalculation(
                        "idempotency-key-123",
                        validRequest()
                );

        assertThat(response.id())
                .isEqualTo("calc_existing");

        assertThat(response.status())
                .isEqualTo(RouteCalculationStatus.PENDING);

        verify(repository).findByIdempotencyKey(
                "idempotency-key-123"
        );

        verify(repository, never())
                .save(any(RouteCalculationEntity.class));

        verify(eventPublisher, never())
                .publishEvent(any(Object.class));
    }

    @Test
    void rejectsInvalidCoordinates() {
        StartRouteCalculationRequest request =
                new StartRouteCalculationRequest(
                        "usr_123",
                        new RoutePointRequest(
                                "Mosbach",
                                100,
                                9.1511
                        ),
                        new RoutePointRequest(
                                "Bad Mergentheim",
                                49.4924,
                                9.7736
                        ),
                        Instant.parse(
                                "2026-10-22T06:00:00Z"
                        ),
                        new ActivityProfileSnapshotRequest(
                                ActivityProfileCode.BICYCLE,
                                "cycling-regular"
                        )
                );

        assertThatThrownBy(
                () -> service.startCalculation(
                        "idempotency-key-123",
                        request
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining("Breitengrad");
    }

    private StartRouteCalculationRequest validRequest() {
        return new StartRouteCalculationRequest(
                "usr_123",
                new RoutePointRequest(
                        "Mosbach",
                        49.3536,
                        9.1511
                ),
                new RoutePointRequest(
                        "Bad Mergentheim",
                        49.4924,
                        9.7736
                ),
                Instant.parse(
                        "2026-10-22T06:00:00Z"
                ),
                new ActivityProfileSnapshotRequest(
                        ActivityProfileCode.BICYCLE,
                        "cycling-regular"
                )
        );
    }

    private RouteCalculationEntity createCalculation(
            String id
    ) {
        return new RouteCalculationEntity(
                id,
                "usr_123",
                "idempotency-key-123",
                "Mosbach",
                49.3536,
                9.1511,
                "Bad Mergentheim",
                49.4924,
                9.7736,
                Instant.parse(
                        "2026-10-22T06:00:00Z"
                ),
                ActivityProfileCode.BICYCLE,
                "cycling-regular",
                NOW
        );
    }
}