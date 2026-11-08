// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetRequestServiceTest {

    private UserRepository userRepository;
    private PasswordResetService passwordResetService;
    private PasswordResetEmailSender passwordResetEmailSender;
    private PasswordResetRequestService requestService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);

        passwordResetService = mock(
                PasswordResetService.class
        );

        passwordResetEmailSender = mock(
                PasswordResetEmailSender.class
        );

        requestService = new PasswordResetRequestService(
                userRepository,
                passwordResetService,
                passwordResetEmailSender
        );
    }

    @Test
    void shouldSendPasswordResetEmailForKnownUser() {
        UserEntity user = createUser();

        when(userRepository.findByEmailIgnoreCase(
                "max@example.de"
        )).thenReturn(Optional.of(user));

        when(passwordResetService.createPasswordResetToken(
                user
        )).thenReturn("neues-passwort-reset-token");

        requestService.requestPasswordReset(
                "  MAX@EXAMPLE.DE  "
        );

        verify(userRepository).findByEmailIgnoreCase(
                "max@example.de"
        );

        verify(passwordResetService)
                .createPasswordResetToken(user);

        verify(passwordResetEmailSender)
                .sendPasswordResetEmail(
                        user,
                        "neues-passwort-reset-token"
                );
    }

    @Test
    void shouldDoNothingForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase(
                "unbekannt@example.de"
        )).thenReturn(Optional.empty());

        requestService.requestPasswordReset(
                "unbekannt@example.de"
        );

        verify(userRepository).findByEmailIgnoreCase(
                "unbekannt@example.de"
        );

        verify(passwordResetService, never())
                .createPasswordResetToken(any());

        verify(passwordResetEmailSender, never())
                .sendPasswordResetEmail(
                        any(),
                        anyString()
                );
    }

    @Test
    void shouldDoNothingForNullEmail() {
        requestService.requestPasswordReset(null);

        verify(userRepository, never())
                .findByEmailIgnoreCase(anyString());

        verify(passwordResetService, never())
                .createPasswordResetToken(any());

        verify(passwordResetEmailSender, never())
                .sendPasswordResetEmail(
                        any(),
                        anyString()
                );
    }

    @Test
    void shouldDoNothingForBlankEmail() {
        requestService.requestPasswordReset("   ");

        verify(userRepository, never())
                .findByEmailIgnoreCase(anyString());

        verify(passwordResetService, never())
                .createPasswordResetToken(any());

        verify(passwordResetEmailSender, never())
                .sendPasswordResetEmail(
                        any(),
                        anyString()
                );
    }

    private UserEntity createUser() {
        return new UserEntity(
                "usr_1234567890",
                "Max Mustermann",
                "max@example.de",
                "alter-password-hash"
        );
    }
}