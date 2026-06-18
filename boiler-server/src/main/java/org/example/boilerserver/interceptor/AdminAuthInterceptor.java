package org.example.boilerserver.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.boilerserver.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    public static final String ADMIN_HEADER = "X-Admin-User-Id";

    private final UserService userService;

    public AdminAuthInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String adminUserId = request.getHeader(ADMIN_HEADER);
        if (!StringUtils.hasText(adminUserId)) {
            throw new IllegalArgumentException("后台接口需要管理员身份，请在请求头中传递 X-Admin-User-Id");
        }
        if (!userService.isAdmin(adminUserId)) {
            throw new IllegalArgumentException("仅管理员可执行该操作");
        }
        return true;
    }
}
