package de.warp.identity.service;
import de.warp.identity.exceptions.InvalidCredentialsException;

import de.warp.identity.dto.CreateSessionRequest;
import de.warp.identity.dto.CreatedSessionResponse;
import de.warp.identity.entity.SessionEntity;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.SessionRepository;
import de.warp.identity.repository.UserRepository;
import de.warp.identity.dto.SessionResponse;
import de.warp.identity.exceptions.SessionExpiredException;
import de.warp.identity.exceptions.SessionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;


class SessionServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-15T12:00:00Z");

    private UserRepository userRepository;
    private SessionRepository sessionRepository;
    private PasswordEncoder passwordEncoder;
    private SessionTokenService sessionTokenService;
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        sessionRepository = mock(SessionRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        sessionTokenService = mock(SessionTokenService.class);

        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        Duration sessionDuration = Duration.ofHours(12);

        when(passwordEncoder.encode(
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn("dummy-password-hash");

        sessionService = new SessionService(
                userRepository,
                sessionRepository,
                passwordEncoder,
                sessionTokenService,
                clock,
                sessionDuration
        );
    }

    @Test
    void shouldCreateSessionForValidCredentials() {
        UserEntity user = new UserEntity(
                "usr_123",
                "Max Mustermann",
                "max@example.de",
                "stored-password-hash"
        );

        CreateSessionRequest request = new CreateSessionRequest(
                "max@example.de",
                "MeinSicheresPasswort123"
        );

        when(userRepository.findByEmailIgnoreCase("max@example.de"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "MeinSicheresPasswort123",
                "stored-password-hash"
        )).thenReturn(true);

        when(sessionTokenService.generateToken())
                .thenReturn("generated-session-token");

        when(sessionTokenService.hashToken("generated-session-token"))
                .thenReturn("a".repeat(64));

        CreatedSessionResponse response =
                sessionService.createSession(request);

        assertThat(response.sessionToken())
                .isEqualTo("generated-session-token");

        assertThat(response.userId())
                .isEqualTo("usr_123");

        assertThat(response.createdAt())
                .isEqualTo(NOW);

        assertThat(response.expiresAt())
                .isEqualTo(NOW.plus(Duration.ofHours(12)));

        ArgumentCaptor<SessionEntity> sessionCaptor =
                ArgumentCaptor.forClass(SessionEntity.class);

        verify(sessionRepository).save(sessionCaptor.capture());

        SessionEntity savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getId())
                .startsWith("ses_");

        assertThat(savedSession.getTokenHash())
                .isEqualTo("a".repeat(64));

        assertThat(savedSession.getTokenHash())
                .isNotEqualTo(response.sessionToken());

        assertThat(savedSession.getUser())
                .isSameAs(user);

        assertThat(savedSession.getCreatedAt())
                .isEqualTo(NOW);

        assertThat(savedSession.getExpiresAt())
                .isEqualTo(NOW.plus(Duration.ofHours(12)));

        assertThat(savedSession.getInvalidatedAt())
                .isNull();
    }
    @Test
    void shouldRejectUnknownEmailAddress() {
        CreateSessionRequest request = new CreateSessionRequest(
                "unknown@example.de",
                "MeinSicheresPasswort123"
        );

        when(userRepository.findByEmailIgnoreCase("unknown@example.de"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.matches(
                "MeinSicheresPasswort123",
                "dummy-password-hash"
        )).thenReturn(false);

        assertThatThrownBy(() -> sessionService.createSession(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(
                        "Die E-Mail-Adresse oder das Passwort ist nicht korrekt."
                );

        verify(passwordEncoder).matches(
                "MeinSicheresPasswort123",
                "dummy-password-hash"
        );

        verify(sessionRepository, never()).save(any(SessionEntity.class));
    }
    @Test
    void shouldRejectIncorrectPassword() {
        UserEntity user = new UserEntity(
                "usr_123",
                "Max Mustermann",
                "max@example.de",
                "stored-password-hash"
        );

        CreateSessionRequest request = new CreateSessionRequest(
                "max@example.de",
                "FalschesPasswort123"
        );

        when(userRepository.findByEmailIgnoreCase("max@example.de"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "FalschesPasswort123",
                "stored-password-hash"
        )).thenReturn(false);

        assertThatThrownBy(() -> sessionService.createSession(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(
                        "Die E-Mail-Adresse oder das Passwort ist nicht korrekt."
                );

        verify(passwordEncoder).matches(
                "FalschesPasswort123",
                "stored-password-hash"
        );

        verify(sessionRepository, never()).save(any(SessionEntity.class));
    }

    @Test
    void shouldValidateActiveSession() {
        UserEntity user = new UserEntity(
                "usr_123",
                "Max Mustermann",
                "max@example.de",
                "stored-password-hash"
        );

        SessionEntity session = new SessionEntity(
                "ses_123",
                "a".repeat(64),
                user,
                NOW.minus(Duration.ofHours(1)),
                NOW.plus(Duration.ofHours(11))
        );

        when(sessionTokenService.hashToken("valid-session-token"))
                .thenReturn("a".repeat(64));

        when(sessionRepository.findByTokenHash("a".repeat(64)))
                .thenReturn(Optional.of(session));

        SessionResponse response =
                sessionService.validateSession("valid-session-token");

        assertThat(response.id()).isEqualTo("ses_123");
        assertThat(response.userId()).isEqualTo("usr_123");
        assertThat(response.createdAt())
                .isEqualTo(NOW.minus(Duration.ofHours(1)));
        assertThat(response.expiresAt())
                .isEqualTo(NOW.plus(Duration.ofHours(11)));

        verify(sessionTokenService)
                .hashToken("valid-session-token");

        verify(sessionRepository)
                .findByTokenHash("a".repeat(64));
    }

    @Test
    void shouldRejectUnknownSession() {
        when(sessionTokenService.hashToken("unknown-session-token"))
                .thenReturn("b".repeat(64));

        when(sessionRepository.findByTokenHash("b".repeat(64)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> sessionService.validateSession(
                        "unknown-session-token"
                )
        )
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessage("Die Sitzung wurde nicht gefunden.");
    }

    @Test
    void shouldRejectExpiredSession() {
        UserEntity user = new UserEntity(
                "usr_123",
                "Max Mustermann",
                "max@example.de",
                "stored-password-hash"
        );

        SessionEntity session = new SessionEntity(
                "ses_123",
                "c".repeat(64),
                user,
                NOW.minus(Duration.ofHours(13)),
                NOW.minus(Duration.ofHours(1))
        );

        when(sessionTokenService.hashToken("expired-session-token"))
                .thenReturn("c".repeat(64));

        when(sessionRepository.findByTokenHash("c".repeat(64)))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(
                () -> sessionService.validateSession(
                        "expired-session-token"
                )
        )
                .isInstanceOf(SessionExpiredException.class)
                .hasMessage(
                        "Die Sitzung ist abgelaufen oder wurde invalidiert."
                );
    }

    @Test
    void shouldRejectInvalidatedSession() {
        UserEntity user = new UserEntity(
                "usr_123",
                "Max Mustermann",
                "max@example.de",
                "stored-password-hash"
        );

        SessionEntity session = new SessionEntity(
                "ses_123",
                "d".repeat(64),
                user,
                NOW.minus(Duration.ofHours(1)),
                NOW.plus(Duration.ofHours(11))
        );

        session.invalidate(NOW.minus(Duration.ofMinutes(10)));

        when(sessionTokenService.hashToken("invalidated-session-token"))
                .thenReturn("d".repeat(64));

        when(sessionRepository.findByTokenHash("d".repeat(64)))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(
                () -> sessionService.validateSession(
                        "invalidated-session-token"
                )
        )
                .isInstanceOf(SessionExpiredException.class)
                .hasMessage(
                        "Die Sitzung ist abgelaufen oder wurde invalidiert."
                );
    }

    @Test
    void shouldInvalidateExistingSession() {
        UserEntity user = new UserEntity(
                "usr_123",
                "Max Mustermann",
                "max@example.de",
                "stored-password-hash"
        );

        SessionEntity session = new SessionEntity(
                "ses_123",
                "e".repeat(64),
                user,
                NOW.minus(Duration.ofHours(1)),
                NOW.plus(Duration.ofHours(11))
        );

        when(sessionTokenService.hashToken("valid-session-token"))
                .thenReturn("e".repeat(64));

        when(sessionRepository.findByTokenHash("e".repeat(64)))
                .thenReturn(Optional.of(session));

        sessionService.invalidateSession("valid-session-token");

        assertThat(session.getInvalidatedAt())
                .isEqualTo(NOW);

        assertThat(session.isValidAt(NOW))
                .isFalse();

        verify(sessionRepository).save(session);
    }

    @Test
    void shouldIgnoreUnknownSessionDuringInvalidation() {
        when(sessionTokenService.hashToken("unknown-session-token"))
                .thenReturn("f".repeat(64));

        when(sessionRepository.findByTokenHash("f".repeat(64)))
                .thenReturn(Optional.empty());

        sessionService.invalidateSession("unknown-session-token");

        verify(sessionRepository, never())
                .save(any(SessionEntity.class));
    }
}