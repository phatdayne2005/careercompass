package vn.uth.careercompass.marketpulse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_trends")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTrend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nguồn tin: LINKEDIN, TOPCV... */
    @NotNull
    @Column(nullable = false, length = 50)
    private String source;

    @NotNull
    @Column(nullable = false, length = 255)
    private String jobTitle;

    @Column(length = 150)
    private String company;

    /** Toàn bộ text JD gốc — để KeywordAnalysisService đếm tần suất từ khoá. */
    @Column(length = 4000)
    private String rawDescription;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}