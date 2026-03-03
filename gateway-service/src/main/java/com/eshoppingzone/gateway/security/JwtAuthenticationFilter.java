package com.eshoppingzone.gateway.security;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * JWT Authentication Filter for Spring Cloud Gateway (Reactive).
 * 
 * This filter intercepts all incoming requests and validates JWT tokens
 * for protected routes. Valid tokens result in authentication context being
 * set for downstream processing.
 * 
 * The filter also propagates user information to downstream services via
 * custom headers (X-User-Id, X-User-Email) so that downstream services
 * can identify the authenticated user without parsing the JWT themselves.
 */
@Component
public class JwtAuthenticationFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    private final SecretKey secretKey;

    /**
     * Routes that should bypass JWT validation.
     * These are handled by permitAll() in SecurityConfig but we also
     * skip token extraction for performance.
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/profiles/login",
            "/profiles/verify-password",
            "/actuator",
            "/eureka",
            "/swagger-ui",
            "/v3/api-docs"
    );

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        logger.info("JwtAuthenticationFilter initialized");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        logger.debug("Processing request: {} {}", method, path);

        // Skip JWT validation for public paths
        if (isPublicPath(path)) {
            // Special handling for POST /profiles (registration) - it's public
            if ("POST".equals(method) && "/profiles".equals(path)) {
                logger.debug("Allowing public registration request: POST /profiles");
                return chain.filter(exchange);
            }
            logger.debug("Skipping JWT validation for public path: {}", path);
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("Missing or invalid Authorization header for request: {} {}", method, path);
            // Let the security filter chain handle unauthorized response
            return chain.filter(exchange);
        }

        // Extract and validate JWT token
        String token = authHeader.substring(BEARER_PREFIX.length());
        
        try {
            Claims claims = validateToken(token);
            String email = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            
            logger.info("JWT validated successfully for user: {} (userId: {})", email, userId);

            // Create authentication object
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            // Mutate request to add user info headers for downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(HEADER_USER_ID, String.valueOf(userId))
                    .header(HEADER_USER_EMAIL, email)
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            logger.debug("Forwarding authenticated request with headers: X-User-Id={}, X-User-Email={}", 
                    userId, email);

            // Continue filter chain with security context
            return chain.filter(mutatedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        } catch (ExpiredJwtException ex) {
            logger.warn("JWT token has expired for request: {} {} - {}", method, path, ex.getMessage());
            return handleUnauthorized(exchange, "JWT token has expired");
        } catch (MalformedJwtException ex) {
            logger.warn("Malformed JWT token for request: {} {} - {}", method, path, ex.getMessage());
            return handleUnauthorized(exchange, "Malformed JWT token");
        } catch (SignatureException ex) {
            logger.warn("Invalid JWT signature for request: {} {} - {}", method, path, ex.getMessage());
            return handleUnauthorized(exchange, "Invalid JWT signature");
        } catch (UnsupportedJwtException ex) {
            logger.warn("Unsupported JWT token for request: {} {} - {}", method, path, ex.getMessage());
            return handleUnauthorized(exchange, "Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT claims string is empty for request: {} {} - {}", method, path, ex.getMessage());
            return handleUnauthorized(exchange, "Invalid JWT token");
        } catch (Exception ex) {
            logger.error("Unexpected error during JWT validation for request: {} {} - {}", 
                    method, path, ex.getMessage(), ex);
            return handleUnauthorized(exchange, "JWT validation failed");
        }
    }

    /**
     * Validates the JWT token and returns the claims.
     *
     * @param token JWT token string
     * @return Claims object containing token payload
     */
    private Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if the request path is a public path that doesn't require authentication.
     *
     * @param path Request path
     * @return true if the path is public
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Handles unauthorized requests by returning a 401 response.
     *
     * @param exchange Server web exchange
     * @param message Error message to include in response
     * @return Mono<Void> representing the response
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        
        String responseBody = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"}",
                java.time.LocalDateTime.now(),
                message
        );
        
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(responseBody.getBytes()))
        );
    }

}
