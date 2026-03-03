package com.eshoppingzone.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for password verification request.
 * Used for re-authentication during sensitive operations like checkout.
 */
public class PasswordVerifyRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Password is required")
    private String password;

    public PasswordVerifyRequest() {
    }

    public PasswordVerifyRequest(Long userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "PasswordVerifyRequest{" +
                "userId=" + userId +
                ", password='[PROTECTED]'" +
                '}';
    }

}
