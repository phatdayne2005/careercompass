package vn.uth.careercompass.kernel.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.kernel.entity.ActivityLog;
import vn.uth.careercompass.kernel.entity.User;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserOrderByCreatedAtDesc(User user);
    Page<ActivityLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @Transactional
    void deleteByUser(User user);
}
