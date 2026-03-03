package com.eshoppingzone.profile.controller;

import com.eshoppingzone.profile.dto.LoginRequest;
import com.eshoppingzone.profile.dto.PasswordVerifyRequest;
import com.eshoppingzone.profile.entity.User;
import com.eshoppingzone.profile.exception.GlobalExceptionHandler;
import com.eshoppingzone.profile.exception.InvalidCredentialsException;
import com.eshoppingzone.profile.exception.ResourceNotFoundException;
import com.eshoppingzone.profile.security.JwtUtil;
import com.eshoppingzone.profile.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for AuthController covering:
 * - Login endpoint (success, failures, validation)
 * - Password verification endpoint
 * - Input validation handling
 * - Security attack attempts
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String LOGIN_URL = "/profiles/login";
    private static final String VERIFY_PASSWORD_URL = "/profiles/verify-password";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "securePassword123";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_FULL_NAME = "Test User";
    private static final String TEST_JWT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.test.token";
    private static final long JWT_EXPIRATION = 86400000L;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(userService, jwtUtil, JWT_EXPIRATION);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    // =========================================================================
    // Login Endpoint - Success Cases
    // =========================================================================
    @Nested
    @DisplayName("Login - Success Cases")
    class LoginSuccessTests {

        @Test
        @DisplayName("login_ValidCredentials_Returns200WithJwtAndUserInfo")
        void login_ValidCredentials_Returns200WithJwtAndUserInfo() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            User authenticatedUser = createUser(TEST_USER_ID, TEST_FULL_NAME, TEST_EMAIL);
            
            when(userService.authenticate(TEST_EMAIL, TEST_PASSWORD)).thenReturn(authenticatedUser);
            when(jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID)).thenReturn(TEST_JWT_TOKEN);

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", is(TEST_JWT_TOKEN)))
                    .andExpect(jsonPath("$.userId", is(TEST_USER_ID.intValue())))
                    .andExpect(jsonPath("$.email", is(TEST_EMAIL)))
                    .andExpect(jsonPath("$.fullName", is(TEST_FULL_NAME)))
                    .andExpect(jsonPath("$.expiresIn", is(86400))); // In seconds
        }

        @Test
        @DisplayName("login_ValidCredentials_ResponseContainsAllRequiredFields")
        void login_ValidCredentials_ResponseContainsAllRequiredFields() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
            User authenticatedUser = createUser(TEST_USER_ID, TEST_FULL_NAME, TEST_EMAIL);
            
            when(userService.authenticate(TEST_EMAIL, TEST_PASSWORD)).thenReturn(authenticatedUser);
            when(jwtUtil.generateToken(TEST_EMAIL, TEST_USER_ID)).thenReturn(TEST_JWT_TOKEN);

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.userId", notNullValue()))
                    .andExpect(jsonPath("$.email", notNullValue()))
                    .andExpect(jsonPath("$.fullName", notNullValue()))
                    .andExpect(jsonPath("$.expiresIn", notNullValue()));
        }
    }

    // =========================================================================
    // Login Endpoint - Authentication Failures
    // =========================================================================
    @Nested
    @DisplayName("Login - Authentication Failures")
    class LoginAuthFailureTests {

        @Test
        @DisplayName("login_WrongPassword_Returns401Unauthorized")
        void login_WrongPassword_Returns401Unauthorized() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongPassword");
            when(userService.authenticate(TEST_EMAIL, "wrongPassword"))
                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.error", is("Unauthorized")))
                    .andExpect(jsonPath("$.message", is("Invalid email or password")));
        }

        @Test
        @DisplayName("login_NonExistentEmail_Returns401Unauthorized")
        void login_NonExistentEmail_Returns401Unauthorized() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("nonexistent@example.com", TEST_PASSWORD);
            when(userService.authenticate("nonexistent@example.com", TEST_PASSWORD))
                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", is("Invalid email or password")));
        }
    }

    // =========================================================================
    // Login Endpoint - Input Validation Failures
    // =========================================================================
    @Nested
    @DisplayName("Login - Input Validation")
    class LoginValidationTests {

        @Test
        @DisplayName("login_MissingEmail_Returns400BadRequest")
        void login_MissingEmail_Returns400BadRequest() throws Exception {
            // Arrange - Email is null
            String requestBody = "{\"password\":\"" + TEST_PASSWORD + "\"}";

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")));
        }

        @Test
        @DisplayName("login_EmptyEmail_Returns400BadRequest")
        void login_EmptyEmail_Returns400BadRequest() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("", TEST_PASSWORD);

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")));
        }

        @Test
        @DisplayName("login_InvalidEmailFormat_Returns400BadRequest")
        void login_InvalidEmailFormat_Returns400BadRequest() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("invalid-email-format", TEST_PASSWORD);

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                    .andExpect(jsonPath("$.fieldErrors[*].message", 
                            hasItem("Email must be a valid email address")));
        }

        @Test
        @DisplayName("login_MissingPassword_Returns400BadRequest")
        void login_MissingPassword_Returns400BadRequest() throws Exception {
            // Arrange
            String requestBody = "{\"email\":\"" + TEST_EMAIL + "\"}";

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
        }

        @Test
        @DisplayName("login_EmptyPassword_Returns400BadRequest")
        void login_EmptyPassword_Returns400BadRequest() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest(TEST_EMAIL, "");

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
        }

        @Test
        @DisplayName("login_BothFieldsMissing_Returns400WithMultipleErrors")
        void login_BothFieldsMissing_Returns400WithMultipleErrors() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("", "");

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors", hasSize(org.hamcrest.Matchers.greaterThan(1))));
        }

        @Test
        @DisplayName("login_MalformedJson_Returns400BadRequest")
        void login_MalformedJson_Returns400BadRequest() throws Exception {
            // Arrange
            String malformedJson = "{\"email\": \"test@example.com\", \"password\": }";

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // Login Endpoint - Security Attack Attempts
    // =========================================================================
    @Nested
    @DisplayName("Login - Security Attacks")
    class LoginSecurityTests {

        @Test
        @DisplayName("login_SqlInjectionInEmail_Returns401NotExploit")
        void login_SqlInjectionInEmail_Returns401NotExploit() throws Exception {
            // Arrange - SQL injection attempt
            String sqlInjectionEmail = "' OR '1'='1' --";
            LoginRequest request = new LoginRequest(sqlInjectionEmail, TEST_PASSWORD);
            
            // The email format validation will catch most SQL injection attempts first
            // But even if it passes, authentication should fail normally

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()); // Invalid email format
        }

        @Test
        @DisplayName("login_SqlInjectionInPassword_Returns401NotExploit")
        void login_SqlInjectionInPassword_Returns401NotExploit() throws Exception {
            // Arrange
            String sqlInjectionPassword = "'; DROP TABLE users; --";
            LoginRequest request = new LoginRequest(TEST_EMAIL, sqlInjectionPassword);
            
            when(userService.authenticate(TEST_EMAIL, sqlInjectionPassword))
                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", is("Invalid email or password")));
        }

        @Test
        @DisplayName("login_XssInEmail_Returns400InvalidFormat")
        void login_XssInEmail_Returns400InvalidFormat() throws Exception {
            // Arrange
            String xssEmail = "<script>alert('xss')</script>@evil.com";

            // Act & Assert
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + xssEmail + "\",\"password\":\"test\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("login_OversizedPayload_Returns400BadRequest")
        void login_OversizedPayload_Returns400BadRequest() throws Exception {
            // Arrange - Very long password (potential buffer overflow attempt)
            StringBuilder longPassword = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                longPassword.append("a");
            }
            LoginRequest request = new LoginRequest(TEST_EMAIL, longPassword.toString());

            // Act & Assert - Should fail validation or authentication, not crash
            MvcResult result = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn();
            
            int status = result.getResponse().getStatus();
            assertTrue(status == 400 || status == 401, 
                    "Oversized payload should result in 400 or 401, not server error");
        }
    }

    // =========================================================================
    // Password Verification Endpoint Tests
    // =========================================================================
    @Nested
    @DisplayName("Password Verification Tests")
    class PasswordVerificationTests {

        @Test
        @DisplayName("verifyPassword_ValidData_ReturnsTrue")
        void verifyPassword_ValidData_ReturnsTrue() throws Exception {
            // Arrange
            PasswordVerifyRequest request = new PasswordVerifyRequest();
            request.setUserId(TEST_USER_ID);
            request.setPassword(TEST_PASSWORD);
            
            when(userService.verifyPassword(TEST_USER_ID, TEST_PASSWORD)).thenReturn(true);

            // Act & Assert
            mockMvc.perform(post(VERIFY_PASSWORD_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid", is(true)));
        }

        @Test
        @DisplayName("verifyPassword_WrongPassword_ReturnsFalse")
        void verifyPassword_WrongPassword_ReturnsFalse() throws Exception {
            // Arrange
            PasswordVerifyRequest request = new PasswordVerifyRequest();
            request.setUserId(TEST_USER_ID);
            request.setPassword("wrongPassword");
            
            when(userService.verifyPassword(TEST_USER_ID, "wrongPassword")).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post(VERIFY_PASSWORD_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid", is(false)));
        }

        @Test
        @DisplayName("verifyPassword_MissingUserId_Returns400BadRequest")
        void verifyPassword_MissingUserId_Returns400BadRequest() throws Exception {
            // Arrange
            String requestBody = "{\"password\":\"" + TEST_PASSWORD + "\"}";

            // Act & Assert
            mockMvc.perform(post(VERIFY_PASSWORD_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("userId")));
        }

        @Test
        @DisplayName("verifyPassword_MissingPassword_Returns400BadRequest")
        void verifyPassword_MissingPassword_Returns400BadRequest() throws Exception {
            // Arrange
            String requestBody = "{\"userId\":" + TEST_USER_ID + "}";

            // Act & Assert
            mockMvc.perform(post(VERIFY_PASSWORD_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
        }

        @Test
        @DisplayName("verifyPassword_NonExistentUser_Returns404NotFound")
        void verifyPassword_NonExistentUser_Returns404NotFound() throws Exception {
            // Arrange
            PasswordVerifyRequest request = new PasswordVerifyRequest();
            request.setUserId(999L);
            request.setPassword(TEST_PASSWORD);
            
            when(userService.verifyPassword(999L, TEST_PASSWORD))
                    .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

            // Act & Assert
            mockMvc.perform(post(VERIFY_PASSWORD_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("User not found")));
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private User createUser(Long id, String fullName, String email) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword("hashedPassword"); // Password hash is not needed for these tests
        return user;
    }
}
