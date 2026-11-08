// [KI-Log Nr. KI-231] (80972f5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: decouple e-mail delivery from identity service via RabbitMQ (WARP-036)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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