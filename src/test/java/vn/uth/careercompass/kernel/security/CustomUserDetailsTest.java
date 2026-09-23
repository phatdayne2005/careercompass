package vn.uth.careercompass.kernel.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.uth.careercompass.kernel.entity.Role;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử lớp bọc người dùng cho Spring Security (FR6.1, NFR-S03).
 *
 * <p>Lớp này mỏng nhưng là chỗ Spring Security đọc để quyết định CHO VÀO hay CHẶN, và
 * quyền của người dùng được dựng ở đây bằng cách nối chuỗi {@code "ROLE_" + tên vai trò}.
 * Sai tiền tố đó thì mọi phép kiểm {@code hasRole(...)} trong SecurityConfig đều trượt,
 * mà lỗi sẽ hiện ra dưới dạng "403 ở khắp nơi" chứ không chỉ thẳng vào đây.
 *
 * <p>Ba phương thức {@code isAccountNonExpired}, {@code isAccountNonLocked} và
 * {@code isCredentialsNonExpired} đang trả về hằng {@code true}: hệ thống chưa có khái
 * niệm hết hạn tài khoản, chỉ có cờ bật/tắt qua {@code isEnabled}. Phép kiểm dưới đây ghi
 * lại quyết định đó — nếu sau này thêm tính năng khoá tài khoản theo thời hạn thì chính
 * những dòng này phải đổi, và người sửa sẽ thấy ngay.
 */
@DisplayName("FR6.1 — Lớp bọc người dùng cho Spring Security")
class CustomUserDetailsTest {

    private static User nguoiDung(RoleName vaiTro, boolean bat) {
        return User.builder()
                .id(1L)
                .email("student@uth.edu.vn")
                .fullName("Nguyen Van A")
                .passwordHash("$2a$10$bam")
                .enabled(bat)
                .role(Role.builder().id(1L).name(vaiTro).build())
                .build();
    }

    @Test
    @DisplayName("Quyền được dựng theo đúng dạng ROLE_<VAI TRÒ> mà SecurityConfig chờ đợi")
    void quyen_dungTienToRole() {
        CustomUserDetails ud = new CustomUserDetails(nguoiDung(RoleName.STUDENT, true));

        assertThat(ud.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    @DisplayName("Mỗi vai trò cho ra đúng một quyền tương ứng")
    void quyen_theoTungVaiTro() {
        for (RoleName vaiTro : RoleName.values()) {
            assertThat(new CustomUserDetails(nguoiDung(vaiTro, true)).getAuthorities())
                    .as("vai trò %s", vaiTro)
                    .extracting(Object::toString)
                    .containsExactly("ROLE_" + vaiTro.name());
        }
    }

    @Test
    @DisplayName("Tên đăng nhập là email, mật khẩu là chuỗi băm chứ không phải bản rõ")
    void tenDangNhapVaMatKhau() {
        CustomUserDetails ud = new CustomUserDetails(nguoiDung(RoleName.STUDENT, true));

        assertThat(ud.getUsername()).isEqualTo("student@uth.edu.vn");
        assertThat(ud.getPassword())
                .as("NFR-S06: chỉ chuỗi băm BCrypt được đưa ra ngoài")
                .startsWith("$2a$");
    }

    @Test
    @DisplayName("Tài khoản Google không có mật khẩu thì getPassword trả null, không ném lỗi")
    void taiKhoanGoogle_khongCoMatKhau() {
        User google = User.builder().id(2L).email("g@gmail.com")
                .passwordHash(null).enabled(true)
                .role(Role.builder().id(1L).name(RoleName.STUDENT).build()).build();

        assertThat(new CustomUserDetails(google).getPassword()).isNull();
    }

    @Test
    @DisplayName("Cờ enabled quyết định đăng nhập được hay không")
    void coEnabled_quyetDinhDangNhap() {
        assertThat(new CustomUserDetails(nguoiDung(RoleName.STUDENT, true)).isEnabled()).isTrue();
        assertThat(new CustomUserDetails(nguoiDung(RoleName.STUDENT, false)).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Ba cờ hết hạn đều trả true — hệ thống chưa có khái niệm tài khoản hết hạn")
    void baCoHetHan_deuTra_true() {
        CustomUserDetails ud = new CustomUserDetails(nguoiDung(RoleName.STUDENT, true));

        // Nếu sau này thêm tính năng khoá theo thời hạn thì ba dòng này phải đổi.
        assertThat(ud.isAccountNonExpired()).isTrue();
        assertThat(ud.isAccountNonLocked()).isTrue();
        assertThat(ud.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Người dùng gốc lấy lại được để tầng trên không phải tra cứu CSDL thêm lần nữa")
    void layLaiDuocNguoiDungGoc() {
        User user = nguoiDung(RoleName.ADMIN, true);

        assertThat(new CustomUserDetails(user).getUser()).isSameAs(user);
    }
}
