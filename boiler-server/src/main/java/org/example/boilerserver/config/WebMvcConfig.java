package org.example.boilerserver.config;

import org.example.boilerserver.interceptor.AdminAuthInterceptor;
import org.example.boilerserver.interceptor.JwtAuthInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Configuration
@EnableConfigurationProperties({FileStorageProperties.class, JwtProperties.class})
public class WebMvcConfig implements WebMvcConfigurer {
    private final HandlerInterceptor jwtAuthInterceptor;
    private final HandlerInterceptor adminAuthInterceptor;
    private final FileStorageProperties fileStorageProperties;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor, AdminAuthInterceptor adminAuthInterceptor,
                        FileStorageProperties fileStorageProperties) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(Objects.requireNonNull(jwtAuthInterceptor))
                .addPathPatterns(
                        "/user/profile",
                        "/user/profile/me",
                        "/user/seller-profile",
                        "/user/seller-profile/me",
                        "/user/seller-profile/files",
                        "/user/admin/**"
                );
        registry.addInterceptor(Objects.requireNonNull(adminAuthInterceptor))
                .addPathPatterns("/user/admin/**");
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        Path uploadRoot = Paths.get(fileStorageProperties.getSellerQualificationDir()).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadRoot.getParent().toUri().toString());
    }
}
