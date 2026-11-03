package de.warp.identity.messaging;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Auftrag zum Versand einer E-Mail. Enthält bewusst keine E-Mail-Adresse:
 * der Konsument lädt den Benutzer anhand der userId.
 */
public record EmailRequestedEvent(
        String eventId,
        MailType type,
        String userId,
        String token,
        Instant occurredAt
) {

    public EmailRequestedEvent {
        Objects.requireNonNull(eventId, "eventId darf nicht null sein");
        Objects.requireNonNull(type, "type darf nicht null sein");
        Objects.requireNonNull(userId, "userId darf nicht null sein");
        Objects.requireNonNull(token, "token darf nicht null sein");
        Objects.requireNonNull(occurredAt, "occurredAt darf nicht null sein");
    }

    public static EmailRequestedEvent create(
            MailType type,
            String userId,
            String token
    ) {
        return new EmailRequestedEvent(
                UUID.randomUUID().toString(),
                type,
                userId,
                token,
                Instant.now()
        );
    }

    /** Der Token darf nie in Logs landen. */
    @Override
    public String toString() {
        return "EmailRequestedEvent[eventId=%s, type=%s, userId=%s]"
                .formatted(eventId, type, userId);
    }
}