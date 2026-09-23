package vn.uth.careercompass.mentor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.mentor.entity.MentorSession;

import java.util.List;

public interface MentorSessionRepository extends JpaRepository<MentorSession, Long> {

    List<MentorSession> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Dọn phiên chat khi xoá tài khoản (DEF-012).
     *
     * <p>Derived delete của Spring Data NẠP thực thể rồi gọi em.remove() từng cái, khác
     * hẳn @Modifying @Query xoá thẳng bằng JPQL. Nhờ vậy cascade khai trên trường
     * chatMessages ({@code cascade = ALL, orphanRemoval = true}) mới chạy và dọn luôn
     * bảng chat_messages. Đổi sang @Query xoá hàng loạt là để lại tin nhắn mồ côi.
     */
    void deleteByUser(User user);
}