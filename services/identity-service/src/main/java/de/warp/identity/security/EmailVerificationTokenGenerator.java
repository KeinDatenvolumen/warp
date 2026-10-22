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
public class EmailVerificationTokenGenerator {

    private static final int TOKEN_LENGTH_IN_BYTES = 32;

    private final SecureRandom secureRandom;

    public EmailVerificationTokenGenerator() {
        this(new SecureRandom());
    }

    EmailVerificationTokenGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(
                secureRandom,
                "secureRandom darf nicht null sein"
        );
    }

    public GeneratedToken generate() {
        byte[] randomBytes = new byte[TOKEN_LENGTH_IN_BYTES];
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
                    "SHA-256 wird von der Java-Laufzeit nicht unterstützt",
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

            if (tokenHash.length() != 64) {
                throw new IllegalArgumentException(
                        "tokenHash muss genau 64 Zeichen besitzen"
                );
            }
        }
    }
}