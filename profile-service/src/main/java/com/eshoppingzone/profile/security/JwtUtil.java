package com.eshoppingzone.profile.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility class for JWT token operations.
 * Handles token generation, validation, and claims extraction.
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        logger.info("JwtUtil initialized with expiration: {} ms", expirationMs);
    }

    /**
     * Generates a JWT token for the given user.
     *
     * @param email  User's email address (used as subject)
     * @param userId User's unique identifier
     * @return Signed JWT token string
     */
    public String generateToken(String email, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();

        logger.info("JWT token generated for user: {} (userId: {}), expires at: {}", email, userId, expiryDate);
        return token;
    }

    /**
     * Validates the JWT token and returns the claims if valid.
     *
     * @param token JWT token to validate
     * @return Claims object containing token payload
     * @throws ExpiredJwtException      if token has expired
     * @throws MalformedJwtException    if token is malformed
     * @throws SignatureException       if signature validation fails
     * @throws UnsupportedJwtException  if token format is unsupported
     * @throws IllegalArgumentException if token is null or empty
     */
    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            logger.debug("JWT token validated successfully for subject: {}", claims.getSubject());
            return claims;
        } catch (ExpiredJwtException ex) {
            logger.error("JWT token has expired: {}", ex.getMessage());
            throw ex;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token format: {}", ex.getMessage());
            throw ex;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
            throw ex;
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
            throw ex;
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty or null: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Extracts the email (subject) from a valid token.
     *
     * @param token JWT token
     * @return Email address from token subject
     */
    public String getEmailFromToken(String token) {
        return validateToken(token).getSubject();
    }

    /**
     * Extracts the userId claim from a valid token.
     *
     * @param token JWT token
     * @return User ID from token claims
     */
    public Long getUserIdFromToken(String token) {
        return validateToken(token).get("userId", Long.class);
    }

    /**
     * Checks if the token is expired without throwing an exception.
     *
     * @param token JWT token
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (Exception ex) {
            logger.warn("Token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // =========================================================================
    // FUTURE ENHANCEMENT: 2FA/TOTP Support (Phase 2)
    // =========================================================================
    // To add Time-based One-Time Password (TOTP) support:
    // 
    // 1. Add dependency: com.warrenstrange:googleauth:1.5.0
    //
    // 2. Add TOTP generation method:
    // public String generateTotpSecret() {
    //     GoogleAuthenticator gAuth = new GoogleAuthenticator();
    //     GoogleAuthenticatorKey key = gAuth.createCredentials();
    //     return key.getKey();
    // }
    //
    // 3. Add TOTP verification method:
    // public boolean verifyTotp(String secret, int code) {
    //     GoogleAuthenticator gAuth = new GoogleAuthenticator();
    //     return gAuth.authorize(secret, code);
    // }
    //
    // 4. Store totpSecret in User entity (encrypted)
    // 5. Add isTwoFactorEnabled boolean to User entity
    // 6. Modify login flow to require TOTP code if 2FA is enabled
    // =========================================================================

}
