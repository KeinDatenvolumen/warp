package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.messaging.EmailRequestedEvent;
import de.warp.identity.messaging.MailType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Fordert den Versand der Passwort-Reset-E-Mail an. Die Zustellung erfolgt
 * entkoppelt nach dem Commit (siehe QueueMailEventRelay und
 * PasswordResetMailDelivery).
 */
@Service
public class PasswordResetEmailSender {

    private final ApplicationEventPublisher eventPublisher;

    public PasswordResetEmailSender(
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher,
                "eventPublisher darf nicht null sein"
        );
    }

    public void sendPasswordResetEmail(
            UserEntity user,
            String plainToken
    ) {
        Objects.requireNonNull(user, "user darf nicht null sein");

        if (plainToken == null || plainToken.isBlank()) {
            throw new IllegalArgumentException(
                    "plainToken darf nicht leer sein"
            );
        }

        eventPublisher.publishEvent(
                EmailRequestedEvent.create(
                        MailType.PASSWORD_RESET,
                        user.getId(),
                        plainToken
                )
        );
    }
}