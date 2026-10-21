package de.warp.identity.service;

import de.warp.identity.entity.EmailVerificationTokenEntity;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.InvalidEmailVerificationTokenException;
import de.warp.identity.repository.EmailVerificationTokenRepository;
import de.warp.identity.security.EmailVerificationTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-21T14:00:00Z");

    private EmailVerificationTokenRepository tokenRepository;
    private EmailVerificationTokenGenerator tokenGenerator;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(
                EmailVerificationTokenRepository.class
        );

        tokenGenerator = new EmailVerificationTokenGenerator();

        Clock clock = Clock.fixed(
                NOW,
                ZoneOffset.UTC
        );

        service = new EmailVerificationService(
                tokenRepository,
                tokenGenerator,
                clock
        );
    }

    @Test
    void shouldCreateAndStoreHashedVerificationToken() {
        UserEntity user = createUser();

        String plainToken =
                service.createVerificationToken(user);

        ArgumentCaptor<EmailVerificationTokenEntity> captor =
                ArgumentCaptor.forClass(
                        EmailVerificationTokenEntity.class
                );

        verify(tokenRepository)
                .markAllUnusedTokensAsUsed(
                        user.getId(),
                        NOW
                );

        verify(tokenRepository).save(captor.capture());

        EmailVerificationTokenEntity savedToken =
                captor.getValue();

        assertThat(plainToken).isNotBlank();

        assertThat(savedToken.getId())
                .startsWith("evt_")
                .hasSize(36);

        assertThat(savedToken.getUser()).isSameAs(user);

        assertThat(savedToken.getTokenHash())
                .isEqualTo(tokenGenerator.hash(plainToken))
                .hasSize(64);

        assertThat(savedToken.getTokenHash())
                .isNotEqualTo(plainToken);

        assertThat(savedToken.getCreatedAt())
                .isEqualTo(NOW);

        assertThat(savedToken.getExpiresAt())
                .isEqualTo(NOW.plusSeconds(24 * 60 * 60));

        assertThat(savedToken.getUsedAt()).isNull();
    }

    @Test
    void shouldConfirmValidToken() {
        UserEntity user = createUser();
        String plainToken = "gueltiges-test-token";

        EmailVerificationTokenEntity token =
                createToken(
                        user,
                        plainToken,
                        NOW.plusSeconds(3600),
                        null
                );

        when(tokenRepository.findByTokenHash(
                tokenGenerator.hash(plainToken)
        )).thenReturn(Optional.of(token));

        service.confirmEmail(plainToken);

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(token.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectUnknownToken() {
        String plainToken = "unbekanntes-token";

        when(tokenRepository.findByTokenHash(
                tokenGenerator.hash(plainToken)
        )).thenReturn(Optional.empty());

        assertInvalidToken(
                () -> service.confirmEmail(plainToken)
        );
    }

    @Test
    void shouldRejectBlankToken() {
        assertInvalidToken(
                () -> service.confirmEmail("   ")
        );
    }

    @Test
    void shouldRejectExpiredToken() {
        UserEntity user = createUser();
        String plainToken = "abgelaufenes-token";

        EmailVerificationTokenEntity token =
                createToken(
                        user,
                        plainToken,
                        NOW.minusSeconds(1),
                        null
                );

        when(tokenRepository.findByTokenHash(
                tokenGenerator.hash(plainToken)
        )).thenReturn(Optional.of(token));

        assertInvalidToken(
                () -> service.confirmEmail(plainToken)
        );

        assertThat(user.isEmailVerified()).isFalse();
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void shouldRejectTokenExpiringExactlyNow() {
        UserEntity user = createUser();
        String plainToken = "jetzt-abgelaufen";

        EmailVerificationTokenEntity token =
                createToken(
                        user,
                        plainToken,
                        NOW,
                        null
                );

        when(tokenRepository.findByTokenHash(
                tokenGenerator.hash(plainToken)
        )).thenReturn(Optional.of(token));

        assertInvalidToken(
                () -> service.confirmEmail(plainToken)
        );
    }

    @Test
    void shouldRejectAlreadyUsedToken() {
        UserEntity user = createUser();
        String plainToken = "verwendetes-token";

        EmailVerificationTokenEntity token =
                createToken(
                        user,
                        plainToken,
                        NOW.plusSeconds(3600),
                        NOW.minusSeconds(60)
                );

        when(tokenRepository.findByTokenHash(
                tokenGenerator.hash(plainToken)
        )).thenReturn(Optional.of(token));

        assertInvalidToken(
                () -> service.confirmEmail(plainToken)
        );

        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void shouldRejectTokenForAlreadyVerifiedUser() {
        UserEntity user = createUser();
        user.markEmailAsVerified();

        String plainToken =
                "token-fuer-verifizierten-benutzer";

        EmailVerificationTokenEntity token =
                createToken(
                        user,
                        plainToken,
                        NOW.plusSeconds(3600),
                        null
                );

        when(tokenRepository.findByTokenHash(
                tokenGenerator.hash(plainToken)
        )).thenReturn(Optional.of(token));

        assertInvalidToken(
                () -> service.confirmEmail(plainToken)
        );

        assertThat(token.getUsedAt()).isNull();
    }

    private UserEntity createUser() {
        return new UserEntity(
                "usr_1234567890",
                "Test User",
                "test@example.com",
                "password-hash"
        );
    }

    private EmailVerificationTokenEntity createToken(
            UserEntity user,
            String plainToken,
            Instant expiresAt,
            Instant usedAt
    ) {
        EmailVerificationTokenEntity token =
                new EmailVerificationTokenEntity(
                        "evt_12345678901234567890123456789012",
                        user,
                        tokenGenerator.hash(plainToken),
                        expiresAt,
                        NOW.minusSeconds(3600)
                );

        if (usedAt != null) {
            token.markAsUsed(usedAt);
        }

        return token;
    }

    private void assertInvalidToken(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(
                        InvalidEmailVerificationTokenException.class
                )
                .hasMessage(
                        "Der E-Mail-Verifikationslink ist ungültig oder abgelaufen"
                );
    }
}