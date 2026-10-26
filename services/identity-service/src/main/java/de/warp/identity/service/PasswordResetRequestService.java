package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class PasswordResetRequestService {

    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final PasswordResetEmailSender passwordResetEmailSender;

    public PasswordResetRequestService(
            UserRepository userRepository,
            PasswordResetService passwordResetService,
            PasswordResetEmailSender passwordResetEmailSender
    ) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository darf nicht null sein"
        );
        this.passwordResetService = Objects.requireNonNull(
                passwordResetService,
                "passwordResetService darf nicht null sein"
        );
        this.passwordResetEmailSender = Objects.requireNonNull(
                passwordResetEmailSender,
                "passwordResetEmailSender darf nicht null sein"
        );
    }

    @Transactional
    public void requestPasswordReset(String email) {
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

        String plainToken =
                passwordResetService
                        .createPasswordResetToken(user);

        passwordResetEmailSender.sendPasswordResetEmail(
                user,
                plainToken
        );
    }
}