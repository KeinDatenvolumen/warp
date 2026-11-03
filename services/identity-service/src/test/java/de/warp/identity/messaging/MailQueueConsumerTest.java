package de.warp.identity.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.mail.MailSendException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MailQueueConsumerTest {

    @Mock
    private MailDeliveryService mailDeliveryService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private MailQueueConsumer consumer;
    private String payload;

    @BeforeEach
    void setUp() {
        consumer = new MailQueueConsumer(
                jsonMapper, mailDeliveryService, 3, Duration.ZERO
        );
        payload = jsonMapper.writeValueAsString(
                EmailRequestedEvent.create(
                        MailType.VERIFICATION, "usr_1", "token-1"
                )
        );
    }

    @Test
    void shouldDeliverMessage() {
        consumer.onMessage(payload);

        ArgumentCaptor<EmailRequestedEvent> captor =
                ArgumentCaptor.forClass(EmailRequestedEvent.class);

        verify(mailDeliveryService).deliver(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo("usr_1");
        assertThat(captor.getValue().type()).isEqualTo(MailType.VERIFICATION);
    }

    @Test
    void shouldRetryUntilDeliverySucceeds() {
        doThrow(new MailSendException("smtp down"))
                .doNothing()
                .when(mailDeliveryService)
                .deliver(any(EmailRequestedEvent.class));

        consumer.onMessage(payload);

        verify(mailDeliveryService, times(2))
                .deliver(any(EmailRequestedEvent.class));
    }

    @Test
    void shouldRejectWithoutRequeueAfterLastAttempt() {
        doThrow(new MailSendException("smtp down"))
                .when(mailDeliveryService)
                .deliver(any(EmailRequestedEvent.class));

        assertThatThrownBy(() -> consumer.onMessage(payload))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(mailDeliveryService, times(3))
                .deliver(any(EmailRequestedEvent.class));
    }

    @Test
    void shouldRejectUnreadablePayload() {
        assertThatThrownBy(() -> consumer.onMessage("{kaputt"))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(mailDeliveryService);
    }
}