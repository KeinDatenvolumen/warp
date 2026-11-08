// [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-163] (41862d2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-179] (5bc6d66) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose route calculations via GraphQL
// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
import de.warp.bff.application.exception.EmailNotVerifiedException;
import de.warp.bff.application.exception.RouteCalculationNotFoundException;
import de.warp.bff.application.exception.RoutingServiceUnavailableException;
import de.warp.bff.application.exception.SavedRouteNotFoundException;
import de.warp.bff.application.exception.StorageServiceUnavailableException;

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

    @Test
    void returnsSavedRouteNotFoundCode() {
        when(systemHealthService.getSystemHealth())
                .thenThrow(new SavedRouteNotFoundException(new RuntimeException()));

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
                    assertThat(errors).hasSize(1);
                    var error = errors.getFirst();
                    assertThat(error.getMessage()).isEqualTo("Die gespeicherte Route wurde nicht gefunden.");
                    assertThat(error.getExtensions()).containsEntry("code", "SAVED_ROUTE_NOT_FOUND");
                });
    }

    @Test
    void returnsStorageServiceUnavailableCode() {
        when(systemHealthService.getSystemHealth())
                .thenThrow(new StorageServiceUnavailableException(new RuntimeException()));

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
                    assertThat(errors).hasSize(1);
                    var error = errors.getFirst();
                    assertThat(error.getMessage()).isEqualTo("Der Speicherdienst ist momentan nicht verfügbar.");
                    assertThat(error.getExtensions()).containsEntry("code", "DOWNSTREAM_SERVICE_UNAVAILABLE");
                });
    }

    @Test
    void returnsEmailNotVerifiedCode() {
        when(systemHealthService.getSystemHealth())
                .thenThrow(new EmailNotVerifiedException());

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
                    assertThat(errors).hasSize(1);
                    var error = errors.getFirst();
                    assertThat(error.getMessage()).isEqualTo("Die E-Mail-Adresse ist noch nicht verifiziert.");
                    assertThat(error.getExtensions()).containsEntry("code", "EMAIL_NOT_VERIFIED");
                });
    }
}