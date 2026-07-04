package vn.uth.careercompass.onboarding.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserSkillRepository userSkillRepository;

    @Value("${app.upload.dir:uploads/transcripts}")
    private String uploadDir;

    /**
     * Lưu file bảng điểm vào thư mục /uploads/transcripts
     * Trả về đường dẫn tương đối để lưu vào DB
     */
    public String saveTranscript(User user, MultipartFile file) throws IOException {
        // Kiểm tra định dạng file
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Tên file không hợp lệ.");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        if (!ext.equals(".pdf") && !ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file PDF, PNG hoặc JPG.");
        }

        // Kiểm tra dung lượng (tối đa 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File vượt quá dung lượng tối đa 10MB.");
        }

        // Tạo thư mục nếu chưa tồn tại
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        // Tạo tên file duy nhất để tránh trùng lặp
        String uniqueFilename = user.getId() + "_" + UUID.randomUUID() + ext;
        Path targetPath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return uploadDir + "/" + uniqueFilename;
    }

    /**
     * Lấy danh sách skill ID hiện có của user để đánh dấu đã chọn trên giao diện
     */
    public List<Long> getUserSkillIds(User user) {
        return userSkillRepository.findByUser(user)
                .stream()
                .map(us -> us.getSkill().getId())
                .toList();
    }
}
