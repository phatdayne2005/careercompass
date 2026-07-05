package vn.uth.careercompass.kernel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.careercompass.kernel.entity.AuthProvider;
import vn.uth.careercompass.kernel.entity.PasswordResetToken;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.PasswordResetTokenRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public void createResetToken(String email) {
        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) return;
        User user = opt.get();
        if (user.getAuthProvider() != AuthProvider.LOCAL)
            return;
        String resetToken = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .token(resetToken)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build());

        String link = baseUrl + "/reset-password?token=" + resetToken;
        String subject = "CareerCompass — Đặt lại mật khẩu";
        String body = "Xin chào,\n\n"
                + "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản CareerCompass.\n"
                + "Nhấn vào link dưới đây để đặt lại mật khẩu (link có hiệu lực trong 30 phút):\n\n"
                + link + "\n\n"
                + "Nếu bạn không yêu cầu điều này, hãy bỏ qua email — mật khẩu của bạn vẫn an toàn.\n\n"
                + "— CareerCompass";
        emailService.sendEmail(user.getEmail(), subject, body);
    }

    public Optional<PasswordResetToken> validateToken(String token) {
        return passwordResetTokenRepository.findByToken(token).filter(PasswordResetToken::isValid);
    }

    @Transactional
    public void resetPassword(String token, String newPassword){
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token).orElseThrow();
        if (!prt.isValid())
            throw new IllegalStateException("Token không hợp lệ hoặc đã hết hạn");
        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        prt.setUsed(true);
    }


}
