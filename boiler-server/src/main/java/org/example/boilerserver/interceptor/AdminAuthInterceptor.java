package org.example.boilerserver.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.boilerserver.auth.AuthContext;
import org.example.boilerserver.service.UserService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private final UserService userService;

    public AdminAuthInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String adminUserId = AuthContext.getRequiredUserId();
        if (!userService.isAdmin(adminUserId)) {
            throw new IllegalArgumentException("仅管理员可执行该操作");
        }
        return true;
    }
}
