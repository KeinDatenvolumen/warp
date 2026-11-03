package de.warp.identity.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DirectMailEventRelayTest {

    @Mock
    private MailDeliveryService mailDeliveryService;

    @Test
    void shouldDeliverDirectly() {
        EmailRequestedEvent event = EmailRequestedEvent.create(
                MailType.PASSWORD_RESET, "usr_1", "token"
        );

        new DirectMailEventRelay(mailDeliveryService)
                .onEmailRequested(event);

        verify(mailDeliveryService).deliver(event);
    }

    @Test
    void shouldNotPropagateDeliveryFailures() {
        EmailRequestedEvent event = EmailRequestedEvent.create(
                MailType.PASSWORD_RESET, "usr_1", "token"
        );
        doThrow(new MailSendException("smtp down"))
                .when(mailDeliveryService).deliver(event);

        assertDoesNotThrow(
                () -> new DirectMailEventRelay(mailDeliveryService)
                        .onEmailRequested(event)
        );
    }
}