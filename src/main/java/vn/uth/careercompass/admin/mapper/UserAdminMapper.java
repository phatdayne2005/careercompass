package vn.uth.careercompass.admin.mapper;
 
import vn.uth.careercompass.admin.dto.UserAdminDto;
import vn.uth.careercompass.kernel.entity.User;
 
public class UserAdminMapper {
    public static UserAdminDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return UserAdminDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roleName(user.getRole() != null ? user.getRole().getName().name() : null)
                .careerRoleName(user.getCareerRole() != null ? user.getCareerRole().getName() : null)
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
