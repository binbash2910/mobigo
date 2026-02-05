package com.binbash.mobigo.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

/**
 * Stores invalidated (blacklisted) JWT tokens to prevent reuse after logout.
 * Only the SHA-256 hash of the token is stored for security.
 * Entries are automatically cleaned up after the token's expiration date.
 */
@Entity
@Table(name = "invalidated_token")
public class InvalidatedToken implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @NotNull
    @Column(name = "invalidated_at", nullable = false)
    private Instant invalidatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }

    public void setInvalidatedAt(Instant invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvalidatedToken)) return false;
        return getId() != null && getId().equals(((InvalidatedToken) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return (
            "InvalidatedToken{" +
            "id=" +
            getId() +
            ", tokenHash='" +
            getTokenHash() +
            "'" +
            ", expiresAt='" +
            getExpiresAt() +
            "'" +
            ", invalidatedAt='" +
            getInvalidatedAt() +
            "'" +
            "}"
        );
    }
}
