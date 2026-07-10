package org.example.boilerserver.config;

import org.example.boilerserver.interceptor.AdminAuthInterceptor;
import org.example.boilerserver.interceptor.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Objects;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final HandlerInterceptor jwtAuthInterceptor;
    private final HandlerInterceptor adminAuthInterceptor;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor, AdminAuthInterceptor adminAuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(Objects.requireNonNull(jwtAuthInterceptor))
                .addPathPatterns(
                        "/user/profile/me",
                        "/user/profile",
                        "/user/seller-profile/me",
                        "/user/seller-profile",
                        "/user/seller-profile/files",
                        "/user/admin/**",
                        "/post/admin/**"
                );
        registry.addInterceptor(Objects.requireNonNull(adminAuthInterceptor))
                .addPathPatterns("/user/admin/**", "/post/admin/**");
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
