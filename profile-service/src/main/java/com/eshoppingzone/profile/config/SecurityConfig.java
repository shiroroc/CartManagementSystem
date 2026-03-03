package com.eshoppingzone.profile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Security configuration for Profile Service.
 * This service acts as the Identity Provider for the system.
 * 
 * Note: No Spring Security filter chain is configured here as this service
 * trusts the Gateway to filter unauthorized requests.
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt password encoder bean for hashing passwords.
     * Uses default strength of 10 rounds.
     * 
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =========================================================================
    // FUTURE ENHANCEMENT: OAuth2 Configuration (Phase 2)
    // =========================================================================
    // To add Google OAuth2 login support, add the following dependencies:
    // - spring-boot-starter-oauth2-client
    // 
    // Then configure:
    // @Bean
    // public ClientRegistrationRepository clientRegistrationRepository() {
    //     return new InMemoryClientRegistrationRepository(googleClientRegistration());
    // }
    //
    // private ClientRegistration googleClientRegistration() {
    //     return ClientRegistration.withRegistrationId("google")
    //         .clientId("${GOOGLE_CLIENT_ID}")
    //         .clientSecret("${GOOGLE_CLIENT_SECRET}")
    //         .scope("openid", "profile", "email")
    //         .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
    //         .tokenUri("https://www.googleapis.com/oauth2/v4/token")
    //         .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
    //         .userNameAttributeName("sub")
    //         .redirectUri("{baseUrl}/profiles/oauth2/callback/google")
    //         .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
    //         .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
    //         .build();
    // }
    // =========================================================================

}
