package de.warp.identity.messaging;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.UserRepository;
import de.warp.identity.service.PasswordResetMailDelivery;
import de.warp.identity.service.VerificationMailDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Führt einen Mail-Auftrag aus. Wird vom Queue-Konsumenten, vom direkten
 * Relay und vom Fallback des Queue-Relays verwendet.
 */
@Service
public class MailDeliveryService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MailDeliveryService.class);

    private final UserRepository userRepository;
    private final VerificationMailDelivery verificationMailDelivery;
    private final PasswordResetMailDelivery passwordResetMailDelivery;

    public MailDeliveryService(
            UserRepository userRepository,
            VerificationMailDelivery verificationMailDelivery,
            PasswordResetMailDelivery passwordResetMailDelivery
    ) {
        this.userRepository = userRepository;
        this.verificationMailDelivery = verificationMailDelivery;
        this.passwordResetMailDelivery = passwordResetMailDelivery;
    }

    public void deliver(EmailRequestedEvent event) {
        Optional<UserEntity> user =
                userRepository.findById(event.userId());

        if (user.isEmpty()) {
            LOGGER.warn(
                    "Mail-Auftrag {} verworfen: Benutzer existiert nicht mehr.",
                    event.eventId()
            );
            return;
        }

        switch (event.type()) {
            case VERIFICATION -> verificationMailDelivery
                    .sendVerificationEmail(user.get(), event.token());
            case PASSWORD_RESET -> passwordResetMailDelivery
                    .sendPasswordResetEmail(user.get(), event.token());
        }
    }
}