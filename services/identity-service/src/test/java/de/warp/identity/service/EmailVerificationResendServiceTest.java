// [KI-Log Nr. KI-11] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): E-Mail-Verifikations-Flow (WARP-007)
package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationResendServiceTest {

    private UserRepository userRepository;
    private EmailVerificationService emailVerificationService;
    private VerificationEmailSender verificationEmailSender;
    private EmailVerificationResendService resendService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);

        emailVerificationService = mock(
                EmailVerificationService.class
        );

        verificationEmailSender = mock(
                VerificationEmailSender.class
        );

        resendService = new EmailVerificationResendService(
                userRepository,
                emailVerificationService,
                verificationEmailSender
        );
    }

    @Test
    void shouldSendNewVerificationEmailForUnverifiedUser() {
        UserEntity user = createUser();

        when(userRepository.findByEmailIgnoreCase(
                "max@example.de"
        )).thenReturn(Optional.of(user));

        when(emailVerificationService.createVerificationToken(
                user
        )).thenReturn("neues-verifikationstoken");

        resendService.resendVerificationEmail(
                "  MAX@EXAMPLE.DE  "
        );

        verify(userRepository).findByEmailIgnoreCase(
                "max@example.de"
        );

        verify(emailVerificationService)
                .createVerificationToken(user);

        verify(verificationEmailSender)
                .sendVerificationEmail(
                        user,
                        "neues-verifikationstoken"
                );
    }

    @Test
    void shouldDoNothingForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase(
                "unbekannt@example.de"
        )).thenReturn(Optional.empty());

        resendService.resendVerificationEmail(
                "unbekannt@example.de"
        );

        verify(userRepository).findByEmailIgnoreCase(
                "unbekannt@example.de"
        );

        verify(emailVerificationService, never())
                .createVerificationToken(
                        org.mockito.ArgumentMatchers.any()
                );

        verify(verificationEmailSender, never())
                .sendVerificationEmail(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyString()
                );
    }

    @Test
    void shouldDoNothingForAlreadyVerifiedUser() {
        UserEntity user = createUser();
        user.markEmailAsVerified();

        when(userRepository.findByEmailIgnoreCase(
                "max@example.de"
        )).thenReturn(Optional.of(user));

        resendService.resendVerificationEmail(
                "max@example.de"
        );

        verify(emailVerificationService, never())
                .createVerificationToken(
                        org.mockito.ArgumentMatchers.any()
                );

        verify(verificationEmailSender, never())
                .sendVerificationEmail(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyString()
                );
    }

    @Test
    void shouldDoNothingForNullEmail() {
        resendService.resendVerificationEmail(null);

        verify(userRepository, never())
                .findByEmailIgnoreCase(
                        org.mockito.ArgumentMatchers.anyString()
                );

        verify(emailVerificationService, never())
                .createVerificationToken(
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void shouldDoNothingForBlankEmail() {
        resendService.resendVerificationEmail("   ");

        verify(userRepository, never())
                .findByEmailIgnoreCase(
                        org.mockito.ArgumentMatchers.anyString()
                );

        verify(emailVerificationService, never())
                .createVerificationToken(
                        org.mockito.ArgumentMatchers.any()
                );

        verify(verificationEmailSender, never())
                .sendVerificationEmail(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyString()
                );
    }

    private UserEntity createUser() {
        return new UserEntity(
                "usr_1234567890",
                "Max Mustermann",
                "max@example.de",
                "password-hash"
        );
    }
}