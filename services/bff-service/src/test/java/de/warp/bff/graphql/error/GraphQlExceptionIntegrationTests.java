package de.warp.bff.graphql.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.warp.bff.application.service.SystemHealthService;
import de.warp.bff.config.GraphQlConfig;
import de.warp.bff.graphql.SystemHealthGraphQlController;
import de.warp.bff.graphql.SessionInvalidException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import de.warp.bff.application.exception
        .RouteCalculationNotFoundException;
import de.warp.bff.application.exception
        .RoutingServiceUnavailableException;

@GraphQlTest(SystemHealthGraphQlController.class)
@Import({
        GraphQlConfig.class,
        GlobalGraphQlExceptionResolver.class
})
class GraphQlExceptionIntegrationTests {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private SystemHealthService systemHealthService;

    @Test
    void returnsStableCodeForUnexpectedResolverException() {
        when(systemHealthService.getSystemHealth())
                .thenThrow(
                        new IllegalStateException(
                                "Interne vertrauliche Information"
                        )
                );

        graphQlTester
                .document("""
                        query SystemHealth {
                            systemHealth {
                                status
                            }
                        }
                        """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors)
                            .hasSize(1);

                    var error = errors.getFirst();

                    assertThat(error.getMessage())
                            .isEqualTo(
                                    "Ein interner Fehler ist aufgetreten."
                            );

                    assertThat(error.getExtensions())
                            .containsEntry(
                                    "code",
                                    "INTERNAL_SERVER_ERROR"
                            );

                    assertThat(error.getMessage())
                            .doesNotContain(
                                    "Interne vertrauliche Information"
                            );
                });
    }

    @Test
    void returnsStableCodeForIllegalArgumentException() {
        when(systemHealthService.getSystemHealth())
                .thenThrow(
                        new IllegalArgumentException(
                                "Internes ungültiges Argument"
                        )
                );

        graphQlTester
                .document("""
                        query SystemHealth {
                            systemHealth {
                                status
                            }
                        }
                        """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors)
                            .hasSize(1);

                    var error = errors.getFirst();

                    assertThat(error.getMessage())
                            .isEqualTo(
                                    "Die Anfrage ist ungültig."
                            );

                    assertThat(error.getExtensions())
                            .containsEntry(
                                    "code",
                                    "BAD_REQUEST"
                            );

                    assertThat(error.getMessage())
                            .doesNotContain(
                                    "Internes ungültiges Argument"
                            );
                });
    }

    @Test
    void returnsSessionInvalidForInvalidSession() {
        when(systemHealthService.getSystemHealth())
                .thenThrow(
                        new SessionInvalidException(
                                new IllegalStateException(
                                        "Interner Session-Token darf nicht erscheinen"
                                )
                        )
                );

        graphQlTester
                .document("""
                    query SystemHealth {
                        systemHealth {
                            status
                        }
                    }
                    """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors)
                            .hasSize(1);

                    var error = errors.getFirst();

                    assertThat(error.getMessage())
                            .isEqualTo(
                                    "Die Sitzung ist ungültig oder abgelaufen."
                            );

                    assertThat(error.getExtensions())
                            .containsEntry(
                                    "code",
                                    "SESSION_INVALID"
                            );

                    assertThat(error.getMessage())
                            .doesNotContain(
                                    "Interner Session-Token"
                            );
                });
    }
    @Test
    void returnsRouteCalculationNotFoundCode() {
        when(systemHealthService.getSystemHealth())
                .thenThrow(
                        new RouteCalculationNotFoundException()
                );

        graphQlTester
                .document("""
                    query SystemHealth {
                        systemHealth {
                            status
                        }
                    }
                    """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors)
                            .hasSize(1);

                    var error = errors.getFirst();

                    assertThat(error.getMessage())
                            .isEqualTo(
                                    "Die Routenberechnung wurde "
                                            + "nicht gefunden."
                            );

                    assertThat(error.getExtensions())
                            .containsEntry(
                                    "code",
                                    "ROUTE_CALCULATION_NOT_FOUND"
                            );
                });
    }

    @Test
    void returnsRoutingServiceUnavailableCode() {
        when(systemHealthService.getSystemHealth())
                .thenThrow(
                        new RoutingServiceUnavailableException()
                );

        graphQlTester
                .document("""
                    query SystemHealth {
                        systemHealth {
                            status
                        }
                    }
                    """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors)
                            .hasSize(1);

                    var error = errors.getFirst();

                    assertThat(error.getMessage())
                            .isEqualTo(
                                    "Die Routenberechnung ist momentan "
                                            + "nicht verfügbar."
                            );

                    assertThat(error.getExtensions())
                            .containsEntry(
                                    "code",
                                    "ROUTING_SERVICE_UNAVAILABLE"
                            );
                });
    }
}