package de.warp.identity.service;

import de.warp.identity.dto.RegistrationRequest;
import de.warp.identity.dto.RegistrationResponse;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.EmailAlreadyExistsException;
import de.warp.identity.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegistrationService {

    private static final String EMAIL_UNIQUE_INDEX =
            "uk_users_email_normalized";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final VerificationEmailSender verificationEmailSender;

    public RegistrationService(
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

    @Transactional
    public RegistrationResponse register(
            RegistrationRequest request
    ) {
        String normalizedName = request.name().trim();
        String normalizedEmail = request.email();

        if (userRepository.existsByEmailIgnoreCase(
                normalizedEmail
        )) {
            throw new EmailAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(
                request.password()
        );

        UserEntity user = new UserEntity(
                generateUserId(),
                normalizedName,
                normalizedEmail,
                passwordHash
        );

        UserEntity savedUser;

        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueIndexViolation(exception)) {
                throw new EmailAlreadyExistsException();
            }

            throw exception;
        }

        String plainVerificationToken =
                emailVerificationService
                        .createVerificationToken(savedUser);

        verificationEmailSender.sendVerificationEmail(
                savedUser,
                plainVerificationToken
        );

        return new RegistrationResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.isEmailVerified(),
                savedUser.getCreatedAt()
        );
    }

    private String generateUserId() {
        return "usr_" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    private boolean isEmailUniqueIndexViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause
                    instanceof ConstraintViolationException
                    constraintException) {

                String constraintName =
                        constraintException.getConstraintName();

                return EMAIL_UNIQUE_INDEX.equalsIgnoreCase(
                        constraintName
                );
            }

            cause = cause.getCause();
        }

        return false;
    }
}