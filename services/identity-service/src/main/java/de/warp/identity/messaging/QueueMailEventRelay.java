package de.warp.identity.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

import static de.warp.identity.messaging.MailMessagingTopology.MAIL_EXCHANGE;
import static de.warp.identity.messaging.MailMessagingTopology.MAIL_ROUTING_KEY;

/**
 * Reiht Mail-Aufträge nach erfolgreichem Commit in RabbitMQ ein. Ein
 * Rollback der Transaktion erzeugt daher keine Mail. Ist der Broker nicht
 * erreichbar, wird direkt zugestellt, damit kein Auftrag verloren geht.
 */
@Component
@ConditionalOnProperty(
        name = "warp.messaging.enabled",
        havingValue = "true"
)
public class QueueMailEventRelay {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(QueueMailEventRelay.class);

    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;
    private final MailDeliveryService mailDeliveryService;

    public QueueMailEventRelay(
            RabbitTemplate rabbitTemplate,
            JsonMapper jsonMapper,
            MailDeliveryService mailDeliveryService
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
        this.mailDeliveryService = mailDeliveryService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void onEmailRequested(EmailRequestedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    MAIL_EXCHANGE,
                    MAIL_ROUTING_KEY,
                    jsonMapper.writeValueAsString(event)
            );

            LOGGER.info(
                    "Mail-Auftrag {} ({}) eingereiht.",
                    event.eventId(),
                    event.type()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Mail-Auftrag {} ({}) konnte nicht eingereiht werden. "
                            + "Direkte Zustellung wird versucht.",
                    event.eventId(),
                    event.type(),
                    exception
            );
            deliverDirectly(event);
        }
    }

    private void deliverDirectly(EmailRequestedEvent event) {
        try {
            mailDeliveryService.deliver(event);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Mail-Auftrag {} ({}) konnte weder eingereiht noch "
                            + "zugestellt werden.",
                    event.eventId(),
                    event.type(),
                    exception
            );
        }
    }
}