package vn.uth.careercompass.blackbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.UserProfileService;
import vn.uth.careercompass.onboarding.controller.OnboardingController;
import vn.uth.careercompass.onboarding.service.OnboardingService;
import vn.uth.careercompass.onboarding.service.TranscriptAnalysisService;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KỸ THUẬT: Chuyển đổi trạng thái (State Transition Testing) — chương IV mục 3.
 *
 * <p>Đối tượng: máy trạng thái ba bước khai báo hồ sơ lần đầu ({@code /onboarding/step1..3}).
 * Khác với hai máy trạng thái đã có (tiến độ node, vòng đời token) ở chỗ trạng thái KHÔNG nằm
 * trong một cột enum, mà là <b>tổ hợp cờ trên bản ghi người dùng</b>: {@code targetRoleId},
 * {@code transcriptPath} / {@code githubUsername}, và {@code onboardingCompleted}.
 *
 * <p>BỐN TRẠNG THÁI:
 * <pre>
 *   S1 MOI_TAO        onboardingCompleted = false, chưa qua bước nào
 *   S2 DA_CHON_NGHE   đã gửi bước 1
 *   S3 DA_NAP_NGUON   đã gửi bước 2 (bảng điểm và/hoặc GitHub)
 *   S4 HOAN_TAT       onboardingCompleted = true  -- trạng thái HẤP THỤ, không có đường ra
 * </pre>
 *
 * <p>BẢNG CHUYỂN TRẠNG THÁI:
 * <pre>
 *   Mã     Trạng thái đầu  Sự kiện                          Trạng thái cuối  Hợp lệ
 *   ST-01  MOI_TAO         POST step1 (chọn nghề hợp lệ)    DA_CHON_NGHE     Có
 *   ST-02  MOI_TAO         POST step1 (không chọn gì)       MOI_TAO          Có (tự lặp)
 *   ST-03  MOI_TAO         POST step1 (bấm Bỏ qua)          MOI_TAO          Có (tự lặp)
 *   ST-04  DA_CHON_NGHE    POST step2                       DA_NAP_NGUON     Có
 *   ST-05  DA_NAP_NGUON    POST step3 (chọn kỹ năng)        HOAN_TAT         Có
 *   ST-06  HOAN_TAT        GET step1 / step2 / step3        HOAN_TAT         KHÔNG
 *   ST-07  HOAN_TAT        POST step3 lần nữa               HOAN_TAT         KHÔNG
 *   ST-08  MOI_TAO         GET step3 (nhảy cóc bỏ 1 và 2)   MOI_TAO          Có
 * </pre>
 *
 * <p>ST-01 → ST-05 phủ trọn đường đi thuận S1 → S2 → S3 → S4. ST-02 và ST-03 là hai cạnh
 * <b>tự lặp</b> — cùng đích nhưng khác hành động, nên phải tách thành hai dòng.
 *
 * <p>ST-06 và ST-07 là phần đắt giá nhất: S4 phải là trạng thái <b>hấp thụ</b>. Cả sáu điểm
 * vào ({@code GET} và {@code POST} của ba bước) đều phải chặn. Nếu bỏ sót chốt chặn ở đúng một
 * điểm, người dùng đã hoàn tất có thể quay lại và {@code replaceSkills()} sẽ <b>xoá sạch</b>
 * danh sách kỹ năng cũ. Kiểm thử đi đường thuận không bao giờ chạm tới cạnh này.
 *
 * <p><b>ST-08 — điều đã kiểm chứng và bác bỏ.</b> Ban đầu chốt chặn thiếu ở {@code GET step3}
 * bị nghi là khiếm khuyết: gõ thẳng URL là vào được bước 3 mà chưa qua bước 1 và 2. Dựng máy
 * trạng thái xong mới thấy KHÔNG phải lỗi — bước 1 có sẵn nút "Bỏ qua" ({@code skip=true}) và
 * mọi trường ở bước 2 đều tuỳ chọn, nên đi đúng luồng giao diện cũng tới được bước 3 với hồ sơ
 * trống. Nhảy cóc không giành thêm quyền gì. ST-08 được giữ lại làm test hồi quy, chốt rằng
 * đây là hành vi CỐ Ý chứ không phải sơ suất.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Chuyen doi trang thai - ba buoc khai bao ho so lan dau")
// Giữ thứ tự ST-01..ST-08 đúng như các dòng của bảng chuyển trạng thái.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnboardingStateTransitionTest {

    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private OnboardingService onboardingService;
    @Mock
    private TranscriptAnalysisService transcriptAnalysisService;
    @Mock
    private CareerRoleRepository careerRoleRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private OnboardingController controller;

    private static final Long MA_NGHE = 5L;

    private final Model model = new ExtendedModelMap();
    private final RedirectAttributes flash = new RedirectAttributesModelMap();

    /** S1/S2/S3 — hồ sơ chưa hoàn tất, mọi bước còn mở. */
    private User dangKhaiBao() {
        User user = User.builder().id(1L).build();
        user.setOnboardingCompleted(false);
        when(authenticatedUserService.requireCurrentUser(authentication)).thenReturn(user);
        return user;
    }

    /** S4 — hồ sơ đã hoàn tất, phải bị chặn ở mọi điểm vào. */
    private User daHoanTat() {
        User user = User.builder().id(1L).build();
        user.setOnboardingCompleted(true);
        when(authenticatedUserService.requireCurrentUser(authentication)).thenReturn(user);
        return user;
    }

    // ================================================================
    // ĐƯỜNG ĐI THUẬN — S1 -> S2 -> S3 -> S4
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("ST-01 | MOI_TAO --[POST step1, chon nghe hop le]--> DA_CHON_NGHE")
    void st01_moiTao_chonNghe_sangBuoc2() {
        User user = dangKhaiBao();

        String ketQua = controller.step1Submit(MA_NGHE, false, authentication, flash);

        assertThat(ketQua).isEqualTo("redirect:/onboarding/step2");
        verify(userProfileService).setTargetRole(user, MA_NGHE);
    }

    @Test
    @Order(2)
    @DisplayName("ST-02 | MOI_TAO --[POST step1, khong chon gi]--> MOI_TAO (tu lap, bao loi)")
    void st02_moiTao_khongChonGi_tuLap() {
        dangKhaiBao();

        String ketQua = controller.step1Submit(null, false, authentication, flash);

        assertThat(ketQua).isEqualTo("redirect:/onboarding/step1");
        assertThat(flash.getFlashAttributes().get("error"))
                .isEqualTo("Vui lòng chọn một vị trí nghề nghiệp.");
        // Cạnh tự lặp thì tuyệt đối không được ghi gì vào hồ sơ.
        verify(userProfileService, never()).setTargetRole(any(), any());
    }

    @Test
    @Order(3)
    @DisplayName("ST-03 | MOI_TAO --[POST step1, bam Bo qua]--> MOI_TAO (tu lap, sang buoc 2)")
    void st03_moiTao_boQua_tuLap() {
        dangKhaiBao();

        String ketQua = controller.step1Submit(null, true, authentication, flash);

        // Đi tiếp sang bước 2 nhưng KHÔNG chuyển trạng thái: hồ sơ vẫn chưa có định hướng.
        assertThat(ketQua).isEqualTo("redirect:/onboarding/step2");
        assertThat(flash.getFlashAttributes()).isEmpty();
        verify(userProfileService, never()).setTargetRole(any(), any());
    }

    @Test
    @Order(4)
    @DisplayName("ST-04 | DA_CHON_NGHE --[POST step2, khong nap gi]--> DA_NAP_NGUON")
    void st04_daChonNghe_napNguon_sangBuoc3() {
        User user = dangKhaiBao();
        user.setCareerRole(CareerRole.builder().id(MA_NGHE).build());

        String ketQua = controller.step2Submit(null, null, authentication, flash);

        assertThat(ketQua).isEqualTo("redirect:/onboarding/step3");
        // Không nạp gì thì không gọi dịch vụ nào — bước 2 hoàn toàn tuỳ chọn.
        verify(userProfileService, never()).storeTranscript(any(), any(), any());
        verify(userProfileService, never()).setGithub(any(), any());
    }

    @Test
    @Order(5)
    @DisplayName("ST-05 | DA_NAP_NGUON --[POST step3, chon ky nang]--> HOAN_TAT")
    void st05_daNapNguon_chonKyNang_hoanTat() {
        User user = dangKhaiBao();
        user.setCareerRole(CareerRole.builder().id(MA_NGHE).build());
        List<Long> kyNang = List.of(1L, 2L, 3L);

        String ketQua = controller.step3Submit(kyNang, authentication);

        assertThat(ketQua).isEqualTo("redirect:/");
        verify(userProfileService).replaceSkills(user, kyNang);
        verify(userProfileService).completeOnboarding(user);
    }

    // ================================================================
    // TRẠNG THÁI HẤP THỤ — không có đường ra khỏi HOAN_TAT
    // ================================================================

    /**
     * Cột: tên điểm vào · lời gọi tương ứng. Sáu điểm vào của ba bước đều phải chặn
     * và trả về cùng một đích "redirect:/".
     */
    static Stream<Arguments> diemVaoSauKhiHoanTat() {
        return Stream.of(
                Arguments.of("GET step1",  (Function<Ngu, String>) n -> n.c.step1(n.model, n.auth)),
                Arguments.of("GET step2",  (Function<Ngu, String>) n -> n.c.step2(n.model, n.auth)),
                Arguments.of("GET step3",  (Function<Ngu, String>) n -> n.c.step3(n.model, n.auth)),
                Arguments.of("POST step1", (Function<Ngu, String>) n -> n.c.step1Submit(MA_NGHE, false, n.auth, n.flash)),
                Arguments.of("POST step2", (Function<Ngu, String>) n -> n.c.step2Submit(null, "octocat", n.auth, n.flash))
        );
    }

    /** Gói tham số cho lambda ở bảng trên — chỉ để test đọc gọn. */
    private record Ngu(OnboardingController c, Model model, Authentication auth, RedirectAttributes flash) {
    }

    @ParameterizedTest(name = "ST-06 | HOAN_TAT --[{0}]--> HOAN_TAT (chuyen huong ve /)")
    @MethodSource("diemVaoSauKhiHoanTat")
    @Order(6)
    @DisplayName("ST-06 | HOAN_TAT | moi diem vao deu bi chan, khong duong nao quay lai")
    void st06_hoanTat_moiDiemVaoBiChan(String tenDiemVao, Function<Ngu, String> goi) {
        daHoanTat();

        String ketQua = goi.apply(new Ngu(controller, model, authentication, flash));

        assertThat(ketQua).as("diem vao %s", tenDiemVao).isEqualTo("redirect:/");
        // Bị chặn thì tuyệt đối không được đụng vào hồ sơ đã hoàn tất.
        verify(userProfileService, never()).setTargetRole(any(), any());
        verify(userProfileService, never()).setGithub(any(), any());
        verify(careerRoleRepository, never()).findAll();
        verify(skillRepository, never()).findAll();
    }

    /**
     * Tách riêng khỏi ST-06 vì đây là cạnh nguy hiểm nhất: nếu chốt chặn thiếu ở đúng
     * điểm này thì {@code replaceSkills()} sẽ xoá sạch danh sách kỹ năng người dùng đã chọn.
     */
    @Test
    @Order(7)
    @DisplayName("ST-07 | HOAN_TAT --[POST step3 lan nua]--> HOAN_TAT, KHONG xoa ky nang cu")
    void st07_hoanTat_guiLaiBuoc3_khongXoaKyNang() {
        daHoanTat();

        String ketQua = controller.step3Submit(List.of(9L), authentication);

        assertThat(ketQua).isEqualTo("redirect:/");
        verify(userProfileService, never()).replaceSkills(any(), any());
        verify(userProfileService, never()).completeOnboarding(any());
    }

    // ================================================================
    // CHUYỂN ĐỔI ĐÃ KIỂM CHỨNG LÀ CỐ Ý — không phải khiếm khuyết
    // ================================================================

    @Test
    @Order(8)
    @DisplayName("ST-08 | MOI_TAO --[GET step3, nhay coc]--> MOI_TAO (co y cho phep, xem Javadoc)")
    void st08_moiTao_nhayCocSangBuoc3_duocPhep() {
        dangKhaiBao();
        when(skillRepository.findAll()).thenReturn(List.of());
        when(onboardingService.getUserSkillIds(any())).thenReturn(List.of());

        String ketQua = controller.step3(model, authentication);

        // Vào được bước 3 dù chưa qua bước 1 và 2 — tương đương đường đi hợp lệ
        // "Bỏ qua bước 1" rồi "gửi bước 2 trống", nên không giành thêm quyền gì.
        assertThat(ketQua).isEqualTo("onboarding/step3_skills");
        assertThat(model.getAttribute("currentStep")).isEqualTo(3);
    }
}
