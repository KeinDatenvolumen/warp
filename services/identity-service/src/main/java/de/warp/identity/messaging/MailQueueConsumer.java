// [KI-Log Nr. KI-21] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone RabbitMQ Mail-Queue & Broker-Fallback (WARP-036)
package de.warp.identity.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/**
 * Verarbeitet Mail-Aufträge aus der Queue. Nach dem letzten fehlgeschlagenen
 * Versuch wird die Nachricht ohne Requeue abgewiesen und landet in der
 * Dead-Letter-Queue.
 */
@Component
@ConditionalOnProperty(
        name = "warp.messaging.enabled",
        havingValue = "true"
)
public class MailQueueConsumer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MailQueueConsumer.class);

    private final JsonMapper jsonMapper;
    private final MailDeliveryService mailDeliveryService;
    private final int maxAttempts;
    private final Duration retryBackoff;

    public MailQueueConsumer(
            JsonMapper jsonMapper,
            MailDeliveryService mailDeliveryService,
            @Value("${warp.messaging.mail.max-attempts:3}")
            int maxAttempts,
            @Value("${warp.messaging.mail.retry-backoff:PT2S}")
            Duration retryBackoff
    ) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "max-attempts muss mindestens 1 sein."
            );
        }

        this.jsonMapper = jsonMapper;
        this.mailDeliveryService = mailDeliveryService;
        this.maxAttempts = maxAttempts;
        this.retryBackoff = retryBackoff;
    }

    @RabbitListener(queues = MailMessagingTopology.MAIL_QUEUE)
    public void onMessage(String payload) {
        EmailRequestedEvent event = parse(payload);

        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                mailDeliveryService.deliver(event);
                LOGGER.info(
                        "Mail-Auftrag {} ({}) zugestellt.",
                        event.eventId(),
                        event.type()
                );
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                LOGGER.warn(
                        "Zustellung von Mail-Auftrag {} ({}) fehlgeschlagen "
                                + "(Versuch {}/{}).",
                        event.eventId(),
                        event.type(),
                        attempt,
                        maxAttempts,
                        exception
                );

                if (attempt < maxAttempts && !pause()) {
                    break;
                }
            }
        }

        throw new AmqpRejectAndDontRequeueException(
                "Mail-Auftrag " + event.eventId()
                        + " endgültig fehlgeschlagen.",
                lastFailure
        );
    }

    private EmailRequestedEvent parse(String payload) {
        try {
            return jsonMapper.readValue(payload, EmailRequestedEvent.class);
        } catch (RuntimeException exception) {
            // Ohne Ursache: Parser-Meldungen könnten Teile des Tokens enthalten.
            throw new AmqpRejectAndDontRequeueException(
                    "Mail-Nachricht ist nicht lesbar."
            );
        }
    }

    /** @return false, wenn die Wartezeit unterbrochen wurde. */
    private boolean pause() {
        if (retryBackoff.isZero() || retryBackoff.isNegative()) {
            return true;
        }

        try {
            Thread.sleep(retryBackoff.toMillis());
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}