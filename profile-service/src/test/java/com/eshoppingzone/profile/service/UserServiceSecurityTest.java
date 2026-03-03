package com.eshoppingzone.profile.service;

import com.eshoppingzone.profile.dto.UserRequest;
import com.eshoppingzone.profile.entity.User;
import com.eshoppingzone.profile.exception.InvalidCredentialsException;
import com.eshoppingzone.profile.exception.ResourceNotFoundException;
import com.eshoppingzone.profile.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Security-focused tests for UserService covering:
 * - BCrypt password hashing on registration
 * - Authentication (login) scenarios
 * - Password verification
 * - Security edge cases (SQL injection attempts, null inputs)
 */
@ExtendWith(MockitoExtension.class)
class UserServiceSecurityTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String WRONG_PASSWORD = "WrongPassword456!";
    private static final Long TEST_USER_ID = 1L;

    // BCrypt hash pattern: $2a$, $2b$, or $2y$ followed by cost factor and hash
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    @Mock
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    // =========================================================================
    // BCrypt Password Hashing Tests (Registration)
    // =========================================================================
    @Nested
    @DisplayName("BCrypt Password Hashing Tests")
    class BCryptHashingTests {

        @Test
        @DisplayName("createUser_StoresHashedPassword_NotPlaintext")
        void createUser_StoresHashedPassword_NotPlaintext() {
            // Arrange
            UserRequest request = createUserRequest(TEST_EMAIL, TEST_PASSWORD);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(TEST_USER_ID);
                return user;
            });

            // Act
            userService.createUser(request);

            // Assert - Capture the saved user and verify password is hashed
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            
            User savedUser = userCaptor.getValue();
            String storedPassword = savedUser.getPassword();
            
            // Password should NOT be plaintext
            assertNotEquals(TEST_PASSWORD, storedPassword, 
                    "Password should not be stored as plaintext");
            
            // Password should match BCrypt pattern
            assertTrue(BCRYPT_PATTERN.matcher(storedPassword).matches(),
                    "Password should be hashed with BCrypt");
        }

        @Test
        @DisplayName("createUser_PasswordHashIsDifferentEachTime_SaltVerification")
        void createUser_PasswordHashIsDifferentEachTime_SaltVerification() {
            // Arrange - Same password used twice
            UserRequest request1 = createUserRequest("user1@example.com", TEST_PASSWORD);
            UserRequest request2 = createUserRequest("user2@example.com", TEST_PASSWORD);
            
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(TEST_USER_ID);
                return user;
            });

            // Act
            userService.createUser(request1);
            userService.createUser(request2);

            // Assert - Capture both saved users
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, org.mockito.Mockito.times(2)).save(userCaptor.capture());
            
            java.util.List<User> savedUsers = userCaptor.getAllValues();
            String hash1 = savedUsers.get(0).getPassword();
            String hash2 = savedUsers.get(1).getPassword();
            
            // Hashes should be different due to random salt
            assertNotEquals(hash1, hash2, 
                    "BCrypt should generate different hashes for same password (salt verification)");
        }

        @Test
        @DisplayName("createUser_HashedPasswordCanBeVerified")
        void createUser_HashedPasswordCanBeVerified() {
            // Arrange
            UserRequest request = createUserRequest(TEST_EMAIL, TEST_PASSWORD);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(TEST_USER_ID);
                return user;
            });

            // Act
            userService.createUser(request);

            // Assert - Verify BCrypt can match original password with stored hash
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            
            String storedHash = userCaptor.getValue().getPassword();
            assertTrue(passwordEncoder.matches(TEST_PASSWORD, storedHash),
                    "BCrypt should be able to verify the original password against stored hash");
        }
    }

    // =========================================================================
    // Authentication (Login) Tests
    // =========================================================================
    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("authenticate_ValidCredentials_ReturnsUser")
        void authenticate_ValidCredentials_ReturnsUser() {
            // Arrange
            User existingUser = createUser(TEST_USER_ID, TEST_EMAIL, 
                    passwordEncoder.encode(TEST_PASSWORD));
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

            // Act
            User result = userService.authenticate(TEST_EMAIL, TEST_PASSWORD);

            // Assert
            assertNotNull(result, "Should return user for valid credentials");
            assertEquals(TEST_USER_ID, result.getId());
            assertEquals(TEST_EMAIL, result.getEmail());
        }

        @Test
        @DisplayName("authenticate_WrongPassword_ThrowsInvalidCredentialsException")
        void authenticate_WrongPassword_ThrowsInvalidCredentialsException() {
            // Arrange
            User existingUser = createUser(TEST_USER_ID, TEST_EMAIL, 
                    passwordEncoder.encode(TEST_PASSWORD));
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

            // Act & Assert
            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.authenticate(TEST_EMAIL, WRONG_PASSWORD)
            );
            
            assertEquals("Invalid email or password", exception.getMessage());
        }

        @Test
        @DisplayName("authenticate_NonExistentEmail_ThrowsInvalidCredentialsException")
        void authenticate_NonExistentEmail_ThrowsInvalidCredentialsException() {
            // Arrange
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // Act & Assert
            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.authenticate("nonexistent@example.com", TEST_PASSWORD)
            );
            
            assertEquals("Invalid email or password", exception.getMessage());
        }

        @Test
        @DisplayName("authenticate_NullEmail_ThrowsException")
        void authenticate_NullEmail_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(InvalidCredentialsException.class,
                    () -> userService.authenticate(null, TEST_PASSWORD));
        }

        @Test
        @DisplayName("authenticate_NullPassword_ThrowsException")
        void authenticate_NullPassword_ThrowsException() {
            // Arrange
            User existingUser = createUser(TEST_USER_ID, TEST_EMAIL, 
                    passwordEncoder.encode(TEST_PASSWORD));
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

            // Act & Assert
            assertThrows(Exception.class,
                    () -> userService.authenticate(TEST_EMAIL, null));
        }

        @Test
        @DisplayName("authenticate_EmptyPassword_ThrowsInvalidCredentialsException")
        void authenticate_EmptyPassword_ThrowsInvalidCredentialsException() {
            // Arrange
            User existingUser = createUser(TEST_USER_ID, TEST_EMAIL, 
                    passwordEncoder.encode(TEST_PASSWORD));
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

            // Act & Assert
            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.authenticate(TEST_EMAIL, "")
            );
            
            assertEquals("Invalid email or password", exception.getMessage());
        }
    }

    // =========================================================================
    // Password Verification Tests
    // =========================================================================
    @Nested
    @DisplayName("Password Verification Tests")
    class PasswordVerificationTests {

        @Test
        @DisplayName("verifyPassword_CorrectPassword_ReturnsTrue")
        void verifyPassword_CorrectPassword_ReturnsTrue() {
            // Arrange
            User existingUser = createUser(TEST_USER_ID, TEST_EMAIL, 
                    passwordEncoder.encode(TEST_PASSWORD));
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(existingUser));

            // Act
            boolean result = userService.verifyPassword(TEST_USER_ID, TEST_PASSWORD);

            // Assert
            assertTrue(result, "Should return true for correct password");
        }

        @Test
        @DisplayName("verifyPassword_WrongPassword_ReturnsFalse")
        void verifyPassword_WrongPassword_ReturnsFalse() {
            // Arrange
            User existingUser = createUser(TEST_USER_ID, TEST_EMAIL, 
                    passwordEncoder.encode(TEST_PASSWORD));
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(existingUser));

            // Act
            boolean result = userService.verifyPassword(TEST_USER_ID, WRONG_PASSWORD);

            // Assert
            assertFalse(result, "Should return false for wrong password");
        }

        @Test
        @DisplayName("verifyPassword_NonExistentUser_ThrowsResourceNotFoundException")
        void verifyPassword_NonExistentUser_ThrowsResourceNotFoundException() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> userService.verifyPassword(999L, TEST_PASSWORD));
        }

        @Test
        @DisplayName("verifyPassword_NullUserId_ThrowsException")
        void verifyPassword_NullUserId_ThrowsException() {
            // Act & Assert
            assertThrows(Exception.class,
                    () -> userService.verifyPassword(null, TEST_PASSWORD));
        }
    }

    // =========================================================================
    // SQL Injection and Security Attack Tests
    // =========================================================================
    @Nested
    @DisplayName("Security Attack Tests")
    class SecurityAttackTests {

        @Test
        @DisplayName("authenticate_SqlInjectionInEmail_DoesNotExploit")
        void authenticate_SqlInjectionInEmail_DoesNotExploit() {
            // Arrange - SQL injection attempt in email
            String sqlInjectionEmail = "' OR '1'='1' --";
            when(userRepository.findByEmail(sqlInjectionEmail)).thenReturn(Optional.empty());

            // Act & Assert - Should throw InvalidCredentialsException, not return all users
            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.authenticate(sqlInjectionEmail, TEST_PASSWORD)
            );
            
            assertEquals("Invalid email or password", exception.getMessage());
            
            // Verify the repository was called with the literal injection string
            verify(userRepository).findByEmail(sqlInjectionEmail);
        }

        @Test
        @DisplayName("authenticate_SqlInjectionInPassword_DoesNotExploit")
        void authenticate_SqlInjectionInPassword_DoesNotExploit() {
            // Arrange
            String sqlInjectionPassword = "'; DROP TABLE users; --";
            User existingUser = createUser(TEST_USER_ID, TEST_EMAIL, 
                    passwordEncoder.encode(TEST_PASSWORD));
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));

            // Act & Assert - Should fail authentication normally
            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> userService.authenticate(TEST_EMAIL, sqlInjectionPassword)
            );
            
            assertEquals("Invalid email or password", exception.getMessage());
        }

        @Test
        @DisplayName("createUser_XssInFullName_StoredAsLiteralString")
        void createUser_XssInFullName_StoredAsLiteralString() {
            // Arrange
            String xssPayload = "<script>alert('XSS')</script>";
            UserRequest request = new UserRequest();
            request.setFullName(xssPayload);
            request.setEmail(TEST_EMAIL);
            request.setPassword(TEST_PASSWORD);
            
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(TEST_USER_ID);
                return user;
            });

            // Act
            userService.createUser(request);

            // Assert - XSS payload should be stored as literal string (escaped by frontend)
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            
            assertEquals(xssPayload, userCaptor.getValue().getFullName(),
                    "XSS payload should be stored as literal string for proper escaping at render time");
        }
    }

    // =========================================================================
    // Password Update Tests
    // =========================================================================
    @Nested
    @DisplayName("Password Update Tests")
    class PasswordUpdateTests {

        @Test
        @DisplayName("updateUser_NewPassword_IsHashedBeforeSave")
        void updateUser_NewPassword_IsHashedBeforeSave() {
            // Arrange
            String newPassword = "NewSecurePassword456!";
            User existingUser = createUser(TEST_USER_ID, TEST_EMAIL, 
                    passwordEncoder.encode(TEST_PASSWORD));
            
            UserRequest updateRequest = createUserRequest(TEST_EMAIL, newPassword);
            
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            userService.updateUser(TEST_USER_ID, updateRequest);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            
            String updatedPasswordHash = userCaptor.getValue().getPassword();
            
            // Verify new password is hashed (not plaintext)
            assertNotEquals(newPassword, updatedPasswordHash);
            assertTrue(BCRYPT_PATTERN.matcher(updatedPasswordHash).matches());
            
            // Verify new password can be matched
            assertTrue(passwordEncoder.matches(newPassword, updatedPasswordHash));
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private UserRequest createUserRequest(String email, String password) {
        UserRequest request = new UserRequest();
        request.setFullName("Test User");
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private User createUser(Long id, String email, String hashedPassword) {
        User user = new User();
        user.setId(id);
        user.setFullName("Test User");
        user.setEmail(email);
        user.setPassword(hashedPassword);
        return user;
    }
}
