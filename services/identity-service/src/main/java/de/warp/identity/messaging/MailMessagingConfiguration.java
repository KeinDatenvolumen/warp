// [KI-Log Nr. KI-21] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone RabbitMQ Mail-Queue & Broker-Fallback (WARP-036)
package de.warp.identity.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static de.warp.identity.messaging.MailMessagingTopology.DEAD_LETTER_EXCHANGE;
import static de.warp.identity.messaging.MailMessagingTopology.DEAD_LETTER_QUEUE;
import static de.warp.identity.messaging.MailMessagingTopology.DEAD_LETTER_ROUTING_KEY;
import static de.warp.identity.messaging.MailMessagingTopology.MAIL_EXCHANGE;
import static de.warp.identity.messaging.MailMessagingTopology.MAIL_QUEUE;
import static de.warp.identity.messaging.MailMessagingTopology.MAIL_ROUTING_KEY;

@Configuration
@ConditionalOnProperty(
        name = "warp.messaging.enabled",
        havingValue = "true"
)
public class MailMessagingConfiguration {

    @Bean
    DirectExchange mailExchange() {
        return new DirectExchange(MAIL_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange mailDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue mailQueue() {
        return QueueBuilder.durable(MAIL_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue mailDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding mailBinding() {
        return BindingBuilder
                .bind(mailQueue())
                .to(mailExchange())
                .with(MAIL_ROUTING_KEY);
    }

    @Bean
    Binding mailDeadLetterBinding() {
        return BindingBuilder
                .bind(mailDeadLetterQueue())
                .to(mailDeadLetterExchange())
                .with(DEAD_LETTER_ROUTING_KEY);
    }
}