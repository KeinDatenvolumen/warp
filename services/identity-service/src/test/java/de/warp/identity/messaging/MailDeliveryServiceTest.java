// [KI-Log Nr. KI-231] (80972f5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: decouple e-mail delivery from identity service via RabbitMQ (WARP-036)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.messaging;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.UserRepository;
import de.warp.identity.service.PasswordResetMailDelivery;
import de.warp.identity.service.VerificationMailDelivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailDeliveryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationMailDelivery verificationMailDelivery;

    @Mock
    private PasswordResetMailDelivery passwordResetMailDelivery;

    private MailDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new MailDeliveryService(
                userRepository,
                verificationMailDelivery,
                passwordResetMailDelivery
        );
    }

    @Test
    void shouldDeliverVerificationMail() {
        UserEntity user = createUser();
        when(userRepository.findById("usr_1")).thenReturn(Optional.of(user));

        service.deliver(EmailRequestedEvent.create(
                MailType.VERIFICATION, "usr_1", "token-1"
        ));

        verify(verificationMailDelivery)
                .sendVerificationEmail(user, "token-1");
        verifyNoInteractions(passwordResetMailDelivery);
    }

    @Test
    void shouldDeliverPasswordResetMail() {
        UserEntity user = createUser();
        when(userRepository.findById("usr_1")).thenReturn(Optional.of(user));

        service.deliver(EmailRequestedEvent.create(
                MailType.PASSWORD_RESET, "usr_1", "token-2"
        ));

        verify(passwordResetMailDelivery)
                .sendPasswordResetEmail(user, "token-2");
        verifyNoInteractions(verificationMailDelivery);
    }

    @Test
    void shouldDropJobWhenUserNoLongerExists() {
        when(userRepository.findById("usr_1")).thenReturn(Optional.empty());

        service.deliver(EmailRequestedEvent.create(
                MailType.VERIFICATION, "usr_1", "token-1"
        ));

        verifyNoInteractions(verificationMailDelivery, passwordResetMailDelivery);
    }

    private UserEntity createUser() {
        return new UserEntity(
                "usr_1", "Max", "max@example.de", "hash"
        );
    }
}