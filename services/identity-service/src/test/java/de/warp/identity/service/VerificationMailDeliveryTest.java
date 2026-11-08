// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-231] (80972f5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: decouple e-mail delivery from identity service via RabbitMQ (WARP-036)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.identity.service;

import de.warp.identity.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VerificationMailDeliveryTest {

    private JavaMailSender mailSender;
    private VerificationMailDelivery verificationMailDelivery;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);

        verificationMailDelivery = new VerificationMailDelivery(
                mailSender,
                "no-reply@warp.local",
                "http://localhost:5173/"
        );
    }

    @Test
    void shouldSendVerificationEmailWithCorrectLink() {
        UserEntity user = createUser();

        verificationMailDelivery.sendVerificationEmail(
                user,
                "mein-test-token"
        );

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom())
                .isEqualTo("no-reply@warp.local");

        assertThat(message.getTo())
                .containsExactly("test@example.com");

        assertThat(message.getSubject())
                .isEqualTo(
                        "E-Mail-Adresse für WARP bestätigen"
                );

        assertThat(message.getText())
                .contains("Hallo Test User")
                .contains(
                        "http://localhost:5173/verify-email"
                                + "?token=mein-test-token"
                )
                .contains("24 Stunden gültig")
                .contains("nur einmal verwendet");
    }

    @Test
    void shouldSendVerificationEmailToPendingEmailWhenPresent() {
        UserEntity user = createUser();
        user.requestEmailChange("pending@example.com");

        verificationMailDelivery.sendVerificationEmail(
                user,
                "mein-test-token"
        );

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo())
                .containsExactly("pending@example.com");
    }

    @Test
    void shouldCreateLinkWithoutDuplicateSlash() {
        String link = verificationMailDelivery
                .createConfirmationLink("test-token");

        assertThat(link).isEqualTo(
                "http://localhost:5173/verify-email"
                        + "?token=test-token"
        );
    }

    @Test
    void shouldRejectBlankToken() {
        assertThatThrownBy(
                () -> verificationMailDelivery
                        .sendVerificationEmail(
                                createUser(),
                                "   "
                        )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "plainToken darf nicht leer sein"
                );
    }

    @Test
    void shouldRejectNullUser() {
        assertThatThrownBy(
                () -> verificationMailDelivery
                        .sendVerificationEmail(
                                null,
                                "test-token"
                        )
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessage("user darf nicht null sein");
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