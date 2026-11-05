// [KI-Log Nr. KI-21] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone RabbitMQ Mail-Queue & Broker-Fallback (WARP-036)
package de.warp.identity.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.MailSendException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class QueueMailEventRelayTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MailDeliveryService mailDeliveryService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private QueueMailEventRelay relay;
    private EmailRequestedEvent event;

    @BeforeEach
    void setUp() {
        relay = new QueueMailEventRelay(
                rabbitTemplate, jsonMapper, mailDeliveryService
        );
        event = EmailRequestedEvent.create(
                MailType.VERIFICATION, "usr_1", "geheimer-token"
        );
    }

    @Test
    void shouldPublishJsonToMailExchange() {
        relay.onEmailRequested(event);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);

        verify(rabbitTemplate).convertAndSend(
                eq(MailMessagingTopology.MAIL_EXCHANGE),
                eq(MailMessagingTopology.MAIL_ROUTING_KEY),
                payload.capture()
        );

        EmailRequestedEvent sent = jsonMapper.readValue(
                payload.getValue(), EmailRequestedEvent.class
        );

        assertThat(sent.eventId()).isEqualTo(event.eventId());
        assertThat(sent.type()).isEqualTo(MailType.VERIFICATION);
        assertThat(sent.userId()).isEqualTo("usr_1");
        assertThat(sent.token()).isEqualTo("geheimer-token");
        assertThat(payload.getValue()).doesNotContain("@");
        verifyNoInteractions(mailDeliveryService);
    }

    @Test
    void shouldFallBackToDirectDeliveryWhenBrokerIsUnavailable() {
        doThrow(new AmqpConnectException(new RuntimeException("down")))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        relay.onEmailRequested(event);

        verify(mailDeliveryService).deliver(event);
    }

    @Test
    void shouldNotPropagateWhenFallbackFailsToo() {
        doThrow(new AmqpConnectException(new RuntimeException("down")))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());
        doThrow(new MailSendException("smtp down"))
                .when(mailDeliveryService).deliver(event);

        assertDoesNotThrow(() -> relay.onEmailRequested(event));
    }
}