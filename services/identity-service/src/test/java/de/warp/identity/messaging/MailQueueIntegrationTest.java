// [KI-Log Nr. KI-231] (80972f5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: decouple e-mail delivery from identity service via RabbitMQ (WARP-036)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.messaging;

import de.warp.identity.entity.UserEntity;
import de.warp.identity.repository.UserRepository;
import de.warp.identity.service.VerificationEmailSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest(properties = {
        "warp.messaging.enabled=true",
        "warp.messaging.mail.max-attempts=2",
        "warp.messaging.mail.retry-backoff=PT0S",
        "management.health.mail.enabled=false"
})
class MailQueueIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Container
    static GenericContainer<?> rabbit =
            new GenericContainer<>("rabbitmq:4-management-alpine")
                    .withExposedPorts(5672)
                    .withEnv("RABBITMQ_DEFAULT_USER", "warp")
                    .withEnv("RABBITMQ_DEFAULT_PASS", "warp")
                    .waitingFor(Wait.forLogMessage(
                            ".*Server startup complete.*\\n", 1
                    ));

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.rabbitmq.addresses",
                () -> "amqp://warp:warp@" + rabbit.getHost()
                        + ":" + rabbit.getMappedPort(5672)
        );
    }

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationEmailSender verificationEmailSender;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldDeliverVerificationMailThroughQueue() {
        UserEntity user = userRepository.save(new UserEntity(
                "usr_queue_1", "Queue User", "queue1@example.de", "hash"
        ));

        verificationEmailSender.sendVerificationEmail(user, "queue-token-1");

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender, timeout(10_000)).send(captor.capture());

        assertThat(captor.getValue().getTo())
                .containsExactly("queue1@example.de");
        assertThat(captor.getValue().getText()).contains("queue-token-1");
    }

    @Test
    void shouldMoveFailedMailToDeadLetterQueue() {
        doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        UserEntity user = userRepository.save(new UserEntity(
                "usr_queue_2", "Queue User 2", "queue2@example.de", "hash"
        ));

        verificationEmailSender.sendVerificationEmail(user, "queue-token-2");

        Message dead = rabbitTemplate.receive(
                MailMessagingTopology.DEAD_LETTER_QUEUE, 15_000
        );

        assertThat(dead).isNotNull();
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
}