package com.eshoppingzone.gateway.config;

import com.eshoppingzone.gateway.security.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Reactive Security Configuration for API Gateway.
 * 
 * This is the centralized security boundary for the entire microservices architecture.
 * All downstream services (order, wallet, product, cart) trust requests that pass through
 * this gateway and do NOT implement their own Spring Security.
 * 
 * Security Model:
 * - Public routes (whitelist): Login, Register, Actuator, Eureka
 * - Protected routes: All business endpoints require valid JWT
 * - CORS: Configured for frontend development (localhost:3000, 4200, 5173)
 * - CSRF: Disabled (stateless REST API with JWT authentication)
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        logger.info("SecurityConfig initialized with JWT authentication filter");
    }

    /**
     * Configures the reactive security filter chain.
     * 
     * Whitelist (no authentication required):
     * - POST /profiles (user registration)
     * - POST /profiles/login (authentication)
     * - /actuator/** (health checks and monitoring)
     * - /eureka/** (service discovery)
     * 
     * Protected (valid JWT required):
     * - All other routes
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        logger.info("Configuring reactive security filter chain");

        return http
                // Disable CSRF - not needed for stateless REST APIs with JWT
                .csrf(csrf -> csrf.disable())
                
                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Stateless session - no session storage needed
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                
                // Configure authorization rules
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints - no authentication required
                        .pathMatchers(HttpMethod.POST, "/profiles").permitAll()  // Registration
                        .pathMatchers(HttpMethod.POST, "/profiles/login").permitAll()  // Login
                        .pathMatchers(HttpMethod.POST, "/profiles/verify-password").permitAll()  // Password verification
                        .pathMatchers("/actuator/**").permitAll()  // Health checks
                        .pathMatchers("/eureka/**").permitAll()  // Service discovery
                        
                        // Swagger/OpenAPI documentation - publicly accessible
                        .pathMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        
                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                
                // Add custom JWT authentication filter before the authorization filter
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                
                // Handle authentication failures
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> {
                            logger.warn("Authentication failed for request: {} - {}", 
                                    exchange.getRequest().getPath(), ex.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory()
                                            .wrap("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing JWT token\"}".getBytes()))
                            );
                        })
                        .accessDeniedHandler((exchange, denied) -> {
                            logger.warn("Access denied for request: {} - {}", 
                                    exchange.getRequest().getPath(), denied.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().writeWith(
                                    Mono.just(exchange.getResponse().bufferFactory()
                                            .wrap("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}".getBytes()))
                            );
                        })
                )
                
                .build();
    }

    /**
     * Configures CORS for frontend applications.
     * 
     * Allowed origins (for development):
     * - localhost:3000 (React)
     * - localhost:4200 (Angular)
     * - localhost:5173 (Vite/Vue)
     * 
     * Production: Should be configured via environment variables
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("Configuring CORS for frontend applications");
        
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",   // React default
                "http://localhost:4200",   // Angular default
                "http://localhost:5173"    // Vite default
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);  // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        logger.info("CORS configured with allowed origins: {}", configuration.getAllowedOrigins());
        return source;
    }

}
