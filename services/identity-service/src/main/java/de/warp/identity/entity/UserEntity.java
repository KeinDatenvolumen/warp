// [KI-Log Nr. KI-051] (cfd2cd9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/warp-005: implement user registration with Tests
// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-202] (d555a4e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add user lookup and account change endpoints to identity service (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)

    package de.warp.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

    @Entity
    @Table(name = "users", schema = "warp_identity")
    public class UserEntity {

        @Id
        @Column(name = "id", nullable = false, length = 40)
        private String id;

        @Column(name = "name", nullable = false, length = 255)
        private String name;

        @Column(name = "email", nullable = false, length = 320)
        private String email;

        @Column(name = "pending_email", length = 320)
        private String pendingEmail;

        @Column(name = "password_hash", nullable = false, length = 255)
        private String passwordHash;

        @Column(name = "email_verified", nullable = false)
        private boolean emailVerified;

        @Column(name = "created_at", nullable = false, updatable = false)
        private Instant createdAt;

        protected UserEntity() {
            // Wird von JPA benötigt.
        }

        public UserEntity(
                String id,
                String name,
                String email,
                String passwordHash
        ) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.passwordHash = passwordHash;
            this.emailVerified = false;
        }

        @PrePersist
        void initializeCreatedAt() {
            if (createdAt == null) {
                createdAt = Instant.now();
            }
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPendingEmail() {
            return pendingEmail;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public boolean isEmailVerified() {
            return emailVerified;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void markEmailAsVerified() {
            if (this.pendingEmail != null && !this.pendingEmail.isBlank()) {
                this.email = this.pendingEmail;
                this.pendingEmail = null;
            }
            this.emailVerified = true;
        }

        public void requestEmailChange(String newEmail) {
            if (newEmail == null || newEmail.isBlank()) {
                throw new IllegalArgumentException(
                        "email darf nicht leer sein"
                );
            }

            this.pendingEmail = newEmail;
        }

        public void changeEmail(String newEmail) {
            if (newEmail == null || newEmail.isBlank()) {
                throw new IllegalArgumentException(
                        "email darf nicht leer sein"
                );
            }

            this.email = newEmail;
            this.pendingEmail = null;
            this.emailVerified = false;
        }

        public void changePasswordHash(String passwordHash) {
            if (passwordHash == null || passwordHash.isBlank()) {
                throw new IllegalArgumentException(
                        "passwordHash darf nicht leer sein"
                );
            }

            this.passwordHash = passwordHash;
        }

    }
