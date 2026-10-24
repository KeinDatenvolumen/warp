
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
            this.emailVerified = true;
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
