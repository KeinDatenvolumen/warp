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

class PasswordResetMailDeliveryTest {

    private JavaMailSender mailSender;
    private PasswordResetMailDelivery passwordResetMailDelivery;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);

        passwordResetMailDelivery =
                new PasswordResetMailDelivery(
                        mailSender,
                        "no-reply@warp.local",
                        "http://localhost:5173/"
                );
    }

    @Test
    void shouldSendPasswordResetEmailWithCorrectLink() {
        UserEntity user = createUser();

        passwordResetMailDelivery.sendPasswordResetEmail(
                user,
                "mein-reset-token"
        );

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(
                        SimpleMailMessage.class
                );

        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom())
                .isEqualTo("no-reply@warp.local");

        assertThat(message.getTo())
                .containsExactly("test@example.com");

        assertThat(message.getSubject())
                .isEqualTo(
                        "WARP-Passwort zurücksetzen"
                );

        assertThat(message.getText())
                .contains("Hallo Test User")
                .contains(
                        "http://localhost:5173/reset-password"
                                + "?token=mein-reset-token"
                )
                .contains("30 Minuten gültig")
                .contains("nur einmal verwendet")
                .contains(
                        "Dein bisheriges Passwort bleibt unverändert"
                );
    }

    @Test
    void shouldCreateLinkWithoutDuplicateSlash() {
        String link = passwordResetMailDelivery
                .createResetLink("test-token");

        assertThat(link).isEqualTo(
                "http://localhost:5173/reset-password"
                        + "?token=test-token"
        );
    }

    @Test
    void shouldEncodeTokenInResetLink() {
        String link = passwordResetMailDelivery
                .createResetLink(
                        "token mit leerzeichen"
                );

        assertThat(link).isEqualTo(
                "http://localhost:5173/reset-password"
                        + "?token=token%20mit%20leerzeichen"
        );
    }

    @Test
    void shouldRejectBlankToken() {
        assertThatThrownBy(
                () -> passwordResetMailDelivery
                        .sendPasswordResetEmail(
                                createUser(),
                                "   "
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "plainToken darf nicht leer sein"
                );
    }

    @Test
    void shouldRejectNullUser() {
        assertThatThrownBy(
                () -> passwordResetMailDelivery
                        .sendPasswordResetEmail(
                                null,
                                "test-token"
                        )
        )
                .isInstanceOf(
                        NullPointerException.class
                )
                .hasMessage(
                        "user darf nicht null sein"
                );
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