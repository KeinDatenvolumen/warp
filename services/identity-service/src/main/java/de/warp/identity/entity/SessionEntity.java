// [KI-Log Nr. KI-078] (02bc37f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat WARP-006: add session persistence model
// [KI-Log Nr. KI-080] (093af46) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: add session persistence model
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "sessions", schema = "warp_identity")
public class SessionEntity {

    @Id
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    protected SessionEntity() {
        // Wird von JPA benötigt.
    }

    public SessionEntity(
            String id,
            String tokenHash,
            UserEntity user,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.user = user;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void invalidate(Instant invalidatedAt) {
        if (this.invalidatedAt == null) {
            this.invalidatedAt = invalidatedAt;
        }
    }

    public boolean isValidAt(Instant instant) {
        return invalidatedAt == null && expiresAt.isAfter(instant);
    }

    public String getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UserEntity getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }
}
