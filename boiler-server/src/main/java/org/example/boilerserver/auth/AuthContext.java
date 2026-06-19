package org.example.boilerserver.auth;

public final class AuthContext {
    private static final ThreadLocal<AuthUser> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void setCurrentUser(AuthUser authUser) {
        CURRENT_USER.set(authUser);
    }

    public static AuthUser getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static String getRequiredUserId() {
        AuthUser authUser = getCurrentUser();
        if (authUser == null) {
            throw new IllegalArgumentException("当前请求未完成认证");
        }
        return authUser.getUserId();
    }

    public static String getRequiredUserType() {
        AuthUser authUser = getCurrentUser();
        if (authUser == null) {
            throw new IllegalArgumentException("当前请求未完成认证");
        }
        return authUser.getUserType();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
