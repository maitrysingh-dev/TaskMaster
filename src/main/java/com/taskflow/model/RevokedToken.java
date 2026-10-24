package com.taskflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A JWT id (jti) that was invalidated through logout. Rows are purged once the token would have expired anyway. */
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @Column(length = 64)
    private String tokenId;

    @Column(nullable = false)
    private Instant expiresAt;

    protected RevokedToken() {
    }

    public RevokedToken(String tokenId, Instant expiresAt) {
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
    }

    public String getTokenId() {
        return tokenId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
