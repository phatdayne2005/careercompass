package vn.uth.careercompass.bva;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;
import vn.uth.careercompass.onboarding.service.OnboardingService;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** BVA 5 điểm cho giới hạn dung lượng file 10 MB. */
@ExtendWith(MockitoExtension.class)
class OnboardingFileSizeBvaTest {

    private static final long MB = 1024L * 1024L;
    private static final long MAX_SIZE = 10L * MB;

    @Mock
    private UserSkillRepository userSkillRepository;
    @Mock
    private MultipartFile file;

    @InjectMocks
    private OnboardingService onboardingService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(onboardingService, "uploadDir", tempDir.toString());
    }

    static Stream<Arguments> fileSizeNormalBva() {
        return Stream.of(
                Arguments.of("min", 0L),
                Arguments.of("min+1", 1L),
                Arguments.of("medium", 5L * MB),
                Arguments.of("max-1", MAX_SIZE - 1),
                Arguments.of("max", MAX_SIZE)
        );
    }

    @ParameterizedTest(name = "file size {0} = {1} bytes")
    @MethodSource("fileSizeNormalBva")
    void fileSize_normalBva_isAccepted(String boundary, long size) throws Exception {
        when(file.getOriginalFilename()).thenReturn("transcript.pdf");
        when(file.getSize()).thenReturn(size);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));

        String savedPath = onboardingService.saveTranscript(
                User.builder().id(1L).build(), file);

        assertThat(savedPath)
                .as("file size boundary %s", boundary)
                .endsWith(".pdf");
    }

    @Test
    void fileSize_maxPlusOne_isRejected() throws Exception {
        when(file.getOriginalFilename()).thenReturn("transcript.pdf");
        when(file.getSize()).thenReturn(MAX_SIZE + 1);

        assertThatThrownBy(() -> onboardingService.saveTranscript(
                User.builder().id(1L).build(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File vượt quá dung lượng tối đa 10MB.");
    }
}
