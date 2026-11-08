// [KI-Log Nr. KI-051] (cfd2cd9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/warp-005: implement user registration with Tests
// [KI-Log Nr. KI-104] (2e8b6e1) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: improve SessionAuthenticationFilter and Tests (WARP-006)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.repository;

import de.warp.identity.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@Transactional
class UserRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18")
                    .withDatabaseName("warp_identity_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );
        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveUser() {
        UserEntity user = new UserEntity(
                "usr_123",
                "Max Mustermann",
                "max@example.de",
                "hashed-password"
        );

        UserEntity savedUser = userRepository.saveAndFlush(user);

        assertEquals("usr_123", savedUser.getId());
        assertEquals("Max Mustermann", savedUser.getName());
        assertEquals("max@example.de", savedUser.getEmail());
        assertEquals(
                "hashed-password",
                savedUser.getPasswordHash()
        );
        assertFalse(savedUser.isEmailVerified());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    void shouldFindExistingEmailIgnoringCase() {
        UserEntity user = new UserEntity(
                "usr_456",
                "Max Mustermann",
                "max@example.de",
                "hashed-password"
        );

        userRepository.saveAndFlush(user);

        boolean emailExists =
                userRepository.existsByEmailIgnoreCase("MAX@EXAMPLE.DE");

        assertTrue(emailExists);
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() {
        UserEntity firstUser = new UserEntity(
                "usr_789",
                "Max Mustermann",
                "max@example.de",
                "first-hashed-password"
        );

        UserEntity secondUser = new UserEntity(
                "usr_987",
                "Maria Musterfrau",
                "MAX@EXAMPLE.DE",
                "second-hashed-password"
        );

        userRepository.saveAndFlush(firstUser);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(secondUser)
        );
    }

}