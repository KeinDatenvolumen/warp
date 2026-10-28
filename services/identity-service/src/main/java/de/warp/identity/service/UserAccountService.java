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
import de.warp.identity.security.PasswordRequirements;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UserAccountService {

    private static final String EMAIL_UNIQUE_INDEX =
            "uk_users_email_normalized";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final VerificationEmailSender verificationEmailSender;

    public UserAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            VerificationEmailSender verificationEmailSender
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.verificationEmailSender = verificationEmailSender;
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(String userId) {
        UserEntity user = findUser(userId);

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }

    @Transactional
    public void changePassword(
            String userId,
            ChangePasswordRequest request
    ) {
        UserEntity user = findUser(userId);

        requireCorrectPassword(user, request.currentPassword());

        String newPassword = request.newPassword();

        if (!PasswordRequirements.areMetBy(newPassword)) {
            throw new PasswordRequirementsNotMetException();
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new InvalidAccountChangeException(
                    "newPassword",
                    "PASSWORD_UNCHANGED",
                    "Das neue Passwort muss sich vom aktuellen "
                            + "Passwort unterscheiden."
            );
        }

        user.changePasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void changeEmail(
            String userId,
            ChangeEmailRequest request
    ) {
        UserEntity user = findUser(userId);

        requireCorrectPassword(user, request.password());

        String newEmail = request.newEmail() == null
                ? ""
                : request.newEmail().trim().toLowerCase(Locale.ROOT);

        if (!EMAIL_PATTERN.matcher(newEmail).matches()) {
            throw new InvalidAccountChangeException(
                    "newEmail",
                    "INVALID_EMAIL",
                    "Die E-Mail-Adresse ist ungültig."
            );
        }

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new InvalidAccountChangeException(
                    "newEmail",
                    "EMAIL_UNCHANGED",
                    "Die neue E-Mail-Adresse muss sich von der "
                            + "aktuellen unterscheiden."
            );
        }

        if (userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new EmailAlreadyExistsException();
        }

        user.changeEmail(newEmail);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueIndexViolation(exception)) {
                throw new EmailAlreadyExistsException();
            }

            throw exception;
        }

        String plainToken =
                emailVerificationService.createVerificationToken(user);

        verificationEmailSender.sendVerificationEmail(user, plainToken);
    }

    private UserEntity findUser(String userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    private void requireCorrectPassword(
            UserEntity user,
            String password
    ) {
        if (password == null
                || !passwordEncoder.matches(
                password,
                user.getPasswordHash()
        )) {
            throw new IncorrectPasswordException();
        }
    }

    private boolean isEmailUniqueIndexViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return EMAIL_UNIQUE_INDEX.equalsIgnoreCase(
                        violation.getConstraintName()
                );
            }

            cause = cause.getCause();
        }

        return false;
    }
}