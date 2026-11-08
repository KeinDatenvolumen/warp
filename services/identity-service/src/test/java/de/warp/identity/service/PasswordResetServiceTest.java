// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.service;

import de.warp.identity.entity.PasswordResetTokenEntity;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.InvalidPasswordResetTokenException;
import de.warp.identity.exceptions.PasswordRequirementsNotMetException;
import de.warp.identity.repository.PasswordResetTokenRepository;
import de.warp.identity.security.PasswordResetTokenGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-10-24T08:00:00Z");

    private static final String PLAIN_TOKEN =
            "plain-password-reset-token";

    private static final String TOKEN_HASH =
            "0123456789abcdef0123456789abcdef"
                    + "0123456789abcdef0123456789abcdef";

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordResetTokenGenerator tokenGenerator;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                NOW,
                ZoneOffset.UTC
        );

        passwordResetService = new PasswordResetService(
                tokenRepository,
                tokenGenerator,
                passwordEncoder,
                clock
        );
    }

    @Test
    void shouldCreatePasswordResetToken() {
        UserEntity user = createUser();

        PasswordResetTokenGenerator.GeneratedToken generatedToken =
                new PasswordResetTokenGenerator.GeneratedToken(
                        PLAIN_TOKEN,
                        TOKEN_HASH
                );

        when(tokenGenerator.generate())
                .thenReturn(generatedToken);

        String result =
                passwordResetService.createPasswordResetToken(user);

        assertThat(result).isEqualTo(PLAIN_TOKEN);

        verify(tokenRepository).markAllUnusedTokensAsUsed(
                user.getId(),
                NOW
        );

        ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor =
                ArgumentCaptor.forClass(
                        PasswordResetTokenEntity.class
                );

        verify(tokenRepository).save(tokenCaptor.capture());

        PasswordResetTokenEntity savedToken =
                tokenCaptor.getValue();

        assertThat(savedToken.getId())
                .startsWith("prt_");

        assertThat(savedToken.getUser())
                .isSameAs(user);

        assertThat(savedToken.getTokenHash())
                .isEqualTo(TOKEN_HASH);

        assertThat(savedToken.getCreatedAt())
                .isEqualTo(NOW);

        assertThat(savedToken.getExpiresAt())
                .isEqualTo(
                        NOW.plusSeconds(30 * 60)
                );

        assertThat(savedToken.isUsed()).isFalse();
    }

    @Test
    void shouldResetPasswordWithValidToken() {
        UserEntity user = createUser();

        PasswordResetTokenEntity token =
                createValidToken(user);

        when(tokenGenerator.hash(PLAIN_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(tokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(token));

        when(passwordEncoder.encode(
                "ein-neues-passwort"
        )).thenReturn("new-password-hash");

        passwordResetService.resetPassword(
                PLAIN_TOKEN,
                "ein-neues-passwort"
        );

        assertThat(user.getPasswordHash())
                .isEqualTo("new-password-hash");

        assertThat(token.isUsed()).isTrue();

        assertThat(token.getUsedAt())
                .isEqualTo(NOW);

        verify(passwordEncoder).encode(
                "ein-neues-passwort"
        );

        verify(tokenRepository).markAllUnusedTokensAsUsed(
                user.getId(),
                NOW
        );
    }

    @Test
    void shouldRejectUnknownToken() {
        when(tokenGenerator.hash(PLAIN_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(tokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> passwordResetService.resetPassword(
                        PLAIN_TOKEN,
                        "ein-neues-passwort"
                )
        )
                .isInstanceOf(
                        InvalidPasswordResetTokenException.class
                );

        verify(passwordEncoder, never())
                .encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectExpiredToken() {
        UserEntity user = createUser();

        PasswordResetTokenEntity token =
                new PasswordResetTokenEntity(
                        "prt_0123456789abcdef0123456789abcdef",
                        user,
                        TOKEN_HASH,
                        NOW,
                        NOW.minusSeconds(30 * 60)
                );

        when(tokenGenerator.hash(PLAIN_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(tokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(
                () -> passwordResetService.resetPassword(
                        PLAIN_TOKEN,
                        "ein-neues-passwort"
                )
        )
                .isInstanceOf(
                        InvalidPasswordResetTokenException.class
                );

        assertThat(token.isUsed()).isFalse();

        assertThat(user.getPasswordHash())
                .isEqualTo("old-password-hash");

        verify(passwordEncoder, never())
                .encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectUsedToken() {
        UserEntity user = createUser();

        PasswordResetTokenEntity token =
                createValidToken(user);

        token.markAsUsed(
                NOW.minusSeconds(60)
        );

        when(tokenGenerator.hash(PLAIN_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(tokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(
                () -> passwordResetService.resetPassword(
                        PLAIN_TOKEN,
                        "ein-neues-passwort"
                )
        )
                .isInstanceOf(
                        InvalidPasswordResetTokenException.class
                );

        assertThat(user.getPasswordHash())
                .isEqualTo("old-password-hash");

        verify(passwordEncoder, never())
                .encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectPasswordThatDoesNotMeetRequirements() {
        UserEntity user = createUser();

        PasswordResetTokenEntity token =
                createValidToken(user);

        when(tokenGenerator.hash(PLAIN_TOKEN))
                .thenReturn(TOKEN_HASH);

        when(tokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(
                () -> passwordResetService.resetPassword(
                        PLAIN_TOKEN,
                        "zu-kurz"
                )
        )
                .isInstanceOf(
                        PasswordRequirementsNotMetException.class
                );

        assertThat(token.isUsed()).isFalse();

        assertThat(user.getPasswordHash())
                .isEqualTo("old-password-hash");

        verify(passwordEncoder, never())
                .encode(org.mockito.ArgumentMatchers.anyString());

        verify(
                tokenRepository,
                never()
        ).markAllUnusedTokensAsUsed(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void shouldRejectBlankToken() {
        assertThatThrownBy(
                () -> passwordResetService.resetPassword(
                        "   ",
                        "ein-neues-passwort"
                )
        )
                .isInstanceOf(
                        InvalidPasswordResetTokenException.class
                );

        verify(tokenGenerator, never())
                .hash(org.mockito.ArgumentMatchers.anyString());

        verify(passwordEncoder, never())
                .encode(org.mockito.ArgumentMatchers.anyString());
    }

    private UserEntity createUser() {
        return new UserEntity(
                "usr_0123456789abcdef0123456789abcdef",
                "Test User",
                "test@example.com",
                "old-password-hash"
        );
    }

    private PasswordResetTokenEntity createValidToken(
            UserEntity user
    ) {
        return new PasswordResetTokenEntity(
                "prt_0123456789abcdef0123456789abcdef",
                user,
                TOKEN_HASH,
                NOW.plusSeconds(30 * 60),
                NOW.minusSeconds(60)
        );
    }
}