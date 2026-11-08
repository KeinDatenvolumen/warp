// [KI-Log Nr. KI-051] (cfd2cd9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/warp-005: implement user registration with Tests
// [KI-Log Nr. KI-068] (a7d6e5a) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/WARP-005: implement user registration
// [KI-Log Nr. KI-071] (7a94bc0) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: use new api contract (WARP-005)
// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.service;

import de.warp.identity.dto.RegistrationRequest;
import de.warp.identity.dto.RegistrationResponse;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.EmailAlreadyExistsException;
import de.warp.identity.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private VerificationEmailSender verificationEmailSender;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                userRepository,
                passwordEncoder,
                emailVerificationService,
                verificationEmailSender
        );
    }

    @Test
    void shouldRegisterUserAndSendVerificationEmail() {
        RegistrationRequest request = new RegistrationRequest(
                "  Max Mustermann  ",
                "  Max@Example.de  ",
                "MeinSicheresPasswort123"
        );

        when(userRepository.existsByEmailIgnoreCase(
                "max@example.de"
        )).thenReturn(false);

        when(passwordEncoder.encode(
                "MeinSicheresPasswort123"
        )).thenReturn("hashed-password");

        when(userRepository.saveAndFlush(
                any(UserEntity.class)
        )).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        when(emailVerificationService.createVerificationToken(
                any(UserEntity.class)
        )).thenReturn("plain-verification-token");

        RegistrationResponse response =
                registrationService.register(request);

        ArgumentCaptor<UserEntity> userCaptor =
                ArgumentCaptor.forClass(UserEntity.class);

        verify(userRepository)
                .existsByEmailIgnoreCase("max@example.de");

        verify(passwordEncoder)
                .encode("MeinSicheresPasswort123");

        verify(userRepository)
                .saveAndFlush(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();

        verify(emailVerificationService)
                .createVerificationToken(savedUser);

        verify(verificationEmailSender)
                .sendVerificationEmail(
                        savedUser,
                        "plain-verification-token"
                );

        assertEquals(
                "Max Mustermann",
                savedUser.getName()
        );
        assertEquals(
                "max@example.de",
                savedUser.getEmail()
        );
        assertEquals(
                "hashed-password",
                savedUser.getPasswordHash()
        );
        assertNotEquals(
                "MeinSicheresPasswort123",
                savedUser.getPasswordHash()
        );
        assertFalse(savedUser.isEmailVerified());

        assertEquals(savedUser.getId(), response.id());
        assertEquals(
                "Max Mustermann",
                response.name()
        );
        assertEquals(
                "max@example.de",
                response.email()
        );
        assertFalse(response.emailVerified());
    }

    @Test
    void shouldCreateTokenBeforeSendingEmail() {
        RegistrationRequest request = new RegistrationRequest(
                "Max Mustermann",
                "max@example.de",
                "MeinSicheresPasswort123"
        );

        when(userRepository.existsByEmailIgnoreCase(
                "max@example.de"
        )).thenReturn(false);

        when(passwordEncoder.encode(
                "MeinSicheresPasswort123"
        )).thenReturn("hashed-password");

        when(userRepository.saveAndFlush(
                any(UserEntity.class)
        )).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        when(emailVerificationService.createVerificationToken(
                any(UserEntity.class)
        )).thenReturn("plain-verification-token");

        registrationService.register(request);

        InOrder order = inOrder(
                userRepository,
                emailVerificationService,
                verificationEmailSender
        );

        order.verify(userRepository)
                .saveAndFlush(any(UserEntity.class));

        order.verify(emailVerificationService)
                .createVerificationToken(
                        any(UserEntity.class)
                );

        order.verify(verificationEmailSender)
                .sendVerificationEmail(
                        any(UserEntity.class),
                        any(String.class)
                );
    }

    @Test
    void shouldRejectAlreadyRegisteredEmail() {
        RegistrationRequest request = new RegistrationRequest(
                "Max Mustermann",
                "  MAX@EXAMPLE.DE  ",
                "MeinSicheresPasswort123"
        );

        when(userRepository.existsByEmailIgnoreCase(
                "max@example.de"
        )).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> registrationService.register(request)
        );

        verify(userRepository)
                .existsByEmailIgnoreCase("max@example.de");

        verify(passwordEncoder, never()).encode(any());

        verify(userRepository, never())
                .saveAndFlush(any(UserEntity.class));

        verify(emailVerificationService, never())
                .createVerificationToken(any(UserEntity.class));

        verify(verificationEmailSender, never())
                .sendVerificationEmail(
                        any(UserEntity.class),
                        any(String.class)
                );
    }

    @Test
    void shouldRejectEmailWhenDatabaseUniqueConstraintIsViolated() {
        RegistrationRequest request = new RegistrationRequest(
                "Max Mustermann",
                "max@example.de",
                "MeinSicheresPasswort123"
        );

        when(userRepository.existsByEmailIgnoreCase(
                "max@example.de"
        )).thenReturn(false);

        when(passwordEncoder.encode(
                "MeinSicheresPasswort123"
        )).thenReturn("hashed-password");

        SQLException sqlException = new SQLException(
                "duplicate key value violates unique constraint",
                "23505"
        );

        ConstraintViolationException constraintViolationException =
                new ConstraintViolationException(
                        "Unique index violation",
                        sqlException,
                        "uk_users_email_normalized"
                );

        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException(
                        "Database constraint violation",
                        constraintViolationException
                );

        when(userRepository.saveAndFlush(
                any(UserEntity.class)
        )).thenThrow(databaseException);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> registrationService.register(request)
        );

        verify(userRepository)
                .existsByEmailIgnoreCase("max@example.de");

        verify(passwordEncoder)
                .encode("MeinSicheresPasswort123");

        verify(userRepository)
                .saveAndFlush(any(UserEntity.class));

        verify(emailVerificationService, never())
                .createVerificationToken(any(UserEntity.class));

        verify(verificationEmailSender, never())
                .sendVerificationEmail(
                        any(UserEntity.class),
                        any(String.class)
                );
    }

    @Test
    void shouldPropagateUnexpectedDatabaseError() {
        RegistrationRequest request = new RegistrationRequest(
                "Max Mustermann",
                "max@example.de",
                "MeinSicheresPasswort123"
        );

        when(userRepository.existsByEmailIgnoreCase(
                "max@example.de"
        )).thenReturn(false);

        when(passwordEncoder.encode(
                "MeinSicheresPasswort123"
        )).thenReturn("hashed-password");

        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException(
                        "Unexpected database error"
                );

        when(userRepository.saveAndFlush(
                any(UserEntity.class)
        )).thenThrow(databaseException);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> registrationService.register(request)
        );

        verify(emailVerificationService, never())
                .createVerificationToken(any(UserEntity.class));

        verify(verificationEmailSender, never())
                .sendVerificationEmail(
                        any(UserEntity.class),
                        any(String.class)
                );
    }
}