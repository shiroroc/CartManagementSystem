package com.eshoppingzone.profile.dto;

/**
 * DTO for login response containing JWT token and user info.
 */
public class LoginResponse {

    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private String tokenType;
    private long expiresIn;

    public LoginResponse() {
        this.tokenType = "Bearer";
    }

    public LoginResponse(String token, Long userId, String email, String fullName, long expiresIn) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "token='[PROTECTED]'" +
                ", userId=" + userId +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                '}';
    }

}
