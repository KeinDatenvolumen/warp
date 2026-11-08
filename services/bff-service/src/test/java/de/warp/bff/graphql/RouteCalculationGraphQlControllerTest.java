// [KI-Log Nr. KI-163] (41862d2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-179] (5bc6d66) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-200] (c059ad7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: require verified email for route calculation (WARP-030)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.warp.bff.application.exception
        .InvalidRouteRequestException;
import de.warp.bff.application.service
        .RouteCalculationInput;
import de.warp.bff.application.service
        .RouteCalculationResult;
import de.warp.bff.application.service
        .RouteCalculationService;
import de.warp.bff.application.service
        .StartedRouteCalculation;
import de.warp.bff.security.AuthenticatedSessionPrincipal;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RouteCalculationGraphQlControllerTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-10-22T16:00:00Z");

    private static final AuthenticatedSessionPrincipal PRINCIPAL =
            new AuthenticatedSessionPrincipal(
                    "usr_123",
                    Instant.parse("2026-10-22T15:00:00Z"),
                    Instant.parse("2026-10-23T03:00:00Z"),
                    true
            );

    @Mock
    private RouteCalculationService routeCalculationService;

    private RouteCalculationGraphQlController controller;

    @BeforeEach
    void setUp() {
        controller = new RouteCalculationGraphQlController(
                routeCalculationService
        );
    }

    @Test
    void startsRouteCalculationForAuthenticatedUser() {
        RouteCalculationGraphQlController
                .RouteCalculationGraphQlInput input =
                createInput();

        when(
                routeCalculationService.startRouteCalculation(
                        "usr_123",
                        new RouteCalculationInput(
                                new RouteCalculationInput.RoutePoint(
                                        "Mosbach",
                                        49.3536,
                                        9.1511
                                ),
                                new RouteCalculationInput.RoutePoint(
                                        "Bad Mergentheim",
                                        49.4924,
                                        9.7736
                                ),
                                Instant.parse(
                                        "2026-10-23T06:00:00Z"
                                ),
                                "BICYCLE"
                        )
                )
        ).thenReturn(
                new StartedRouteCalculation(
                        "calc_123",
                        "PENDING",
                        CREATED_AT
                )
        );

        RouteCalculationGraphQlController
                .RouteCalculationPayload response =
                controller.startRouteCalculation(
                        input,
                        PRINCIPAL
                );

        assertThat(response.calculation())
                .isNotNull();

        assertThat(response.calculation().id())
                .isEqualTo("calc_123");

        assertThat(response.calculation().status())
                .isEqualTo(
                        RouteCalculationGraphQlController
                                .RouteCalculationStatus.PENDING
                );

        assertThat(response.calculation().createdAt().toInstant())
                .isEqualTo(CREATED_AT);

        assertThat(response.errors())
                .isEmpty();

        verify(routeCalculationService)
                .startRouteCalculation(
                        eq("usr_123"),
                        argThat(applicationInput ->
                                applicationInput.activityProfile()
                                        .equals("BICYCLE")
                                        && applicationInput.start()
                                        .label()
                                        .equals("Mosbach")
                                        && applicationInput
                                        .destination()
                                        .label()
                                        .equals(
                                                "Bad Mergentheim"
                                        )
                        )
                );
    }

    @Test
    void returnsMutationErrorForInvalidRouteRequest() {
        RouteCalculationGraphQlController
                .RouteCalculationGraphQlInput input =
                createInput();

        when(
                routeCalculationService.startRouteCalculation(
                        "usr_123",
                        new RouteCalculationInput(
                                new RouteCalculationInput.RoutePoint(
                                        "Mosbach",
                                        49.3536,
                                        9.1511
                                ),
                                new RouteCalculationInput.RoutePoint(
                                        "Bad Mergentheim",
                                        49.4924,
                                        9.7736
                                ),
                                Instant.parse(
                                        "2026-10-23T06:00:00Z"
                                ),
                                "BICYCLE"
                        )
                )
        ).thenThrow(
                new InvalidRouteRequestException()
        );

        RouteCalculationGraphQlController
                .RouteCalculationPayload response =
                controller.startRouteCalculation(
                        input,
                        PRINCIPAL
                );

        assertThat(response.calculation())
                .isNull();

        assertThat(response.errors())
                .hasSize(1);

        assertThat(response.errors().getFirst().code())
                .isEqualTo("INVALID_ROUTE_REQUEST");

        assertThat(response.errors().getFirst().message())
                .isEqualTo(
                        "Die Routenanfrage ist ungültig."
                );
    }

    @Test
    void rejectsRouteCalculationForUnverifiedEmail() {
        RouteCalculationGraphQlController
                .RouteCalculationGraphQlInput input =
                createInput();

        AuthenticatedSessionPrincipal unverifiedPrincipal =
                new AuthenticatedSessionPrincipal(
                        "usr_123",
                        Instant.parse("2026-10-22T15:00:00Z"),
                        Instant.parse("2026-10-23T03:00:00Z"),
                        false
                );

        RouteCalculationGraphQlController
                .RouteCalculationPayload response =
                controller.startRouteCalculation(
                        input,
                        unverifiedPrincipal
                );

        assertThat(response.calculation())
                .isNull();

        assertThat(response.errors())
                .hasSize(1);

        assertThat(response.errors().getFirst().code())
                .isEqualTo("EMAIL_NOT_VERIFIED");

        assertThat(response.errors().getFirst().message())
                .isEqualTo(
                        "Die E-Mail-Adresse ist noch nicht verifiziert."
                );

        verify(
                routeCalculationService,
                never()
        ).startRouteCalculation(
                any(),
                any()
        );
    }

    @Test
    void returnsCompletedRouteCalculationWithGeometry() {
        RouteCalculationResult result =
                createCompletedResult();

        when(
                routeCalculationService.getRouteCalculation(
                        "usr_123",
                        "calc_123"
                )
        ).thenReturn(result);

        RouteCalculationGraphQlController
                .RouteCalculationPayloadResult response =
                controller.routeCalculation(
                        "calc_123",
                        PRINCIPAL
                );

        assertThat(response.id())
                .isEqualTo("calc_123");

        assertThat(response.status())
                .isEqualTo(
                        RouteCalculationGraphQlController
                                .RouteCalculationStatus.COMPLETED
                );

        assertThat(response.start().label())
                .isEqualTo("Mosbach");

        assertThat(response.destination().label())
                .isEqualTo("Bad Mergentheim");

        assertThat(response.activityProfile().code())
                .isEqualTo(
                        RouteCalculationGraphQlController
                                .ActivityProfileCode.BICYCLE
                );

        assertThat(response.activityProfile().displayName())
                .isEqualTo("Fahrrad");

        assertThat(response.distanceMeters())
                .isEqualTo(57600);

        assertThat(response.durationSeconds())
                .isEqualTo(4320);

        assertThat(response.geometry())
                .isNotNull();

        assertThat(response.geometry().type())
                .isEqualTo("LineString");

        assertThat(response.geometry().coordinates())
                .containsExactly(
                        List.of(9.1511, 49.3536),
                        List.of(9.7736, 49.4924)
                );

        assertThat(response.segments())
                .isNotNull()
                .hasSize(1);

        assertThat(response.segments().getFirst().distanceMeters())
                .isEqualTo(57600.0);

        assertThat(response.segments().getFirst().durationSeconds())
                .isEqualTo(4320.0);

        assertThat(response.error())
                .isNull();
    }

    @Test
    void returnsFailedRouteCalculationWithStableError() {
        RouteCalculationResult result =
                new RouteCalculationResult(
                        "calc_123",
                        "FAILED",
                        new RouteCalculationResult.RoutePoint(
                                "Mosbach",
                                49.3536,
                                9.1511
                        ),
                        new RouteCalculationResult.RoutePoint(
                                "Bad Mergentheim",
                                49.4924,
                                9.7736
                        ),
                        new RouteCalculationResult
                                .ActivityProfileSnapshot(
                                "BICYCLE",
                                "Fahrrad"
                        ),
                        Instant.parse(
                                "2026-10-23T06:00:00Z"
                        ),
                        null,
                        null,
                        null,
                        null,
                        CREATED_AT,
                        new RouteCalculationResult
                                .RouteCalculationError(
                                "ROUTE_NOT_FOUND",
                                "Für Start, Ziel und "
                                        + "Aktivitätsprofil konnte "
                                        + "keine Route gefunden "
                                        + "werden."
                        )
                );

        when(
                routeCalculationService.getRouteCalculation(
                        "usr_123",
                        "calc_123"
                )
        ).thenReturn(result);

        RouteCalculationGraphQlController
                .RouteCalculationPayloadResult response =
                controller.routeCalculation(
                        "calc_123",
                        PRINCIPAL
                );

        assertThat(response.status())
                .isEqualTo(
                        RouteCalculationGraphQlController
                                .RouteCalculationStatus.FAILED
                );

        assertThat(response.error())
                .isNotNull();

        assertThat(response.error().code())
                .isEqualTo("ROUTE_NOT_FOUND");

        assertThat(response.geometry())
                .isNull();
    }

    @Test
    void rejectsMutationWithoutAuthenticatedSession() {
        assertThatThrownBy(
                () -> controller.startRouteCalculation(
                        createInput(),
                        null
                )
        )
                .isInstanceOf(
                        SessionInvalidException.class
                );
    }

    @Test
    void rejectsQueryWithoutAuthenticatedSession() {
        assertThatThrownBy(
                () -> controller.routeCalculation(
                        "calc_123",
                        null
                )
        )
                .isInstanceOf(
                        SessionInvalidException.class
                );
    }

    private RouteCalculationGraphQlController
            .RouteCalculationGraphQlInput createInput() {
        return new RouteCalculationGraphQlController
                .RouteCalculationGraphQlInput(
                new RouteCalculationGraphQlController
                        .RoutePointGraphQlInput(
                        "Mosbach",
                        49.3536,
                        9.1511
                ),
                new RouteCalculationGraphQlController
                        .RoutePointGraphQlInput(
                        "Bad Mergentheim",
                        49.4924,
                        9.7736
                ),
                OffsetDateTime.parse(
                        "2026-10-23T08:00:00+02:00"
                ),
                RouteCalculationGraphQlController
                        .ActivityProfileCode.BICYCLE
        );
    }

    private RouteCalculationResult createCompletedResult() {
        var geometry =
                JsonMapper.builder()
                        .build()
                        .readTree(
                                """
                                {
                                  "type": "LineString",
                                  "coordinates": [
                                    [9.1511, 49.3536],
                                    [9.7736, 49.4924]
                                  ]
                                }
                                """
                        );

        var segments =
                JsonMapper.builder()
                        .build()
                        .readTree(
                                """
                                [
                                  {
                                    "distance": 57600.0,
                                    "duration": 4320.0
                                  }
                                ]
                                """
                        );

        return new RouteCalculationResult(
                "calc_123",
                "COMPLETED",
                new RouteCalculationResult.RoutePoint(
                        "Mosbach",
                        49.3536,
                        9.1511
                ),
                new RouteCalculationResult.RoutePoint(
                        "Bad Mergentheim",
                        49.4924,
                        9.7736
                ),
                new RouteCalculationResult
                        .ActivityProfileSnapshot(
                        "BICYCLE",
                        "Fahrrad"
                ),
                Instant.parse("2026-10-23T06:00:00Z"),
                Instant.parse("2026-10-23T07:12:00Z"),
                57600.0,
                4320.0,
                geometry,
                segments,
                CREATED_AT,
                null
        );
    }
}