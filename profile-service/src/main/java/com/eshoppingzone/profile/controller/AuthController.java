package com.eshoppingzone.profile.controller;

import com.eshoppingzone.profile.dto.LoginRequest;
import com.eshoppingzone.profile.dto.LoginResponse;
import com.eshoppingzone.profile.dto.PasswordVerifyRequest;
import com.eshoppingzone.profile.dto.PasswordVerifyResponse;
import com.eshoppingzone.profile.entity.User;
import com.eshoppingzone.profile.security.JwtUtil;
import com.eshoppingzone.profile.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller for user login and password verification.
 * Acts as the Identity Provider for the CartManagementSystem.
 */
@RestController
@RequestMapping("/profiles")
@Tag(name = "Authentication", description = "Authentication APIs for login and password verification")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final long jwtExpiration;

    public AuthController(UserService userService, 
                          JwtUtil jwtUtil,
                          @Value("${jwt.expiration}") long jwtExpiration) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.jwtExpiration = jwtExpiration;
        logger.info("AuthController initialized");
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request Login credentials (email and password)
     * @return JWT token and user information if authentication successful
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user and returns JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Login request received for email: {}", request.getEmail());
        
        // Authenticate user (throws InvalidCredentialsException if invalid)
        User user = userService.authenticate(request.getEmail(), request.getPassword());
        
        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        logger.info("JWT token generated successfully for user: {} (userId: {})", 
                user.getEmail(), user.getId());
        
        // Build response
        LoginResponse response = new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                jwtExpiration / 1000  // Convert to seconds for response
        );
        
        logger.info("Login successful for user: {}", user.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Verifies a user's password for re-authentication during sensitive operations.
     * This endpoint is used for checkout confirmation and wallet deduction workflows.
     *
     * @param request Password verification request (userId and password)
     * @return Verification result (valid/invalid)
     */
    @PostMapping("/verify-password")
    @Operation(summary = "Verify password", 
               description = "Re-authenticates user password for sensitive operations like checkout")
    public ResponseEntity<PasswordVerifyResponse> verifyPassword(
            @Valid @RequestBody PasswordVerifyRequest request) {
        logger.info("Password verification request received for userId: {}", request.getUserId());
        
        boolean isValid = userService.verifyPassword(request.getUserId(), request.getPassword());
        
        PasswordVerifyResponse response;
        if (isValid) {
            response = PasswordVerifyResponse.success();
            logger.info("Password verification successful for userId: {}", request.getUserId());
        } else {
            response = PasswordVerifyResponse.failure();
            logger.warn("Password verification failed for userId: {}", request.getUserId());
        }
        
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // FUTURE ENHANCEMENT: OAuth2 Endpoints (Phase 2)
    // =========================================================================
    // @GetMapping("/oauth2/google")
    // @Operation(summary = "Google OAuth2 login", description = "Initiates Google OAuth2 login flow")
    // public ResponseEntity<Void> googleOAuth2Login() {
    //     // Redirect to Google OAuth2 authorization endpoint
    //     // This will be handled by Spring Security OAuth2 Client
    //     return ResponseEntity.status(302)
    //         .header("Location", "/oauth2/authorization/google")
    //         .build();
    // }
    //
    // @GetMapping("/oauth2/callback/google")
    // @Operation(summary = "Google OAuth2 callback", description = "Handles Google OAuth2 callback")
    // public ResponseEntity<LoginResponse> googleOAuth2Callback(
    //         @AuthenticationPrincipal OAuth2User oauth2User) {
    //     // Find or create user from OAuth2 info
    //     User user = userService.findOrCreateOAuth2User(oauth2User, "google");
    //     
    //     // Generate JWT token
    //     String token = jwtUtil.generateToken(user.getEmail(), user.getId());
    //     
    //     return ResponseEntity.ok(new LoginResponse(token, user.getId(), 
    //         user.getEmail(), user.getFullName(), jwtExpiration / 1000));
    // }
    // =========================================================================

    // =========================================================================
    // FUTURE ENHANCEMENT: 2FA Endpoints (Phase 2)
    // =========================================================================
    // @PostMapping("/2fa/setup")
    // @Operation(summary = "Setup 2FA", description = "Generates TOTP secret for 2FA setup")
    // public ResponseEntity<TwoFactorSetupResponse> setup2FA(
    //         @RequestHeader("Authorization") String authHeader) {
    //     Long userId = extractUserIdFromToken(authHeader);
    //     String totpSecret = jwtUtil.generateTotpSecret();
    //     // Return QR code URL for Google Authenticator
    //     return ResponseEntity.ok(new TwoFactorSetupResponse(totpSecret, generateQrCodeUrl(totpSecret)));
    // }
    //
    // @PostMapping("/2fa/verify")
    // @Operation(summary = "Verify 2FA code", description = "Verifies TOTP code during login")
    // public ResponseEntity<LoginResponse> verify2FA(
    //         @Valid @RequestBody TwoFactorVerifyRequest request) {
    //     // Verify TOTP code and complete login
    //     if (userService.verifyTotpCode(request.getUserId(), request.getCode())) {
    //         String token = jwtUtil.generateToken(request.getEmail(), request.getUserId());
    //         return ResponseEntity.ok(new LoginResponse(token, ...));
    //     }
    //     throw new InvalidCredentialsException("Invalid 2FA code");
    // }
    // =========================================================================

}
