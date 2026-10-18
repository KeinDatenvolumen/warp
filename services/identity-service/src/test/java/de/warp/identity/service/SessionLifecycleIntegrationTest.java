package de.warp.identity.service;

import de.warp.identity.dto.CreateSessionRequest;
import de.warp.identity.dto.CreatedSessionResponse;
import de.warp.identity.dto.SessionResponse;
import de.warp.identity.entity.SessionEntity;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.SessionExpiredException;
import de.warp.identity.repository.SessionRepository;
import de.warp.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Transactional
class SessionLifecycleIntegrationTest {

    private static final String USER_ID =
            "usr_session_integration";

    private static final String EMAIL =
            "session.integration@example.de";

    private static final String RAW_PASSWORD =
            "MeinSicheresPasswort123";

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18")
                    .withDatabaseName(
                            "warp_identity_session_test"
                    )
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );
        registry.add(
                "warp.session.duration",
                () -> "PT12H"
        );
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SessionTokenService sessionTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldPersistValidateAndInvalidateSession() {
        UserEntity user = new UserEntity(
                USER_ID,
                "Max Mustermann",
                EMAIL,
                passwordEncoder.encode(RAW_PASSWORD)
        );

        userRepository.saveAndFlush(user);

        CreatedSessionResponse createdSession =
                sessionService.createSession(
                        new CreateSessionRequest(
                                EMAIL,
                                RAW_PASSWORD
                        )
                );

        assertThat(createdSession.sessionToken())
                .isNotBlank();

        assertThat(createdSession.userId())
                .isEqualTo(USER_ID);

        assertThat(createdSession.expiresAt())
                .isAfter(createdSession.createdAt());

        String tokenHash =
                sessionTokenService.hashToken(
                        createdSession.sessionToken()
                );

        SessionEntity persistedSession =
                sessionRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow();

        assertThat(persistedSession.getId())
                .startsWith("ses_");

        assertThat(persistedSession.getTokenHash())
                .isEqualTo(tokenHash);

        assertThat(persistedSession.getTokenHash())
                .isNotEqualTo(
                        createdSession.sessionToken()
                );

        assertThat(persistedSession.getUser().getId())
                .isEqualTo(USER_ID);

        assertThat(persistedSession.getInvalidatedAt())
                .isNull();

        SessionResponse validatedSession =
                sessionService.validateSession(
                        createdSession.sessionToken()
                );

        assertThat(validatedSession.userId())
                .isEqualTo(USER_ID);

        assertThat(validatedSession.createdAt())
                .isEqualTo(
                        createdSession.createdAt()
                );

        assertThat(validatedSession.expiresAt())
                .isEqualTo(
                        createdSession.expiresAt()
                );

        sessionService.invalidateSession(
                createdSession.sessionToken()
        );

        sessionRepository.flush();

        SessionEntity invalidatedSession =
                sessionRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow();

        assertThat(invalidatedSession.getInvalidatedAt())
                .isNotNull();

        assertThatThrownBy(
                () -> sessionService.validateSession(
                        createdSession.sessionToken()
                )
        )
                .isInstanceOf(
                        SessionExpiredException.class
                )
                .hasMessage(
                        "Die Sitzung ist abgelaufen "
                                + "oder wurde invalidiert."
                );
    }
}