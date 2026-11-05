// [KI-Log Nr. KI-21] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone RabbitMQ Mail-Queue & Broker-Fallback (WARP-036)
package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.messaging.EmailRequestedEvent;
import de.warp.identity.messaging.MailType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PasswordResetEmailSenderTest {

    private ApplicationEventPublisher eventPublisher;
    private PasswordResetEmailSender sender;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        sender = new PasswordResetEmailSender(eventPublisher);
    }

    @Test
    void shouldPublishPasswordResetEvent() {
        sender.sendPasswordResetEmail(createUser(), "mein-reset-token");

        ArgumentCaptor<EmailRequestedEvent> captor =
                ArgumentCaptor.forClass(EmailRequestedEvent.class);

        verify(eventPublisher).publishEvent(captor.capture());

        EmailRequestedEvent event = captor.getValue();

        assertThat(event.type()).isEqualTo(MailType.PASSWORD_RESET);
        assertThat(event.userId()).isEqualTo("usr_1234567890");
        assertThat(event.token()).isEqualTo("mein-reset-token");
    }

    @Test
    void shouldRejectBlankToken() {
        assertThatThrownBy(
                () -> sender.sendPasswordResetEmail(createUser(), "   ")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plainToken darf nicht leer sein");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldRejectNullUser() {
        assertThatThrownBy(
                () -> sender.sendPasswordResetEmail(null, "test-token")
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("user darf nicht null sein");

        verifyNoInteractions(eventPublisher);
    }

    private UserEntity createUser() {
        return new UserEntity(
                "usr_1234567890",
                "Test User",
                "test@example.com",
                "password-hash"
        );
    }
}