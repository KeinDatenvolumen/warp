// [KI-Log Nr. KI-231] (80972f5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: decouple e-mail delivery from identity service via RabbitMQ (WARP-036)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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