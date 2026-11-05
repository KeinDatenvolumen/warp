// [KI-Log Nr. KI-11] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): E-Mail-Verifikations-Flow (WARP-007)
package de.warp.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "email_verification_tokens",
        schema = "warp_identity"
)
public class EmailVerificationTokenEntity {

    @Id
    @Column(name = "id", nullable = false, length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailVerificationTokenEntity() {
        // Wird von JPA benötigt.
    }

    public EmailVerificationTokenEntity(
            String id,
            UserEntity user,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(
                id,
                "id darf nicht null sein"
        );
        this.user = Objects.requireNonNull(
                user,
                "user darf nicht null sein"
        );
        this.tokenHash = Objects.requireNonNull(
                tokenHash,
                "tokenHash darf nicht null sein"
        );
        this.expiresAt = Objects.requireNonNull(
                expiresAt,
                "expiresAt darf nicht null sein"
        );
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt darf nicht null sein"
        );
    }

    public String getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, "now darf nicht null sein");
        return !expiresAt.isAfter(now);
    }

    public void markAsUsed(Instant usedAt) {
        Objects.requireNonNull(
                usedAt,
                "usedAt darf nicht null sein"
        );

        if (this.usedAt != null) {
            throw new IllegalStateException(
                    "Verifikationstoken wurde bereits verwendet"
            );
        }

        if (usedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "usedAt darf nicht vor createdAt liegen"
            );
        }

        this.usedAt = usedAt;
    }
}