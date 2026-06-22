package org.example.boilerserver.auth;

public class AuthUser {
    private final String userId;
    private final String userType;

    public AuthUser(String userId, String userType) {
        this.userId = userId;
        this.userType = userType;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserType() {
        return userType;
    }
}
