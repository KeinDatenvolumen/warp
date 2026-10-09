package de.warp.bff.graphql.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import de.warp.bff.application.service.SystemHealthService;
import de.warp.bff.config.GraphQlConfig;
import de.warp.bff.graphql.SystemHealthGraphQlController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

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
}