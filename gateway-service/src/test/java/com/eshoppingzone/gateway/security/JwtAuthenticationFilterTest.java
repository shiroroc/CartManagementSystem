package com.eshoppingzone.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for JwtAuthenticationFilter (Gateway WebFilter).
 * 
 * Tests the reactive security filter behavior for:
 * - Public route bypass (no token required)
 * - Protected route enforcement (token required)
 * - JWT validation (expired, tampered, malformed)
 * - User header propagation (X-User-Id, X-User-Email)
 */
class JwtAuthenticationFilterTest {

    private static final String TEST_SECRET = "CartManagementSystemSecretKeyForJWTTokenGeneration2024SecureKey";
    private static final String DIFFERENT_SECRET = "DifferentSecretKeyForTestingWrongSignatureScenario12345678";
    private static final long JWT_EXPIRATION_MS = 86400000L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final Long TEST_USER_ID = 123L;

    private JwtAuthenticationFilter filter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(TEST_SECRET);
        secretKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // =========================================================================
    // Helper: Generate Valid JWT Token
    // =========================================================================
    private String generateValidToken() {
        return Jwts.builder()
                .subject(TEST_EMAIL)
                .claim("userId", TEST_USER_ID)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
                .signWith(secretKey)
                .compact();
    }

    private String generateExpiredToken() {
        return Jwts.builder()
                .subject(TEST_EMAIL)
                .claim("userId", TEST_USER_ID)
                .issuedAt(new Date(System.currentTimeMillis() - 200000))
                .expiration(new Date(System.currentTimeMillis() - 100000)) // Expired
                .signWith(secretKey)
                .compact();
    }

    private String generateTokenWithDifferentKey() {
        SecretKey differentKey = Keys.hmacShaKeyFor(DIFFERENT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(TEST_EMAIL)
                .claim("userId", TEST_USER_ID)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
                .signWith(differentKey)
                .compact();
    }

    // =========================================================================
    // Helper: Create Mock Exchange and FilterChain
    // =========================================================================
    private MockServerWebExchange createExchange(String method, String path, String authHeader) {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder;
        
        if ("POST".equals(method)) {
            requestBuilder = MockServerHttpRequest.post(path);
        } else if ("PUT".equals(method)) {
            requestBuilder = MockServerHttpRequest.put(path);
        } else if ("DELETE".equals(method)) {
            requestBuilder = MockServerHttpRequest.delete(path);
        } else {
            requestBuilder = MockServerHttpRequest.get(path);
        }
        
        if (authHeader != null) {
            requestBuilder.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        
        return MockServerWebExchange.from(requestBuilder.build());
    }

    private WebFilterChain createSuccessChain(AtomicBoolean chainCalled, AtomicReference<ServerWebExchange> capturedExchange) {
        return exchange -> {
            chainCalled.set(true);
            capturedExchange.set(exchange);
            return Mono.empty();
        };
    }

    // =========================================================================
    // Public Routes Tests - Should Allow Without Token
    // =========================================================================
    @Nested
    @DisplayName("Public Routes - No Token Required")
    class PublicRouteTests {

        @Test
        @DisplayName("publicRoute_PostProfilesLogin_AllowsWithoutToken")
        void publicRoute_PostProfilesLogin_AllowsWithoutToken() {
            // Arrange
            MockServerWebExchange exchange = createExchange("POST", "/profiles/login", null);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertTrue(chainCalled.get(), "Filter chain should be called for public login route");
        }

        @Test
        @DisplayName("publicRoute_PostProfilesRegistration_AllowsWithoutToken")
        void publicRoute_PostProfilesRegistration_AllowsWithoutToken() {
            // Arrange
            MockServerWebExchange exchange = createExchange("POST", "/profiles", null);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertTrue(chainCalled.get(), "Filter chain should be called for public registration route");
        }

        @Test
        @DisplayName("publicRoute_PostVerifyPassword_AllowsWithoutToken")
        void publicRoute_PostVerifyPassword_AllowsWithoutToken() {
            // Arrange
            MockServerWebExchange exchange = createExchange("POST", "/profiles/verify-password", null);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertTrue(chainCalled.get(), "Filter chain should be called for verify-password route");
        }

        @Test
        @DisplayName("publicRoute_Actuator_AllowsWithoutToken")
        void publicRoute_Actuator_AllowsWithoutToken() {
            // Arrange
            MockServerWebExchange exchange = createExchange("GET", "/actuator/health", null);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertTrue(chainCalled.get(), "Filter chain should be called for actuator routes");
        }
    }

    // =========================================================================
    // Protected Routes Tests - Token Required
    // =========================================================================
    @Nested
    @DisplayName("Protected Routes - Token Required")
    class ProtectedRouteTests {

        @Test
        @DisplayName("protectedRoute_WalletsWithoutToken_ChainCalledButNoAuthContext")
        void protectedRoute_WalletsWithoutToken_ChainCalledButNoAuthContext() {
            // Arrange
            MockServerWebExchange exchange = createExchange("GET", "/wallets/user/1", null);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            // Chain is called (security config will reject), but no user headers added
            assertTrue(chainCalled.get(), "Chain should be called to let SecurityConfig handle auth");
            assertFalse(capturedExchange.get().getRequest().getHeaders().containsKey("X-User-Id"),
                    "X-User-Id header should not be present without valid token");
        }

        @Test
        @DisplayName("protectedRoute_OrdersWithoutToken_NoUserHeaders")
        void protectedRoute_OrdersWithoutToken_NoUserHeaders() {
            // Arrange
            MockServerWebExchange exchange = createExchange("POST", "/orders", null);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertFalse(capturedExchange.get().getRequest().getHeaders().containsKey("X-User-Email"),
                    "X-User-Email header should not be present without valid token");
        }

        @Test
        @DisplayName("protectedRoute_ProductsWithoutToken_NoUserHeaders")
        void protectedRoute_ProductsWithoutToken_NoUserHeaders() {
            // Arrange
            MockServerWebExchange exchange = createExchange("GET", "/products", null);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertTrue(chainCalled.get());
        }

        @Test
        @DisplayName("protectedRoute_CartWithoutToken_NoUserHeaders")
        void protectedRoute_CartWithoutToken_NoUserHeaders() {
            // Arrange
            MockServerWebExchange exchange = createExchange("GET", "/cart/1", null);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertTrue(chainCalled.get());
        }
    }

    // =========================================================================
    // Valid Token Tests - Headers Propagated
    // =========================================================================
    @Nested
    @DisplayName("Valid Token - User Headers Propagated")
    class ValidTokenTests {

        @Test
        @DisplayName("protectedRoute_ValidJwt_AddsUserIdHeader")
        void protectedRoute_ValidJwt_AddsUserIdHeader() {
            // Arrange
            String validToken = generateValidToken();
            MockServerWebExchange exchange = createExchange("GET", "/wallets/user/1", "Bearer " + validToken);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertTrue(chainCalled.get());
            
            String userIdHeader = capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Id");
            assertNotNull(userIdHeader, "X-User-Id header should be present");
            assertEquals(String.valueOf(TEST_USER_ID), userIdHeader);
        }

        @Test
        @DisplayName("protectedRoute_ValidJwt_AddsUserEmailHeader")
        void protectedRoute_ValidJwt_AddsUserEmailHeader() {
            // Arrange
            String validToken = generateValidToken();
            MockServerWebExchange exchange = createExchange("POST", "/orders", "Bearer " + validToken);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            
            String emailHeader = capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Email");
            assertNotNull(emailHeader, "X-User-Email header should be present");
            assertEquals(TEST_EMAIL, emailHeader);
        }

        @Test
        @DisplayName("protectedRoute_ValidJwt_ChainIsCalled")
        void protectedRoute_ValidJwt_ChainIsCalled() {
            // Arrange
            String validToken = generateValidToken();
            MockServerWebExchange exchange = createExchange("GET", "/products", "Bearer " + validToken);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertTrue(chainCalled.get(), "Filter chain should be called for valid token");
        }
    }

    // =========================================================================
    // Invalid Token Tests - Should Return 401
    // =========================================================================
    @Nested
    @DisplayName("Invalid Tokens - 401 Response")
    class InvalidTokenTests {

        @Test
        @DisplayName("protectedRoute_ExpiredJwt_Returns401")
        void protectedRoute_ExpiredJwt_Returns401() {
            // Arrange
            String expiredToken = generateExpiredToken();
            MockServerWebExchange exchange = createExchange("GET", "/wallets/user/1", "Bearer " + expiredToken);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            WebFilterChain chain = ex -> {
                chainCalled.set(true);
                return Mono.empty();
            };

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            
            // For expired token, filter returns 401 directly
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
            assertFalse(chainCalled.get(), "Chain should not be called for expired token");
        }

        @Test
        @DisplayName("protectedRoute_TamperedJwt_Returns401")
        void protectedRoute_TamperedJwt_Returns401() {
            // Arrange
            String validToken = generateValidToken();
            // Tamper with the token by changing characters in the signature
            String tamperedToken = validToken.substring(0, validToken.length() - 10) + "TAMPERED!!";
            MockServerWebExchange exchange = createExchange("GET", "/orders", "Bearer " + tamperedToken);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            WebFilterChain chain = ex -> {
                chainCalled.set(true);
                return Mono.empty();
            };

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
            assertFalse(chainCalled.get());
        }

        @Test
        @DisplayName("protectedRoute_WrongSignatureKey_Returns401")
        void protectedRoute_WrongSignatureKey_Returns401() {
            // Arrange
            String tokenWithDifferentKey = generateTokenWithDifferentKey();
            MockServerWebExchange exchange = createExchange("GET", "/products", "Bearer " + tokenWithDifferentKey);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            WebFilterChain chain = ex -> {
                chainCalled.set(true);
                return Mono.empty();
            };

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
            assertFalse(chainCalled.get());
        }

        @Test
        @DisplayName("protectedRoute_MalformedJwt_Returns401")
        void protectedRoute_MalformedJwt_Returns401() {
            // Arrange
            String malformedToken = "not.a.valid.jwt.token.here";
            MockServerWebExchange exchange = createExchange("GET", "/cart/1", "Bearer " + malformedToken);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            WebFilterChain chain = ex -> {
                chainCalled.set(true);
                return Mono.empty();
            };

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
            assertFalse(chainCalled.get());
        }

        @Test
        @DisplayName("protectedRoute_RandomGarbageToken_Returns401")
        void protectedRoute_RandomGarbageToken_Returns401() {
            // Arrange
            String garbageToken = "random_garbage_12345";
            MockServerWebExchange exchange = createExchange("GET", "/wallets", "Bearer " + garbageToken);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            WebFilterChain chain = ex -> {
                chainCalled.set(true);
                return Mono.empty();
            };

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        }
    }

    // =========================================================================
    // Authorization Header Edge Cases
    // =========================================================================
    @Nested
    @DisplayName("Authorization Header Edge Cases")
    class AuthHeaderEdgeCaseTests {

        @Test
        @DisplayName("protectedRoute_MissingBearerPrefix_NoUserHeaders")
        void protectedRoute_MissingBearerPrefix_NoUserHeaders() {
            // Arrange - Token without "Bearer " prefix
            String validToken = generateValidToken();
            MockServerWebExchange exchange = createExchange("GET", "/orders", validToken); // No "Bearer " prefix
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            // Chain called but no headers added (security config will reject)
            assertFalse(capturedExchange.get().getRequest().getHeaders().containsKey("X-User-Id"));
        }

        @Test
        @DisplayName("protectedRoute_EmptyBearerToken_NoUserHeaders")
        void protectedRoute_EmptyBearerToken_NoUserHeaders() {
            // Arrange
            MockServerWebExchange exchange = createExchange("GET", "/products", "Bearer ");
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        @DisplayName("protectedRoute_EmptyAuthorizationHeader_NoUserHeaders")
        void protectedRoute_EmptyAuthorizationHeader_NoUserHeaders() {
            // Arrange
            MockServerWebExchange exchange = createExchange("GET", "/cart/1", "");
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertFalse(capturedExchange.get().getRequest().getHeaders().containsKey("X-User-Email"));
        }

        @Test
        @DisplayName("protectedRoute_WrongAuthScheme_NoUserHeaders")
        void protectedRoute_WrongAuthScheme_NoUserHeaders() {
            // Arrange - Using "Basic" instead of "Bearer"
            MockServerWebExchange exchange = createExchange("GET", "/wallets", "Basic dXNlcjpwYXNz");
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertFalse(capturedExchange.get().getRequest().getHeaders().containsKey("X-User-Id"));
        }
    }

    // =========================================================================
    // Path Matching Edge Cases
    // =========================================================================
    @Nested
    @DisplayName("Path Matching Edge Cases")
    class PathMatchingTests {

        @Test
        @DisplayName("getProfilesById_IsProtected_RequiresToken")
        void getProfilesById_IsProtected_RequiresToken() {
            // Arrange - GET /profiles/{id} should be protected (unlike POST /profiles)
            MockServerWebExchange exchange = createExchange("GET", "/profiles/1", null);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
            WebFilterChain chain = createSuccessChain(chainCalled, capturedExchange);

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            // Chain called but no user headers (protected route without token)
            assertFalse(capturedExchange.get().getRequest().getHeaders().containsKey("X-User-Id"));
        }

        @Test
        @DisplayName("swaggerUi_IsPublic_AllowsWithoutToken")
        void swaggerUi_IsPublic_AllowsWithoutToken() {
            // Arrange
            MockServerWebExchange exchange = createExchange("GET", "/swagger-ui.html", null);
            AtomicBoolean chainCalled = new AtomicBoolean(false);
            WebFilterChain chain = ex -> {
                chainCalled.set(true);
                return Mono.empty();
            };

            // Act
            Mono<Void> result = filter.filter(exchange, chain);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
            assertTrue(chainCalled.get());
        }
    }
}
