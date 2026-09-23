package vn.uth.careercompass.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử cổng chặn ép hoàn tất onboarding (FR6.3).
 *
 * Trước lớp này OnboardingInterceptor mới phủ 45% nhánh — thấp nhất dự án. Lý do là nó
 * gần như toàn nhánh: mười bốn tiền tố URL được bỏ qua, ba trạng thái đăng nhập, phép thử
 * vai trò, cờ onboarding và một khối bắt ngoại lệ. Chạy qua HTTP thật thì mỗi nhánh tốn
 * một request; ở đây là logic thuần nên kiểm thẳng, vài mili giây một nhánh.
 *
 * Hai nhánh đáng chú ý vì sai là hỏng thật:
 *   - Thiếu một tiền tố trong shouldSkip là sinh viên chưa onboarding sẽ bị đá khỏi trang
 *     đó; riêng "/onboarding" mà thiếu thì thành vòng lặp chuyển hướng vô hạn.
 *   - Cờ onboardingCompleted để null (tài khoản tạo trước khi thêm cột) phải bị coi là
 *     CHƯA hoàn thành. Viết !user.getOnboardingCompleted() thay vì !Boolean.TRUE.equals(…)
 *     sẽ ném NullPointerException.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FR6.3 — Cổng chặn ép hoàn tất onboarding")
class OnboardingInterceptorTest {

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private OnboardingInterceptor interceptor;

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void donDep() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest request(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRequestURI(uri);
        return r;
    }

    private static void dangNhap(String... quyen) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student@uth.edu.vn", "n/a",
                java.util.Arrays.stream(quyen).map(SimpleGrantedAuthority::new).toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static User sinhVien(Boolean daHoanTat) {
        return User.builder().id(1L).email("student@uth.edu.vn")
                .onboardingCompleted(daHoanTat).build();
    }

    // ============ Nhánh 1: danh sách tiền tố được bỏ qua ============

    @ParameterizedTest(name = "{0} đi thẳng, không bị chặn")
    @ValueSource(strings = {
            "/onboarding/step1", "/login", "/logout", "/register", "/forgot",
            "/oauth2/authorization/google", "/css/app.css", "/js/app.js",
            "/images/logo.png", "/uploads/transcripts/a.pdf",
            "/admin/users", "/counselor/templates", "/error", "/favicon.ico"
    })
    @DisplayName("Mười bốn tiền tố trong danh sách trắng đều không bị chặn")
    void cacTienToDuocBoQua(String uri) throws Exception {
        // Cố tình dựng tình huống ĐÁNG LẼ bị chặn: sinh viên chưa hoàn tất onboarding.
        dangNhap("ROLE_STUDENT");
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(sinhVien(false));

        boolean choDiTiep = interceptor.preHandle(request(uri), response, new Object());

        assertThat(choDiTiep).isTrue();
        assertThat(response.getRedirectedUrl()).isNull();
        // Bỏ qua sớm thì không được chạm tới CSDL.
        verify(authenticatedUserService, never()).requireCurrentUser(any());
    }

    @Test
    @DisplayName("Đường dẫn chỉ CHỨA tiền tố ở giữa thì vẫn bị chặn")
    void tienToPhaiONgayDau() throws Exception {
        dangNhap("ROLE_STUDENT");
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(sinhVien(false));

        // "/mentor/login" có chữ login nhưng không BẮT ĐẦU bằng /login.
        boolean choDiTiep = interceptor.preHandle(request("/mentor/login"), response, new Object());

        assertThat(choDiTiep).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/onboarding/step1");
    }

    // ============ Nhánh 2: trạng thái đăng nhập ============

    @Test
    @DisplayName("Chưa đăng nhập thì để Spring Security tự xử lý")
    void chuaDangNhap_khongChan() throws Exception {
        SecurityContextHolder.clearContext();

        assertThat(interceptor.preHandle(request("/dashboard"), response, new Object())).isTrue();
        assertThat(response.getRedirectedUrl()).isNull();
    }

    @Test
    @DisplayName("Người dùng ẩn danh thì để Spring Security tự xử lý")
    void nguoiDungAnDanh_khongChan() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(interceptor.preHandle(request("/dashboard"), response, new Object())).isTrue();
        assertThat(response.getRedirectedUrl()).isNull();
        verify(authenticatedUserService, never()).requireCurrentUser(any());
    }

    @Test
    @DisplayName("Phiên chưa xác thực thì để Spring Security tự xử lý")
    void phienChuaXacThuc_khongChan() throws Exception {
        UsernamePasswordAuthenticationToken chuaXacThuc =
                new UsernamePasswordAuthenticationToken("student@uth.edu.vn", "n/a");
        SecurityContextHolder.getContext().setAuthentication(chuaXacThuc);

        assertThat(chuaXacThuc.isAuthenticated()).isFalse();
        assertThat(interceptor.preHandle(request("/dashboard"), response, new Object())).isTrue();
        assertThat(response.getRedirectedUrl()).isNull();
    }

    // ============ Nhánh 3: vai trò ============

    @Test
    @DisplayName("Quản trị viên không phải làm onboarding")
    void quanTriVien_khongBiChan() throws Exception {
        dangNhap("ROLE_ADMIN");

        assertThat(interceptor.preHandle(request("/market/pulse"), response, new Object())).isTrue();
        verify(authenticatedUserService, never()).requireCurrentUser(any());
    }

    @Test
    @DisplayName("Cố vấn học tập không phải làm onboarding")
    void coVan_khongBiChan() throws Exception {
        dangNhap("ROLE_COUNSELOR");

        assertThat(interceptor.preHandle(request("/market/pulse"), response, new Object())).isTrue();
        verify(authenticatedUserService, never()).requireCurrentUser(any());
    }

    @Test
    @DisplayName("Người dùng mang nhiều vai trò, có STUDENT thì vẫn bị chặn")
    void nhieuVaiTro_coStudent_vanBiChan() throws Exception {
        dangNhap("ROLE_COUNSELOR", "ROLE_STUDENT");
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(sinhVien(false));

        assertThat(interceptor.preHandle(request("/dashboard"), response, new Object())).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/onboarding/step1");
    }

    // ============ Nhánh 4: cờ hoàn tất onboarding ============

    @Test
    @DisplayName("Sinh viên đã hoàn tất thì đi tiếp bình thường")
    void daHoanTat_diTiep() throws Exception {
        dangNhap("ROLE_STUDENT");
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(sinhVien(true));

        assertThat(interceptor.preHandle(request("/dashboard"), response, new Object())).isTrue();
        assertThat(response.getRedirectedUrl()).isNull();
    }

    @Test
    @DisplayName("Sinh viên chưa hoàn tất bị đưa về bước 1")
    void chuaHoanTat_biChuyenHuong() throws Exception {
        dangNhap("ROLE_STUDENT");
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(sinhVien(false));

        assertThat(interceptor.preHandle(request("/dashboard"), response, new Object())).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/onboarding/step1");
    }

    @Test
    @DisplayName("Cờ để null cũng bị coi là CHƯA hoàn tất, không ném lỗi")
    void coNull_coiNhuChuaHoanTat() throws Exception {
        dangNhap("ROLE_STUDENT");
        // Tài khoản tạo trước khi thêm cột onboarding_completed thì cờ là null.
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(sinhVien(null));

        assertThat(interceptor.preHandle(request("/dashboard"), response, new Object())).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/onboarding/step1");
    }

    @Test
    @DisplayName("Chuyển hướng bám theo context path khi ứng dụng chạy dưới thư mục con")
    void chuyenHuong_bamTheoContextPath() throws Exception {
        dangNhap("ROLE_STUDENT");
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(sinhVien(false));
        MockHttpServletRequest req = request("/dashboard");
        req.setContextPath("/careercompass");

        interceptor.preHandle(req, response, new Object());

        assertThat(response.getRedirectedUrl()).isEqualTo("/careercompass/onboarding/step1");
    }

    // ============ Nhánh 5: không nạp được người dùng ============

    @Test
    @DisplayName("Không nạp được người dùng thì nhường Spring Security, không chặn oan")
    void khongNapDuocNguoiDung_khongChan() throws Exception {
        dangNhap("ROLE_STUDENT");
        when(authenticatedUserService.requireCurrentUser(any()))
                .thenThrow(new IllegalStateException("Không tìm thấy người dùng"));

        assertThat(interceptor.preHandle(request("/dashboard"), response, new Object())).isTrue();
        assertThat(response.getRedirectedUrl()).isNull();
    }
}
