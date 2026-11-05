// [KI-Log Nr. KI-14] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Passwort-Reset-Workflow (WARP-008)
package de.warp.identity.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

@Component
public class PasswordResetTokenGenerator {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom;

    public PasswordResetTokenGenerator() {
        this(new SecureRandom());
    }

    PasswordResetTokenGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(
                secureRandom,
                "secureRandom darf nicht null sein"
        );
    }

    public GeneratedToken generate() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);

        String plainToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        return new GeneratedToken(
                plainToken,
                hash(plainToken)
        );
    }

    public String hash(String plainToken) {
        Objects.requireNonNull(
                plainToken,
                "plainToken darf nicht null sein"
        );

        if (plainToken.isBlank()) {
            throw new IllegalArgumentException(
                    "plainToken darf nicht leer sein"
            );
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                    plainToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 wird von der Laufzeitumgebung nicht unterstützt",
                    exception
            );
        }
    }

    public record GeneratedToken(
            String plainToken,
            String tokenHash
    ) {

        public GeneratedToken {
            Objects.requireNonNull(
                    plainToken,
                    "plainToken darf nicht null sein"
            );
            Objects.requireNonNull(
                    tokenHash,
                    "tokenHash darf nicht null sein"
            );

            if (plainToken.isBlank()) {
                throw new IllegalArgumentException(
                        "plainToken darf nicht leer sein"
                );
            }

            if (tokenHash.isBlank()) {
                throw new IllegalArgumentException(
                        "tokenHash darf nicht leer sein"
                );
            }
        }
    }
}