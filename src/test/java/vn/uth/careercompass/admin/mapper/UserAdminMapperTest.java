package vn.uth.careercompass.admin.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.uth.careercompass.admin.dto.UserAdminDto;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.kernel.entity.Role;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.entity.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử bộ chuyển đổi User sang UserAdminDto (FR7.2).
 *
 * Lớp chỉ có một phương thức nhưng chứa ba phép thử null, và trước đây mới phủ 50% nhánh.
 * Cả ba đều là null hợp lệ trong dữ liệu thật:
 *   - user == null khi tra id không tồn tại,
 *   - role == null với tài khoản tạo bằng tay trong CSDL chưa gán vai trò,
 *   - careerRole == null với sinh viên đã bấm "bỏ qua" ở bước 1 onboarding (FR2.1).
 *
 * Bỏ sót một nhánh ở đây là trang /admin/users ném NullPointerException cho TOÀN BỘ danh
 * sách chỉ vì một bản ghi thiếu dữ liệu.
 */
@DisplayName("FR7.2 — Chuyển đổi User sang DTO cho trang quản trị")
class UserAdminMapperTest {

    private static final LocalDateTime LUC_TAO = LocalDateTime.of(2026, 9, 1, 8, 30);

    @Test
    @DisplayName("Hồ sơ đầy đủ được chuyển đổi đúng từng trường")
    void hoSoDayDu_chuyenDoiDungTungTruong() {
        User user = User.builder()
                .id(1L)
                .fullName("Nguyen Van A")
                .email("a@uth.edu.vn")
                .role(Role.builder().id(1L).name(RoleName.STUDENT).build())
                .careerRole(CareerRole.builder().id(2L).name("Backend Developer").build())
                .enabled(true)
                .createdAt(LUC_TAO)
                .build();

        UserAdminDto dto = UserAdminMapper.toDto(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(dto.getEmail()).isEqualTo("a@uth.edu.vn");
        assertThat(dto.getRoleName()).isEqualTo("STUDENT");
        assertThat(dto.getCareerRoleName()).isEqualTo("Backend Developer");
        assertThat(dto.getEnabled()).isTrue();
        assertThat(dto.getCreatedAt()).isEqualTo(LUC_TAO);
    }

    @Test
    @DisplayName("User null thì trả null, không ném ngoại lệ")
    void userNull_traNull() {
        assertThat(UserAdminMapper.toDto(null)).isNull();
    }

    @Test
    @DisplayName("Chưa gán vai trò hệ thống thì roleName để trống, các trường khác vẫn đủ")
    void chuaGanVaiTro_roleNameNull() {
        User user = User.builder()
                .id(2L).fullName("Tran Thi B").email("b@uth.edu.vn")
                .role(null)
                .careerRole(CareerRole.builder().id(2L).name("Backend Developer").build())
                .enabled(true).createdAt(LUC_TAO).build();

        UserAdminDto dto = UserAdminMapper.toDto(user);

        assertThat(dto.getRoleName()).isNull();
        assertThat(dto.getCareerRoleName()).isEqualTo("Backend Developer");
        assertThat(dto.getEmail()).isEqualTo("b@uth.edu.vn");
    }

    @Test
    @DisplayName("Sinh viên bỏ qua bước chọn nghề thì careerRoleName để trống")
    void boQuaChonNghe_careerRoleNameNull() {
        User user = User.builder()
                .id(3L).fullName("Le Van C").email("c@uth.edu.vn")
                .role(Role.builder().id(1L).name(RoleName.STUDENT).build())
                .careerRole(null)
                .enabled(false).createdAt(LUC_TAO).build();

        UserAdminDto dto = UserAdminMapper.toDto(user);

        assertThat(dto.getCareerRoleName()).isNull();
        assertThat(dto.getRoleName()).isEqualTo("STUDENT");
        assertThat(dto.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("Thiếu cả hai vai trò cùng lúc vẫn chuyển đổi được")
    void thieuCaHaiVaiTro_vanChuyenDoiDuoc() {
        User user = User.builder()
                .id(4L).fullName("Pham Thi D").email("d@uth.edu.vn")
                .role(null).careerRole(null).enabled(true).createdAt(LUC_TAO).build();

        UserAdminDto dto = UserAdminMapper.toDto(user);

        assertThat(dto).isNotNull();
        assertThat(dto.getRoleName()).isNull();
        assertThat(dto.getCareerRoleName()).isNull();
        assertThat(dto.getId()).isEqualTo(4L);
    }
}
