// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-231] (80972f5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: decouple e-mail delivery from identity service via RabbitMQ (WARP-036)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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