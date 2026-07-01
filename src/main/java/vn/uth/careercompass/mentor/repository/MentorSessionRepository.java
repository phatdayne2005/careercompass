package vn.uth.careercompass.mentor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.mentor.entity.MentorSession;

import java.util.List;

public interface MentorSessionRepository extends JpaRepository<MentorSession, Long> {

    List<MentorSession> findByUserOrderByCreatedAtDesc(User user);
}