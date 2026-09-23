package vn.uth.careercompass.kernel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.careercompass.kernel.entity.PasswordResetToken;

import java.util.Optional;
import vn.uth.careercompass.kernel.entity.User;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    @Transactional
    void deleteByToken(String token);

    /** Dọn token khi xoá tài khoản (DEF-012). Bảng này có khoá ngoại tới users. */
    void deleteByUser(User user);
}
