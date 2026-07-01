package vn.uth.careercompass.mentor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.uth.careercompass.mentor.entity.ChatMessage;
import vn.uth.careercompass.mentor.entity.MentorSession;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionOrderByCreatedAtAsc(MentorSession session);
}