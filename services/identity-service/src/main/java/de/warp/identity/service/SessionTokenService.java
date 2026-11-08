// [KI-Log Nr. KI-081] (26da463) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add secure session token generation
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.service;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class SessionTokenService {

    private static final int TOKEN_LENGTH_BYTES = 32;
    private static final String HASH_ALGORITHM = "SHA-256";

    private final SecureRandom secureRandom;

    public SessionTokenService() {
        this.secureRandom = new SecureRandom();
    }

    public String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_LENGTH_BYTES];
        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    public String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] tokenHash = messageDigest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(tokenHash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Required session token hash algorithm is not available.",
                    exception
            );
        }
    }
}
