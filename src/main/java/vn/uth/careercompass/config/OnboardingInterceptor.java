package vn.uth.careercompass.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;

/**
 * Interceptor bắt mọi request của STUDENT chưa hoàn thành onboarding
 * và redirect về /onboarding/step1.
 *
 * Không áp dụng với:
 *  - Các route /onboarding/** (để không bị vòng lặp)
 *  - /login, /logout, /register, /css/**, /js/**, /oauth2/**  (public resources)
 *  - /admin/**, /counselor/**  (role khác không cần onboarding)
 *  - API / error paths
 */
@Component
@RequiredArgsConstructor
public class OnboardingInterceptor implements HandlerInterceptor {

    private final AuthenticatedUserService authenticatedUserService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        // Bỏ qua các path không cần check
        if (shouldSkip(uri)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;  // Chưa đăng nhập → Spring Security tự xử lý
        }

        // Chỉ áp dụng cho STUDENT
        boolean isStudent = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()));
        if (!isStudent) {
            return true;
        }

        // Kiểm tra cờ onboardingCompleted
        try {
            User user = authenticatedUserService.requireCurrentUser(auth);
            if (!Boolean.TRUE.equals(user.getOnboardingCompleted())) {
                response.sendRedirect(request.getContextPath() + "/onboarding/step1");
                return false;
            }
        } catch (Exception e) {
            // Không thể load user → để Spring Security xử lý
            return true;
        }

        return true;
    }

    private boolean shouldSkip(String uri) {
        return uri.startsWith("/onboarding")
                || uri.startsWith("/login")
                || uri.startsWith("/logout")
                || uri.startsWith("/register")
                || uri.startsWith("/forgot")
                || uri.startsWith("/oauth2")
                || uri.startsWith("/css")
                || uri.startsWith("/js")
                || uri.startsWith("/images")
                || uri.startsWith("/uploads")
                || uri.startsWith("/admin")
                || uri.startsWith("/counselor")
                || uri.startsWith("/error")
                || uri.startsWith("/favicon");
    }
}
