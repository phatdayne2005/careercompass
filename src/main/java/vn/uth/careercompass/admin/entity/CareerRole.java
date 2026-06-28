package vn.uth.careercompass.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "career_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String expectedSalaryRange;

    @Column(length = 50)
    private String marketDemand;

}
