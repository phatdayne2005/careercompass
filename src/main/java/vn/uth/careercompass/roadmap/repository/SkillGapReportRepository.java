package vn.uth.careercompass.roadmap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.roadmap.entity.SkillGapReport;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillGapReportRepository extends JpaRepository<SkillGapReport, Long> {
    List<SkillGapReport> findByUserOrderByCreatedAtDesc(User user);

    Optional<SkillGapReport> findByIdAndUser(Long id, User user);

    /** Xoá mọi báo cáo skill-gap của 1 template (dùng khi P7 xoá lộ trình). */
    void deleteByTemplate_Id(Long templateId);
}
