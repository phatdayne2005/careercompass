package vn.uth.careercompass.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình Spring MVC:
 * 1. Đăng ký OnboardingInterceptor để redirect STUDENT chưa onboard.
 * 2. Expose thư mục uploads/ làm static resource (để hiển thị transcript đã upload).
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final OnboardingInterceptor onboardingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(onboardingInterceptor)
                .addPathPatterns("/**")
                // Bỏ qua các path public và các path của chính onboarding
                .excludePathPatterns(
                        "/onboarding/**",
                        "/login", "/logout", "/register", "/forgot",
                        "/oauth2/**",
                        "/css/**", "/js/**", "/images/**",
                        "/uploads/**",
                        "/admin/**", "/counselor/**",
                        "/error", "/favicon.ico"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose thư mục uploads/ (transcript files) dưới URL /uploads/**
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
