package vn.uth.careercompass.mentor;

import vn.uth.careercompass.kernel.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "mentor_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MentorSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; 

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}