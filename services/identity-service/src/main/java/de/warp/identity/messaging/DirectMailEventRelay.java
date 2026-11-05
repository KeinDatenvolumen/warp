// [KI-Log Nr. KI-21] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone RabbitMQ Mail-Queue & Broker-Fallback (WARP-036)
package de.warp.identity.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Aktiv, wenn warp.messaging.enabled=false (Tests, Betrieb ohne Broker):
 * stellt Mail-Aufträge nach dem Commit direkt per SMTP zu.
 */
@Component
@ConditionalOnProperty(
        name = "warp.messaging.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DirectMailEventRelay {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DirectMailEventRelay.class);

    private final MailDeliveryService mailDeliveryService;

    public DirectMailEventRelay(MailDeliveryService mailDeliveryService) {
        this.mailDeliveryService = mailDeliveryService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void onEmailRequested(EmailRequestedEvent event) {
        try {
            mailDeliveryService.deliver(event);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Mail-Auftrag {} ({}) konnte nicht zugestellt werden.",
                    event.eventId(),
                    event.type(),
                    exception
            );
        }
    }
}