package vn.uth.careercompass.mentor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // Lấy toàn bộ tin nhắn thuộc một MentorSession, sắp xếp thời gian từ cũ đến mới
    List<ChatMessage> findBySessionOrderBySentAtAsc(MentorSession session);
}