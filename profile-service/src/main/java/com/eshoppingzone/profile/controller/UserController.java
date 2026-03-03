package com.eshoppingzone.profile.controller;

import com.eshoppingzone.profile.dto.UserRequest;
import com.eshoppingzone.profile.dto.UserResponse;
import com.eshoppingzone.profile.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/profiles")
@Tag(name = "User Profile", description = "User Profile Management APIs")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
        logger.info("UserController initialized");
    }

    @GetMapping
    @Operation(summary = "Get all users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        logger.info("GET /profiles - Fetching all users");
        List<UserResponse> users = userService.getAllUsers();
        logger.info("Returning {} users", users.size());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        logger.info("GET /profiles/{} - Fetching user by ID", id);
        UserResponse user = userService.getUserById(id);
        logger.info("Found user: {}", user.getEmail());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email")
    @Operation(summary = "Get user by email")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        logger.info("GET /profiles/email?email={} - Fetching user by email", email);
        UserResponse user = userService.getUserByEmail(email);
        logger.info("Found user with id: {}", user.getId());
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @Operation(summary = "Create a new user (Register)")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        logger.info("POST /profiles - Creating new user with email: {}", request.getEmail());
        UserResponse createdUser = userService.createUser(request);
        logger.info("User created with id: {}", createdUser.getId());
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, 
                                                    @Valid @RequestBody UserRequest request) {
        logger.info("PUT /profiles/{} - Updating user", id);
        UserResponse updatedUser = userService.updateUser(id, request);
        logger.info("User updated: {}", updatedUser.getEmail());
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.info("DELETE /profiles/{} - Deleting user", id);
        userService.deleteUser(id);
        logger.info("User deleted with id: {}", id);
        return ResponseEntity.noContent().build();
    }

}
