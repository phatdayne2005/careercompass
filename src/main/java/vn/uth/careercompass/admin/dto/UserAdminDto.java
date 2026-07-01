package vn.uth.careercompass.admin.dto;
 
import lombok.*;
import java.time.LocalDateTime;
 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAdminDto {
    private Long id;
    private String fullName;
    private String email;
    private String roleName;
    private String careerRoleName;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
