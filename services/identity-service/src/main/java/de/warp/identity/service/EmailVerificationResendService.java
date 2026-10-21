package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class EmailVerificationResendService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final VerificationEmailSender verificationEmailSender;

    public EmailVerificationResendService(
            UserRepository userRepository,
            EmailVerificationService emailVerificationService,
            VerificationEmailSender verificationEmailSender
    ) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository darf nicht null sein"
        );
        this.emailVerificationService = Objects.requireNonNull(
                emailVerificationService,
                "emailVerificationService darf nicht null sein"
        );
        this.verificationEmailSender = Objects.requireNonNull(
                verificationEmailSender,
                "verificationEmailSender darf nicht null sein"
        );
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail = email
                .trim()
                .toLowerCase(Locale.ROOT);

        Optional<UserEntity> optionalUser =
                userRepository.findByEmailIgnoreCase(
                        normalizedEmail
                );

        if (optionalUser.isEmpty()) {
            return;
        }

        UserEntity user = optionalUser.get();

        if (user.isEmailVerified()) {
            return;
        }

        String plainToken =
                emailVerificationService
                        .createVerificationToken(user);

        verificationEmailSender.sendVerificationEmail(
                user,
                plainToken
        );
    }
}