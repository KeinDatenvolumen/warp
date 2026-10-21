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

class VerificationEmailSenderTest {

    private JavaMailSender mailSender;
    private VerificationEmailSender verificationEmailSender;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);

        verificationEmailSender = new VerificationEmailSender(
                mailSender,
                "no-reply@warp.local",
                "http://localhost:5173/"
        );
    }

    @Test
    void shouldSendVerificationEmailWithCorrectLink() {
        UserEntity user = createUser();

        verificationEmailSender.sendVerificationEmail(
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
    void shouldCreateLinkWithoutDuplicateSlash() {
        String link = verificationEmailSender
                .createConfirmationLink("test-token");

        assertThat(link).isEqualTo(
                "http://localhost:5173/verify-email"
                        + "?token=test-token"
        );
    }

    @Test
    void shouldRejectBlankToken() {
        assertThatThrownBy(
                () -> verificationEmailSender
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
                () -> verificationEmailSender
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