package com.taskflow;

import com.taskflow.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-definitely-long-enough";

    @Test
    void tokenRoundTripKeepsSubjectAndId() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        Claims claims = jwtService.parse(jwtService.generateToken("someone@example.com"));

        assertEquals("someone@example.com", claims.getSubject());
        assertNotNull(claims.getId());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken("someone@example.com");

        assertThrows(JwtException.class, () -> jwtService.parse(token + "x"));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, -1_000);
        String token = jwtService.generateToken("someone@example.com");

        assertThrows(JwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = new JwtService("another-secret-that-is-also-long-enough-123", 60_000)
                .generateToken("someone@example.com");

        assertThrows(JwtException.class, () -> new JwtService(SECRET, 60_000).parse(token));
    }
}
