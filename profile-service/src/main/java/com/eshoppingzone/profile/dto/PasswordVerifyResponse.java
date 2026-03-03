package com.eshoppingzone.profile.dto;

/**
 * DTO for password verification response.
 */
public class PasswordVerifyResponse {

    private boolean valid;
    private String message;

    public PasswordVerifyResponse() {
    }

    public PasswordVerifyResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static PasswordVerifyResponse success() {
        return new PasswordVerifyResponse(true, "Password verified successfully");
    }

    public static PasswordVerifyResponse failure() {
        return new PasswordVerifyResponse(false, "Invalid password");
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "PasswordVerifyResponse{" +
                "valid=" + valid +
                ", message='" + message + '\'' +
                '}';
    }

}
