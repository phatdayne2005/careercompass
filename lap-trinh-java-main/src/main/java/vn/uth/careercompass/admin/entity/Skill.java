package vn.uth.careercompass.admin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * [STUB - thuộc gói P7 Admin/Counselor]
 * P1 tạo tạm để DB có bảng skills + cho UserSkill tham chiếu.
 * Khi đồng đội nhận P7, sẽ extend file này thêm các quan hệ với
 * SkillNode, SkillTreeTemplate. KHÔNG xoá hoặc move nếu chưa thông báo cho P7.
 */

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    @NotNull
    private String name;
    @Column(length = 50)
    private String category;
}
