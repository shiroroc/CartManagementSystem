package com.eshoppingzone.profile.service;

import com.eshoppingzone.profile.dto.UserRequest;
import com.eshoppingzone.profile.dto.UserResponse;
import com.eshoppingzone.profile.entity.User;
import com.eshoppingzone.profile.exception.DuplicateResourceException;
import com.eshoppingzone.profile.exception.InvalidCredentialsException;
import com.eshoppingzone.profile.exception.ResourceNotFoundException;
import com.eshoppingzone.profile.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        logger.info("UserService initialized with BCrypt password encoder");
    }

    public List<UserResponse> getAllUsers() {
        logger.debug("Fetching all users");
        List<UserResponse> users = userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        logger.info("Retrieved {} users", users.size());
        return users;
    }

    public UserResponse getUserById(Long id) {
        logger.debug("Fetching user by id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("User not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
        logger.info("Found user with id: {}", id);
        return mapToResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
        logger.info("Found user with email: {}", email);
        return mapToResponse(user);
    }

    public UserResponse createUser(UserRequest request) {
        logger.info("Creating new user with email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }
        
        User user = mapToEntity(request);
        // Hash the password using BCrypt before saving
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        user.setPassword(hashedPassword);
        logger.debug("Password hashed successfully for user: {}", request.getEmail());
        
        User savedUser = userRepository.save(user);
        logger.info("User created successfully with id: {} and email: {}", savedUser.getId(), savedUser.getEmail());
        
        return mapToResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        logger.info("Updating user with id: {}", id);
        
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Update failed - user not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
        
        if (!existingUser.getEmail().equals(request.getEmail()) 
                && userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Update failed - email already in use: {}", request.getEmail());
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        existingUser.setFullName(request.getFullName());
        existingUser.setEmail(request.getEmail());
        
        // Hash password only if provided and changed
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            String hashedPassword = passwordEncoder.encode(request.getPassword());
            existingUser.setPassword(hashedPassword);
            logger.debug("Password updated and hashed for user: {}", id);
        }
        
        User updatedUser = userRepository.save(existingUser);
        logger.info("User updated successfully with id: {}", id);
        
        return mapToResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        logger.info("Deleting user with id: {}", id);
        
        if (!userRepository.existsById(id)) {
            logger.warn("Delete failed - user not found with id: {}", id);
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        
        userRepository.deleteById(id);
        logger.info("User deleted successfully with id: {}", id);
    }

    /**
     * Authenticates a user with email and password.
     * Used by the login endpoint.
     *
     * @param email User's email address
     * @param rawPassword User's plain text password
     * @return User entity if authentication successful
     * @throws InvalidCredentialsException if credentials are invalid
     */
    public User authenticate(String email, String rawPassword) {
        logger.info("Authentication attempt for email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Authentication failed - user not found with email: {}", email);
                    return new InvalidCredentialsException("Invalid email or password");
                });
        
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            logger.warn("Authentication failed - invalid password for email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }
        
        logger.info("Authentication successful for email: {}", email);
        return user;
    }

    /**
     * Verifies a user's password for re-authentication during sensitive operations.
     * Used for checkout and wallet operations that require password confirmation.
     *
     * @param userId User's ID
     * @param rawPassword User's plain text password
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(Long userId, String rawPassword) {
        logger.info("Password verification attempt for userId: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Password verification failed - user not found with id: {}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });
        
        boolean matches = passwordEncoder.matches(rawPassword, user.getPassword());
        
        if (matches) {
            logger.info("Password verification successful for userId: {}", userId);
        } else {
            logger.warn("Password verification failed for userId: {}", userId);
        }
        
        return matches;
    }

    // =========================================================================
    // FUTURE ENHANCEMENT: OAuth2 User Linking (Phase 2)
    // =========================================================================
    // public User findOrCreateOAuth2User(OAuth2User oauth2User, String provider) {
    //     String email = oauth2User.getAttribute("email");
    //     String name = oauth2User.getAttribute("name");
    //     
    //     return userRepository.findByEmail(email)
    //         .orElseGet(() -> {
    //             User newUser = new User();
    //             newUser.setEmail(email);
    //             newUser.setFullName(name);
    //             newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
    //             newUser.setOauth2Provider(provider);
    //             return userRepository.save(newUser);
    //         });
    // }
    // =========================================================================

    // =========================================================================
    // FUTURE ENHANCEMENT: 2FA/TOTP Support (Phase 2)
    // =========================================================================
    // public void enableTwoFactorAuth(Long userId, String totpSecret) {
    //     User user = userRepository.findById(userId)
    //         .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    //     user.setTotpSecret(encrypt(totpSecret));
    //     user.setTwoFactorEnabled(true);
    //     userRepository.save(user);
    // }
    //
    // public boolean verifyTotpCode(Long userId, int totpCode) {
    //     User user = userRepository.findById(userId)
    //         .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    //     if (!user.isTwoFactorEnabled()) return true;
    //     
    //     GoogleAuthenticator gAuth = new GoogleAuthenticator();
    //     return gAuth.authorize(decrypt(user.getTotpSecret()), totpCode);
    // }
    // =========================================================================

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        return response;
    }

    private User mapToEntity(UserRequest request) {
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        // Password is NOT set here - it's hashed separately in createUser()
        return user;
    }

}
