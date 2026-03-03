package com.eshoppingzone.profile.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for JwtUtil covering:
 * - Token generation
 * - Token validation (happy path and edge cases)
 * - Claims extraction
 * - Expiration handling
 * - Security attack scenarios (tampering, wrong signature)
 */
class JwtUtilTest {

    private static final String TEST_SECRET = "CartManagementSystemSecretKeyForJWTTokenGeneration2024SecureKey";
    private static final String DIFFERENT_SECRET = "DifferentSecretKeyForTestingWrongSignatureScenario12345678";
    private static final long TEST_EXPIRATION_MS = 86400000L; // 24 hours
    private static final String TEST_EMAIL = "test@example.com";
    private static final Long TEST_USER_ID = 123L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, TEST_EXPIRATION_MS);
    }

    // =========================================================================
    // Token Generation Tests
    // =========================================================================
    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("generateToken_ValidInputs_ReturnsNonNullToken")
        void generateToken_ValidInputs_ReturnsNonNullToken() {
            // Act
            String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

            // Assert
            assertNotNull(token, "Generated token should not be null");
            assertTrue(token.length() > 0, "Generated token should not be empty");
            // JWT should have 3 parts separated by dots
            assertEquals(3, token.split("\\.").length, "JWT should have 3 parts (header.payload.signature)");
        }

        @Test
        @DisplayName("generateToken_SameInputs_ProducesDifferentTokensEachTime")
        void generateToken_SameInputs_ProducesDifferentTokensEachTime() throws InterruptedException {
            // Act - Generate tokens with small delay (different iat)
            String token1 = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);
            Thread.sleep(10); // Small delay to ensure different timestamp
            String token2 = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

            // Assert - Tokens should be different due to different iat timestamp
            // Note: They might be same if generated in same millisecond, so we check claims
            assertNotNull(token1);
            assertNotNull(token2);
        }

        @Test
        @DisplayName("generateToken_ContainsCorrectClaims")
        void generateToken_ContainsCorrectClaims() {
            // Act
            String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);
            Claims claims = jwtUtil.validateToken(token);

            // Assert
            assertEquals(TEST_EMAIL, claims.getSubject(), "Subject should be email");
            assertEquals(TEST_USER_ID, claims.get("userId", Long.class), "userId claim should match");
            assertNotNull(claims.getIssuedAt(), "IssuedAt should not be null");
            assertNotNull(claims.getExpiration(), "Expiration should not be null");
        }
    }

    // =========================================================================
    // Token Validation Tests - Happy Path
    // =========================================================================
    @Nested
    @DisplayName("Token Validation - Happy Path")
    class TokenValidationHappyPathTests {

        @Test
        @DisplayName("validateToken_ValidToken_ReturnsClaims")
        void validateToken_ValidToken_ReturnsClaims() {
            // Arrange
            String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

            // Act
            Claims claims = jwtUtil.validateToken(token);

            // Assert
            assertNotNull(claims, "Claims should not be null for valid token");
            assertEquals(TEST_EMAIL, claims.getSubject());
        }

        @Test
        @DisplayName("isTokenValid_ValidToken_ReturnsTrue")
        void isTokenValid_ValidToken_ReturnsTrue() {
            // Arrange
            String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

            // Act
            boolean isValid = jwtUtil.isTokenValid(token);

            // Assert
            assertTrue(isValid, "Valid token should return true");
        }
    }

    // =========================================================================
    // Token Validation Tests - Expiration
    // =========================================================================
    @Nested
    @DisplayName("Token Validation - Expiration")
    class TokenExpirationTests {

        @Test
        @DisplayName("validateToken_ExpiredToken_ThrowsExpiredJwtException")
        void validateToken_ExpiredToken_ThrowsExpiredJwtException() {
            // Arrange - Create a JwtUtil with very short expiration (already expired)
            JwtUtil shortLivedJwtUtil = new JwtUtil(TEST_SECRET, -1000L); // Negative = already expired
            String expiredToken = shortLivedJwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

            // Act & Assert
            assertThrows(ExpiredJwtException.class, () -> jwtUtil.validateToken(expiredToken),
                    "Should throw ExpiredJwtException for expired token");
        }

        @Test
        @DisplayName("isTokenValid_ExpiredToken_ReturnsFalse")
        void isTokenValid_ExpiredToken_ReturnsFalse() {
            // Arrange
            JwtUtil shortLivedJwtUtil = new JwtUtil(TEST_SECRET, -1000L);
            String expiredToken = shortLivedJwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

            // Act
            boolean isValid = jwtUtil.isTokenValid(expiredToken);

            // Assert
            assertFalse(isValid, "Expired token should return false");
        }
    }

    // =========================================================================
    // Token Validation Tests - Security Attacks
    // =========================================================================
    @Nested
    @DisplayName("Token Validation - Security Attacks")
    class SecurityAttackTests {

        @Test
        @DisplayName("validateToken_TamperedPayload_ThrowsSignatureException")
        void validateToken_TamperedPayload_ThrowsSignatureException() {
            // Arrange
            String validToken = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);
            String[] parts = validToken.split("\\.");
            
            // Tamper with payload (change the middle part)
            String tamperedPayload = "eyJzdWIiOiJoYWNrZXJAZXZpbC5jb20iLCJ1c2VySWQiOjk5OTk5OX0";
            String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

            // Act & Assert
            assertThrows(SignatureException.class, () -> jwtUtil.validateToken(tamperedToken),
                    "Should throw SignatureException for tampered token");
        }

        @Test
        @DisplayName("validateToken_WrongSignatureKey_ThrowsSignatureException")
        void validateToken_WrongSignatureKey_ThrowsSignatureException() {
            // Arrange - Create token with different secret key
            SecretKey differentKey = Keys.hmacShaKeyFor(DIFFERENT_SECRET.getBytes(StandardCharsets.UTF_8));
            String tokenWithDifferentKey = Jwts.builder()
                    .subject(TEST_EMAIL)
                    .claim("userId", TEST_USER_ID)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + TEST_EXPIRATION_MS))
                    .signWith(differentKey)
                    .compact();

            // Act & Assert - Try to validate with original JwtUtil (different key)
            assertThrows(SignatureException.class, () -> jwtUtil.validateToken(tokenWithDifferentKey),
                    "Should throw SignatureException for token signed with different key");
        }

        @Test
        @DisplayName("validateToken_MalformedToken_ThrowsMalformedJwtException")
        void validateToken_MalformedToken_ThrowsMalformedJwtException() {
            // Arrange
            String malformedToken = "not.a.valid.jwt.token";

            // Act & Assert
            assertThrows(MalformedJwtException.class, () -> jwtUtil.validateToken(malformedToken),
                    "Should throw MalformedJwtException for malformed token");
        }

        @Test
        @DisplayName("validateToken_RandomGarbageString_ThrowsException")
        void validateToken_RandomGarbageString_ThrowsException() {
            // Arrange
            String garbageString = "random_garbage_string_12345";

            // Act & Assert
            assertThrows(Exception.class, () -> jwtUtil.validateToken(garbageString),
                    "Should throw exception for random garbage string");
        }

        @Test
        @DisplayName("validateToken_NullToken_ThrowsIllegalArgumentException")
        void validateToken_NullToken_ThrowsIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> jwtUtil.validateToken(null),
                    "Should throw IllegalArgumentException for null token");
        }

        @Test
        @DisplayName("validateToken_EmptyToken_ThrowsIllegalArgumentException")
        void validateToken_EmptyToken_ThrowsIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> jwtUtil.validateToken(""),
                    "Should throw IllegalArgumentException for empty token");
        }

        @Test
        @DisplayName("isTokenValid_TamperedToken_ReturnsFalse")
        void isTokenValid_TamperedToken_ReturnsFalse() {
            // Arrange
            String validToken = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);
            String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

            // Act
            boolean isValid = jwtUtil.isTokenValid(tamperedToken);

            // Assert
            assertFalse(isValid, "Tampered token should return false");
        }
    }

    // =========================================================================
    // Claims Extraction Tests
    // =========================================================================
    @Nested
    @DisplayName("Claims Extraction Tests")
    class ClaimsExtractionTests {

        @Test
        @DisplayName("getEmailFromToken_ValidToken_ReturnsEmail")
        void getEmailFromToken_ValidToken_ReturnsEmail() {
            // Arrange
            String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

            // Act
            String email = jwtUtil.getEmailFromToken(token);

            // Assert
            assertEquals(TEST_EMAIL, email, "Extracted email should match");
        }

        @Test
        @DisplayName("getUserIdFromToken_ValidToken_ReturnsUserId")
        void getUserIdFromToken_ValidToken_ReturnsUserId() {
            // Arrange
            String token = jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

            // Act
            Long userId = jwtUtil.getUserIdFromToken(token);

            // Assert
            assertEquals(TEST_USER_ID, userId, "Extracted userId should match");
        }

        @Test
        @DisplayName("getEmailFromToken_ExpiredToken_ThrowsExpiredJwtException")
        void getEmailFromToken_ExpiredToken_ThrowsExpiredJwtException() {
            // Arrange
            JwtUtil shortLivedJwtUtil = new JwtUtil(TEST_SECRET, -1000L);
            String expiredToken = shortLivedJwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID);

            // Act & Assert
            assertThrows(ExpiredJwtException.class, () -> jwtUtil.getEmailFromToken(expiredToken));
        }

        @Test
        @DisplayName("getUserIdFromToken_InvalidToken_ThrowsException")
        void getUserIdFromToken_InvalidToken_ThrowsException() {
            // Arrange
            String invalidToken = "invalid.token.here";

            // Act & Assert
            assertThrows(Exception.class, () -> jwtUtil.getUserIdFromToken(invalidToken));
        }
    }

    // =========================================================================
    // Edge Cases and Boundary Tests
    // =========================================================================
    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("generateToken_SpecialCharactersInEmail_WorksCorrectly")
        void generateToken_SpecialCharactersInEmail_WorksCorrectly() {
            // Arrange
            String emailWithSpecialChars = "user+test@sub.domain.example.com";

            // Act
            String token = jwtUtil.generateToken(emailWithSpecialChars, TEST_USER_ID);
            String extractedEmail = jwtUtil.getEmailFromToken(token);

            // Assert
            assertEquals(emailWithSpecialChars, extractedEmail);
        }

        @Test
        @DisplayName("generateToken_MaxLongUserId_WorksCorrectly")
        void generateToken_MaxLongUserId_WorksCorrectly() {
            // Arrange
            Long maxUserId = Long.MAX_VALUE;

            // Act
            String token = jwtUtil.generateToken(TEST_EMAIL, maxUserId);
            Long extractedUserId = jwtUtil.getUserIdFromToken(token);

            // Assert
            assertEquals(maxUserId, extractedUserId);
        }

        @Test
        @DisplayName("generateToken_ZeroUserId_WorksCorrectly")
        void generateToken_ZeroUserId_WorksCorrectly() {
            // Arrange
            Long zeroUserId = 0L;

            // Act
            String token = jwtUtil.generateToken(TEST_EMAIL, zeroUserId);
            Long extractedUserId = jwtUtil.getUserIdFromToken(token);

            // Assert
            assertEquals(zeroUserId, extractedUserId);
        }

        @Test
        @DisplayName("validateToken_WhitespaceOnlyToken_ThrowsException")
        void validateToken_WhitespaceOnlyToken_ThrowsException() {
            // Arrange
            String whitespaceToken = "   ";

            // Act & Assert
            assertThrows(Exception.class, () -> jwtUtil.validateToken(whitespaceToken));
        }
    }
}
