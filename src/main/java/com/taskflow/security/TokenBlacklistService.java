package com.taskflow.security;

import com.taskflow.model.RevokedToken;
import com.taskflow.repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * JWTs are stateless, so "logout" is implemented by remembering the ids of tokens that were logged out
 * and rejecting them in the auth filter until they expire naturally.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final RevokedTokenRepository repository;

    public TokenBlacklistService(RevokedTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void revoke(String tokenId, Instant expiresAt) {
        if (!repository.existsById(tokenId)) {
            repository.save(new RevokedToken(tokenId, expiresAt));
        }
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String tokenId) {
        return repository.existsById(tokenId);
    }

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteExpired(Instant.now());
        if (removed > 0) {
            log.info("Removed {} expired entries from the token blacklist", removed);
        }
    }
}
