// [KI-Log Nr. KI-051] (cfd2cd9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/warp-005: implement user registration with Tests
// [KI-Log Nr. KI-068] (a7d6e5a) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/WARP-005: implement user registration
// [KI-Log Nr. KI-071] (7a94bc0) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: use new api contract (WARP-005)
// [KI-Log Nr. KI-091] (fc545e3) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: complete success response and automated coverage (WARP-005)
// [KI-Log Nr. KI-094] (a3d7118) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: remove message field from registration response (WARP-005)
// [KI-Log Nr. KI-112] (88962a1) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: complete success response and automated coverage (WARP-005)
// [KI-Log Nr. KI-115] (434109e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: remove message field from registration response (WARP-005)
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