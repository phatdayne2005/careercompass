package vn.uth.careercompass.marketpulse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.marketpulse.entity.JobTrend;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobTrendRepository extends JpaRepository<JobTrend, Long> {
    List<JobTrend> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime since);
    List<JobTrend> findTop50ByOrderByCreatedAtDesc();
}