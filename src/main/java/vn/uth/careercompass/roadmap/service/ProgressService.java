package vn.uth.careercompass.roadmap.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.repository.SkillNodeRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.ActivityLogService;
import vn.uth.careercompass.roadmap.RoadmapActivityType;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProgressService {
    private final SkillNodeRepository skillNodeRepository;
    private final UserNodeProgressRepository userNodeProgressRepository;
    private final ActivityLogService activityLogService;
    private final RoadmapService roadmapService;

    @Transactional
    public UserNodeProgress updateProgress(User user, Long skillNodeId, ProgressStatus status) {
        if (skillNodeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu skillNodeId");
        }
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu trạng thái tiến độ");
        }

        SkillNode skillNode = skillNodeRepository.findById(skillNodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy node kỹ năng"));

        // Gating: không cho đánh dấu đang học/hoàn thành khi node còn bị khóa (tầng trước chưa xong).
        if (!ProgressStatus.NOT_STARTED.equals(status) && roadmapService.isNodeLocked(user, skillNode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cần hoàn thành các node ở tầng trước để mở khóa node này");
        }

        UserNodeProgress progress = userNodeProgressRepository.findByUserAndSkillNode(user, skillNode)
                .orElseGet(() -> UserNodeProgress.builder()
                        .user(user)
                        .skillNode(skillNode)
                        .build());

        progress.setStatus(status);
        progress.setCompletedAt(ProgressStatus.DONE.equals(status) ? LocalDateTime.now() : null);
        UserNodeProgress saved = userNodeProgressRepository.save(progress);

        // Ghi hoạt động để Dashboard (P5) đọc "hoạt động gần đây"
        if (ProgressStatus.DONE.equals(status)) {
            activityLogService.log(user, RoadmapActivityType.NODE_DONE,
                    "Hoàn thành kỹ năng: " + skillNode.getSkill().getName());
        }
        return saved;
    }
}
