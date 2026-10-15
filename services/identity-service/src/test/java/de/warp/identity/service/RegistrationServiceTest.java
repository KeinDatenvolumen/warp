package de.warp.identity.service;

import de.warp.identity.dto.RegistrationRequest;
import de.warp.identity.dto.RegistrationResponse;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;

import de.warp.identity.exceptions.EmailAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void shouldRegisterUserWithNormalizedDataAndHashedPassword() {
        RegistrationRequest request = new RegistrationRequest(
                "  Max Mustermann  ",
                "  Max@Example.de  ",
                "MeinSicheresPasswort123"
        );

        when(userRepository.existsByEmailIgnoreCase("max@example.de"))
                .thenReturn(false);

        when(passwordEncoder.encode("MeinSicheresPasswort123"))
                .thenReturn("hashed-password");

        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        assertEquals("Max Mustermann", savedUser.getName());
        assertEquals("max@example.de", savedUser.getEmail());
        assertEquals("hashed-password", savedUser.getPasswordHash());
        assertNotEquals(
                "MeinSicheresPasswort123",
                savedUser.getPasswordHash()
        );
        assertFalse(savedUser.isEmailVerified());

        assertEquals(savedUser.getId(), response.id());
        assertEquals("Max Mustermann", response.name());
        assertEquals("max@example.de", response.email());
        assertFalse(response.emailVerified());
        assertEquals(
                "Das Benutzerkonto wurde erstellt. Eine Bestätigungs-E-Mail wurde versendet.",
                response.message()
        );
    }

    @Test
    void shouldRejectAlreadyRegisteredEmail() {
        RegistrationRequest request = new RegistrationRequest(
                "Max Mustermann",
                "  MAX@EXAMPLE.DE  ",
                "MeinSicheresPasswort123"
        );

        when(userRepository.existsByEmailIgnoreCase("max@example.de"))
                .thenReturn(true);

        assertThrows(
                de.warp.identity.exceptions.EmailAlreadyExistsException.class,
                () -> registrationService.register(request)
        );

        verify(userRepository)
                .existsByEmailIgnoreCase("max@example.de");

        verify(passwordEncoder, never())
                .encode(any());

        verify(userRepository, never())
                .saveAndFlush(any(UserEntity.class));
    }

    @Test
    void shouldRejectEmailWhenDatabaseUniqueConstraintIsViolated() {
        RegistrationRequest request = new RegistrationRequest(
                "Max Mustermann",
                "max@example.de",
                "MeinSicheresPasswort123"
        );

        when(userRepository.existsByEmailIgnoreCase("max@example.de"))
                .thenReturn(false);

        when(passwordEncoder.encode("MeinSicheresPasswort123"))
                .thenReturn("hashed-password");

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

        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenThrow(databaseException);

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
    }
}