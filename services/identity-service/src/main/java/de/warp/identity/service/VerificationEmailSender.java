package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.messaging.EmailRequestedEvent;
import de.warp.identity.messaging.MailType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Fordert den Versand der Verifikations-E-Mail an. Die Zustellung erfolgt
 * entkoppelt nach dem Commit (siehe QueueMailEventRelay und
 * VerificationMailDelivery).
 */
@Service
public class VerificationEmailSender {

    private final ApplicationEventPublisher eventPublisher;

    public VerificationEmailSender(
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = Objects.requireNonNull(
                eventPublisher,
                "eventPublisher darf nicht null sein"
        );
    }

    public void sendVerificationEmail(
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
                        MailType.VERIFICATION,
                        user.getId(),
                        plainToken
                )
        );
    }
}