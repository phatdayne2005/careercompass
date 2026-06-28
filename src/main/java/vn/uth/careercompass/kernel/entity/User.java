package vn.uth.careercompass.kernel.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import vn.uth.careercompass.admin.entity.CareerRole;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(nullable = false, length = 100)
    private String fullName;
    @NotNull
    @Column(nullable = false, unique = true, length = 150)
    @Email
    private String email;
    @Column(nullable = true, length = 255)
    private String passwordHash;
    @NotNull
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    @NotNull
    private Role role;
    @Column(length = 100)
    private String githubUsername;
    @Column(length = 255)
    private String transcriptPath;
    private Double gpa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "career_role_id")
    private CareerRole careerRole;

    @Column(nullable = false)
    @NotNull
    @Builder.Default
    private Boolean enabled = true;
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
