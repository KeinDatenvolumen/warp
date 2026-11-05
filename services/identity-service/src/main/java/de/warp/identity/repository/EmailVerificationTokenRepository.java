// [KI-Log Nr. KI-11] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): E-Mail-Verifikations-Flow (WARP-007)
package de.warp.identity.repository;

import de.warp.identity.entity.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationTokenEntity, String> {

    Optional<EmailVerificationTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE EmailVerificationTokenEntity token
               SET token.usedAt = :usedAt
             WHERE token.user.id = :userId
               AND token.usedAt IS NULL
            """)
    int markAllUnusedTokensAsUsed(
            @Param("userId") String userId,
            @Param("usedAt") Instant usedAt
    );
}