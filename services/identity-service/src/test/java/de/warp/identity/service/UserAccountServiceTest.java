// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Wetter- & Routen-Plattform
package de.warp.identity.service;

import de.warp.identity.dto.ChangeEmailRequest;
import de.warp.identity.dto.ChangePasswordRequest;
import de.warp.identity.dto.UserResponse;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.EmailAlreadyExistsException;
import de.warp.identity.exceptions.IncorrectPasswordException;
import de.warp.identity.exceptions.InvalidAccountChangeException;
import de.warp.identity.exceptions.PasswordRequirementsNotMetException;
import de.warp.identity.exceptions.UserNotFoundException;
import de.warp.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    private static final String CURRENT_PASSWORD = "current-password-123";
    private static final String NEW_PASSWORD = "new-password-12345";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private VerificationEmailSender verificationEmailSender;

    private UserAccountService service;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(
                userRepository,
                passwordEncoder,
                emailVerificationService,
                verificationEmailSender
        );

        user = new UserEntity(
                "usr_1",
                "Max Mustermann",
                "max@example.de",
                "old-hash"
        );
    }

    @Test
    void shouldReturnUser() {
        when(userRepository.findById("usr_1"))
                .thenReturn(Optional.of(user));

        UserResponse response = service.getUser("usr_1");

        assertThat(response.id()).isEqualTo("usr_1");
        assertThat(response.name()).isEqualTo("Max Mustermann");
        assertThat(response.email()).isEqualTo("max@example.de");
        assertThat(response.emailVerified()).isFalse();
    }

    @Test
    void shouldFailForUnknownUser() {
        when(userRepository.findById("usr_x"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser("usr_x"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldChangePassword() {
        when(userRepository.findById("usr_1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "old-hash"))
                .thenReturn(true);
        when(passwordEncoder.matches(NEW_PASSWORD, "old-hash"))
                .thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenReturn("new-hash");

        service.changePassword(
                "usr_1",
                new ChangePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD)
        );

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void shouldRejectWrongCurrentPassword() {
        when(userRepository.findById("usr_1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(
                "usr_1",
                new ChangePasswordRequest("wrong", NEW_PASSWORD)
        )).isInstanceOf(IncorrectPasswordException.class);

        verify(passwordEncoder, never()).encode(any());
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
    }

    @Test
    void shouldRejectTooShortNewPassword() {
        when(userRepository.findById("usr_1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "old-hash"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(
                "usr_1",
                new ChangePasswordRequest(CURRENT_PASSWORD, "short")
        )).isInstanceOf(PasswordRequirementsNotMetException.class);
    }

    @Test
    void shouldRejectUnchangedPassword() {
        when(userRepository.findById("usr_1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "old-hash"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(
                "usr_1",
                new ChangePasswordRequest(
                        CURRENT_PASSWORD,
                        CURRENT_PASSWORD
                )
        )).isInstanceOfSatisfying(
                InvalidAccountChangeException.class,
                exception -> assertThat(exception.getCode())
                        .isEqualTo("PASSWORD_UNCHANGED")
        );
    }

    @Test
    void shouldChangeEmailResetVerificationAndSendMail() {
        user.markEmailAsVerified();

        when(userRepository.findById("usr_1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "old-hash"))
                .thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("neu@example.de"))
                .thenReturn(false);
        when(emailVerificationService.createVerificationToken(user))
                .thenReturn("plain-token");

        service.changeEmail(
                "usr_1",
                new ChangeEmailRequest("  Neu@Example.de ", CURRENT_PASSWORD)
        );

        assertThat(user.getEmail()).isEqualTo("neu@example.de");
        assertThat(user.isEmailVerified()).isFalse();
        verify(userRepository).saveAndFlush(user);
        verify(verificationEmailSender)
                .sendVerificationEmail(user, "plain-token");
    }

    @Test
    void shouldRejectEmailOfAnotherUser() {
        when(userRepository.findById("usr_1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "old-hash"))
                .thenReturn(true);
        when(userRepository.existsByEmailIgnoreCase("neu@example.de"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.changeEmail(
                "usr_1",
                new ChangeEmailRequest("neu@example.de", CURRENT_PASSWORD)
        )).isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).saveAndFlush(any());
        assertThat(user.getEmail()).isEqualTo("max@example.de");
    }

    @Test
    void shouldRejectInvalidEmail() {
        when(userRepository.findById("usr_1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "old-hash"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.changeEmail(
                "usr_1",
                new ChangeEmailRequest("keine-mail", CURRENT_PASSWORD)
        )).isInstanceOfSatisfying(
                InvalidAccountChangeException.class,
                exception -> assertThat(exception.getCode())
                        .isEqualTo("INVALID_EMAIL")
        );
    }

    @Test
    void shouldRejectUnchangedEmailIgnoringCase() {
        when(userRepository.findById("usr_1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CURRENT_PASSWORD, "old-hash"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.changeEmail(
                "usr_1",
                new ChangeEmailRequest("MAX@example.de", CURRENT_PASSWORD)
        )).isInstanceOfSatisfying(
                InvalidAccountChangeException.class,
                exception -> assertThat(exception.getCode())
                        .isEqualTo("EMAIL_UNCHANGED")
        );
    }

    @Test
    void shouldRejectEmailChangeWithWrongPassword() {
        when(userRepository.findById("usr_1"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.changeEmail(
                "usr_1",
                new ChangeEmailRequest("neu@example.de", "wrong")
        )).isInstanceOf(IncorrectPasswordException.class);

        verify(userRepository, never()).saveAndFlush(any());
        verify(verificationEmailSender, never())
                .sendVerificationEmail(any(), any());
    }
}