package de.warp.identity.service;

import de.warp.identity.dto.RegistrationRequest;
import de.warp.identity.dto.RegistrationResponse;
import de.warp.identity.entity.UserEntity;
import de.warp.identity.exceptions.EmailAlreadyExistsException;
import de.warp.identity.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class RegistrationService {

    private static final String SUCCESS_MESSAGE =
            "Das Benutzerkonto wurde erstellt. Eine Bestätigungs-E-Mail wurde versendet.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        String normalizedName = request.name().trim();
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.password());

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
            throw new EmailAlreadyExistsException();
        }

        return new RegistrationResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.isEmailVerified(),
                savedUser.getCreatedAt(),
                SUCCESS_MESSAGE
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateUserId() {
        return "usr_" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }
}
