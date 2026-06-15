package vn.uth.careercompass.kernel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.kernel.entity.ActivityLog;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.ActivityLogRepository;

@Service
@RequiredArgsConstructor
public class ActivityLogService {
    private final ActivityLogRepository activityLogRepository;

    public void log(User user, String type, String description){
        ActivityLog log = ActivityLog.builder()
                .user(user)
                .type(type)
                .description(description)
                .build();
        activityLogRepository.save(log);
    }
}
