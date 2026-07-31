package vn.uth.careercompass.onboarding.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.entity.UserSkill;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link OnboardingService}.
 *
 * <p>MỤC TIÊU: test RIÊNG logic của OnboardingService, KHÔNG bật Spring.
 * Dependency {@code UserSkillRepository} được Mockito mock. {@code MultipartFile}
 * cũng được mock để ta điều khiển tên file / dung lượng / nội dung tuỳ ý.
 *
 * <p>KỸ THUẬT: {@code saveTranscript} có field {@code @Value("${app.upload.dir:...}")}.
 * Spring không chạy nên field này = null → ta tự "tiêm tay" bằng
 * {@link ReflectionTestUtils#setField}, trỏ vào 1 thư mục tạm ({@code @TempDir})
 * để phần ghi file thật diễn ra trong sandbox, không đụng thư mục dự án.
 */
@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserSkillRepository userSkillRepository;

    // MultipartFile là THAM SỐ của method (không phải dependency constructor),
    // nhưng ta vẫn dùng @Mock để tái sử dụng và tự dạy hành vi trong từng test.
    // Mock không được dùng ở test nào cũng không sao (chỉ stub thừa mới lỗi).
    @Mock
    private MultipartFile file;

    @InjectMocks
    private OnboardingService onboardingService;

    // JUnit 5 tự tạo + tự dọn thư mục tạm này sau mỗi test.
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Gán tay uploadDir về thư mục tạm — nếu để null thì Paths.get(null) sẽ NPE.
        ReflectionTestUtils.setField(onboardingService, "uploadDir", tempDir.toString());
    }

    // ============================================================
    // saveTranscript(user, file)
    // ============================================================

    @Test
    void saveTranscript_whenFilenameIsNull_throwsIllegalArgument() throws Exception {
        // Given: file không có tên gốc (originalFilename = null)
        when(file.getOriginalFilename()).thenReturn(null);

        // When + Then: chặn ngay, không đọc tới dung lượng hay ghi file
        assertThatThrownBy(() -> onboardingService.saveTranscript(new User(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tên file không hợp lệ.");
        // LƯU Ý: không stub getSize()/getInputStream() vì luồng dừng sớm ở đây;
        // stub thừa sẽ khiến Mockito (strict) ném UnnecessaryStubbingException.
    }

    @Test
    void saveTranscript_whenExtensionNotAllowed_throwsIllegalArgument() throws Exception {
        // Given: đuôi .txt không nằm trong danh sách cho phép (pdf/png/jpg/jpeg)
        when(file.getOriginalFilename()).thenReturn("bang_diem.txt");

        // When + Then: từ chối trước cả bước kiểm dung lượng
        assertThatThrownBy(() -> onboardingService.saveTranscript(new User(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chỉ chấp nhận file PDF, PNG hoặc JPG.");
    }

    @Test
    void saveTranscript_whenFileTooLarge_throwsIllegalArgument() throws Exception {
        // Given: đuôi hợp lệ nhưng dung lượng vượt ngưỡng 10MB
        when(file.getOriginalFilename()).thenReturn("bang_diem.pdf");
        when(file.getSize()).thenReturn(11L * 1024 * 1024); // 11MB

        // When + Then
        assertThatThrownBy(() -> onboardingService.saveTranscript(new User(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File vượt quá dung lượng tối đa 10MB.");
    }

    @Test
    void saveTranscript_whenValidPdf_savesFileAndReturnsRelativePath() throws Exception {
        // Given: file PDF hợp lệ, nhỏ, có nội dung đọc được
        User user = User.builder().id(42L).build();
        when(file.getOriginalFilename()).thenReturn("bang_diem.pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("noi dung pdf".getBytes()));

        // When
        String returnedPath = onboardingService.saveTranscript(user, file);

        // Then: đường dẫn trả về đúng format "<uploadDir>/<userId>_<uuid>.pdf"
        assertThat(returnedPath)
                .startsWith(tempDir.toString() + "/42_")
                .endsWith(".pdf");
        // Và: file thật đã được ghi ra đĩa (đọc lại được).
        assertThat(Files.exists(Paths.get(returnedPath))).isTrue();
    }

    @Test
    void saveTranscript_whenFilenameHasNoDot_throwsStringIndexOutOfBounds() throws Exception {
        // Given: tên file không có dấu chấm nào -> lastIndexOf(".") = -1 -> substring(-1)
        when(file.getOriginalFilename()).thenReturn("bangdiemkhongduoi");

        // When + Then: hiện tại code KHÔNG bắt trường hợp này nên ném StringIndexOutOfBoundsException
        // thô thay vì IllegalArgumentException thân thiện.
        // BUG?: filename thiếu phần mở rộng (vd "resume") làm substring(lastIndexOf(".")) với index=-1
        //       ném StringIndexOutOfBoundsException, không phải thông báo "Tên file không hợp lệ".
        //       Nên guard `lastIndexOf(".") < 0` để trả IllegalArgumentException cho đồng nhất.
        assertThatThrownBy(() -> onboardingService.saveTranscript(new User(), file))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
    }

    // ============================================================
    // getUserSkillIds(user)
    // ============================================================

    @Test
    void getUserSkillIds_returnsIdsOfUserSkills() {
        // Given: user đang có 2 skill (id 1 và 2)
        User user = User.builder().id(1L).build();
        Skill java = Skill.builder().id(1L).name("Java").build();
        Skill sql = Skill.builder().id(2L).name("SQL").build();
        List<UserSkill> userSkills = List.of(
                UserSkill.builder().user(user).skill(java).build(),
                UserSkill.builder().user(user).skill(sql).build());
        when(userSkillRepository.findByUser(user)).thenReturn(userSkills);

        // When
        List<Long> ids = onboardingService.getUserSkillIds(user);

        // Then: trả đúng danh sách id skill để UI tick "đã chọn"
        assertThat(ids).containsExactly(1L, 2L);
    }

    @Test
    void getUserSkillIds_whenNoSkills_returnsEmptyList() {
        // Given: user chưa chọn skill nào
        User user = User.builder().id(1L).build();
        when(userSkillRepository.findByUser(user)).thenReturn(List.of());

        // When
        List<Long> ids = onboardingService.getUserSkillIds(user);

        // Then: trả list rỗng (không null) -> UI lặp an toàn
        assertThat(ids).isEmpty();
    }
}
