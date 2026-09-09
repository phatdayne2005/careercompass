package vn.uth.careercompass.blackbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * KỸ THUẬT: Bảng quyết định (Decision Table Testing) — chương IV mục 3.
 *
 * <p>Đối tượng: bộ ba phép kiểm đầu vào của {@code OnboardingService.saveTranscript()}:
 * <pre>
 *   if (originalFilename == null)            -&gt; "Tên file không hợp lệ."
 *   if (ext khong thuoc {pdf,png,jpg,jpeg})  -&gt; "Chỉ chấp nhận file PDF, PNG hoặc JPG."
 *   if (size &gt; 10MB)                         -&gt; "File vượt quá dung lượng tối đa 10MB."
 * </pre>
 *
 * <p><b>Ba điều kiện đầu vào:</b>
 * <ul>
 *   <li>C1 — Tên tệp có null không: Y | N                                    (2 giá trị)</li>
 *   <li>C2 — Đuôi tệp: .pdf | .png | .jpg | .jpeg | khác | không có dấu chấm (6 giá trị)</li>
 *   <li>C3 — Dung lượng ≤ 10MB: Y | N                                        (2 giá trị)</li>
 * </ul>
 *
 * <p>Số rule tối đa = 2 × 6 × 2 = <b>24</b>. Nhưng mã nguồn kiểm ba điều kiện <i>tuần tự</i>
 * và thoát ngay khi gặp lỗi, nên khi một điều kiện phía trước đã quyết định kết quả thì các
 * điều kiện phía sau trở thành <b>không quan tâm</b> (ký hiệu "—"). Gộp các rule chỉ khác nhau
 * ở ô không-quan-tâm lại, bảng rút gọn còn <b>8 rule</b>:
 *
 * <pre>
 *   Điều kiện / Hành động          R1    R2    R3    R4    R5     R6      R7    R8
 *   C1 Tên tệp = null?             Y     N     N     N     N      N       N     N
 *   C2 Đuôi tệp                    —    .pdf  .png  .jpg  .jpeg  hợp lệ   khác  không có
 *   C3 Dung lượng <= 10MB?         —     Y     Y     Y     Y      N       —     —
 *   A1 Lưu tệp, trả về đường dẫn   -     X     X     X     X      -       -     -
 *   A2 Lỗi "Tên file không hợp lệ" X     -     -     -     -      -       -     -
 *   A3 Lỗi "Vượt quá 10MB"         -     -     -     -     -      X       -     -
 *   A4 Lỗi "Chỉ chấp nhận PDF..."  -     -     -     -     -      -       X     X
 * </pre>
 *
 * <p><b>R6 là một rule đã gộp:</b> khi tệp quá dung lượng thì hành động giống hệt nhau với
 * cả bốn đuôi hợp lệ, nên bốn rule con được thu về một, ô C2 ghi "hợp lệ" thay vì một đuôi
 * cụ thể. Đây chính là bước rút gọn bảng quyết định của chương IV.
 *
 * <p><b>Vì sao chọn hàm này:</b> báo cáo độ bao phủ ({@code docs/coverage/README.md}) chỉ đích
 * danh {@code OnboardingService} đạt nhánh 75,0% (9/12) và ba nhánh còn thiếu nằm gọn ở dòng
 * kiểm đuôi tệp — vì mọi test trước đây đều đặt tên tệp là {@code transcript.pdf}, ba nhánh
 * {@code .png}, {@code .jpg}, {@code .jpeg} chưa bao giờ được thử. Bảng quyết định này phủ trọn.
 *
 * <p><b>Khiếm khuyết đã sửa — DEF-011 (R8):</b> tệp không có dấu chấm (ví dụ "bangdiem") làm
 * {@code lastIndexOf(".")} trả về -1, kéo theo {@code substring(-1)} ném
 * {@code StringIndexOutOfBoundsException} — người dùng nhận lỗi 500 thay vì thông báo 400.
 *
 * <p>Cần nói rõ để không nhận công sai: <b>bảng quyết định KHÔNG phải nơi phát hiện lỗi này</b>.
 * Test hộp trắng {@code OnboardingServiceTest} đã ghi đúng nguyên nhân từ trước
 * ("BUG?: nên guard {@code lastIndexOf(".") < 0}") — nhưng lại <i>chốt luôn hành vi lỗi</i> bằng
 * {@code assertThatThrownBy(...).isInstanceOf(StringIndexOutOfBoundsException.class)}. Bộ test vì
 * thế vẫn xanh và khiếm khuyết nằm im suốt thời gian đó.
 *
 * <p>Đóng góp thật của kỹ thuật bảng quyết định là <b>buộc phải sửa</b>: mỗi cột của bảng phải
 * ứng với một hành động xác định trong nhóm A1..A4, không thể để một ô là "sập 500". Ghi chú
 * "BUG?" có thể sống mãi trong mã nguồn; một ô trống trong bảng quyết định thì không.
 * {@code OnboardingService} đã được sửa: coi "không có đuôi" là đuôi rỗng để rơi đúng vào A4.
 *
 * <p><b>Quan hệ với sheet giá trị biên:</b> C3 ở bảng này chỉ lấy giá trị <i>nominal</i>
 * (1 byte và 11 MB) để phân biệt nhánh. Các giá trị sát biên 10 MB (max-1, max, max+1) thuộc
 * về {@code OnboardingFileSizeBvaTest} — hai kỹ thuật bổ sung nhau chứ không trùng.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bang quyet dinh - kiem dinh dang va dung luong tep bang diem")
// Giữ thứ tự R1..R8 đúng như các cột của bảng quyết định trong báo cáo.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TranscriptFileDecisionTableTest {

    private static final long MB = 1024L * 1024L;
    private static final long DUNG_LUONG_HOP_LE = 1L;          // C3 = Y, giá trị nominal
    private static final long DUNG_LUONG_QUA_LON = 11L * MB;   // C3 = N, giá trị nominal

    @Mock
    private UserSkillRepository userSkillRepository;
    @Mock
    private MultipartFile file;

    @InjectMocks
    private OnboardingService onboardingService;

    @TempDir
    Path thuMucTam;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(onboardingService, "uploadDir", thuMucTam.toString());
    }

    /** Chuẩn bị chung cho các rule đi tới bước ghi tệp thật. */
    private void choTepLa(String tenTep, long dungLuong) throws Exception {
        when(file.getOriginalFilename()).thenReturn(tenTep);
        when(file.getSize()).thenReturn(dungLuong);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1}));
    }

    private User nguoiDung() {
        return User.builder().id(1L).build();
    }

    // ================================================================
    // NHÓM TỪ CHỐI VÌ TÊN TỆP — A2
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("R1 | ten tep = null -> TU CHOI 'Ten file khong hop le'")
    void rule1_tenTepNull_tuChoi() {
        when(file.getOriginalFilename()).thenReturn(null);

        assertThatThrownBy(() -> onboardingService.saveTranscript(nguoiDung(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tên file không hợp lệ.");
    }

    // ================================================================
    // NHÓM CHẤP NHẬN — A1 (bốn đuôi hợp lệ, dung lượng trong ngưỡng)
    // ================================================================

    @Test
    @Order(2)
    @DisplayName("R2 | duoi .pdf + dung luong hop le -> LUU THANH CONG")
    void rule2_duoiPdf_luuThanhCong() throws Exception {
        choTepLa("bangdiem.pdf", DUNG_LUONG_HOP_LE);

        String duongDan = onboardingService.saveTranscript(nguoiDung(), file);

        assertThat(duongDan).endsWith(".pdf");
        assertThat(thuMucTam.resolve(Path.of(duongDan).getFileName())).exists();
    }

    @Test
    @Order(3)
    @DisplayName("R3 | duoi .png + dung luong hop le -> LUU THANH CONG")
    void rule3_duoiPng_luuThanhCong() throws Exception {
        choTepLa("bangdiem.png", DUNG_LUONG_HOP_LE);

        String duongDan = onboardingService.saveTranscript(nguoiDung(), file);

        assertThat(duongDan).endsWith(".png");
    }

    @Test
    @Order(4)
    @DisplayName("R4 | duoi .jpg + dung luong hop le -> LUU THANH CONG")
    void rule4_duoiJpg_luuThanhCong() throws Exception {
        choTepLa("bangdiem.jpg", DUNG_LUONG_HOP_LE);

        String duongDan = onboardingService.saveTranscript(nguoiDung(), file);

        assertThat(duongDan).endsWith(".jpg");
    }

    @Test
    @Order(5)
    @DisplayName("R5 | duoi .jpeg + dung luong hop le -> LUU THANH CONG")
    void rule5_duoiJpeg_luuThanhCong() throws Exception {
        choTepLa("bangdiem.jpeg", DUNG_LUONG_HOP_LE);

        String duongDan = onboardingService.saveTranscript(nguoiDung(), file);

        assertThat(duongDan).endsWith(".jpeg");
    }

    // ================================================================
    // NHÓM TỪ CHỐI VÌ DUNG LƯỢNG — A3 (rule đã gộp)
    // ================================================================

    @Test
    @Order(6)
    @DisplayName("R6 | duoi hop le nhung dung luong > 10MB -> TU CHOI 'Vuot qua 10MB'")
    void rule6_duoiHopLe_dungLuongQuaLon_tuChoi() {
        when(file.getOriginalFilename()).thenReturn("bangdiem.pdf");
        when(file.getSize()).thenReturn(DUNG_LUONG_QUA_LON);

        assertThatThrownBy(() -> onboardingService.saveTranscript(nguoiDung(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File vượt quá dung lượng tối đa 10MB.");
    }

    // ================================================================
    // NHÓM TỪ CHỐI VÌ ĐỊNH DẠNG — A4
    // ================================================================

    @Test
    @Order(7)
    @DisplayName("R7 | duoi .exe (khong thuoc danh sach) -> TU CHOI 'Chi chap nhan PDF, PNG hoac JPG'")
    void rule7_duoiKhongHopLe_tuChoi() {
        when(file.getOriginalFilename()).thenReturn("bangdiem.exe");

        assertThatThrownBy(() -> onboardingService.saveTranscript(nguoiDung(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chỉ chấp nhận file PDF, PNG hoặc JPG.");
    }

    /**
     * DEF-011 — hồi quy. Trước khi sửa, rule này ném {@code StringIndexOutOfBoundsException}
     * (lỗi 500) chứ không phải {@code IllegalArgumentException} (lỗi 400).
     * Điều kiện C3 để "—" vì phép kiểm đuôi chặn trước khi tới phép kiểm dung lượng.
     */
    @Test
    @Order(8)
    @DisplayName("R8 | ten tep khong co dau cham -> TU CHOI 400, KHONG duoc sap 500 (DEF-011)")
    void rule8_tenTepKhongCoDuoi_tuChoi() {
        when(file.getOriginalFilename()).thenReturn("bangdiem");

        assertThatThrownBy(() -> onboardingService.saveTranscript(nguoiDung(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chỉ chấp nhận file PDF, PNG hoặc JPG.")
                .isNotInstanceOf(StringIndexOutOfBoundsException.class);
    }

    // ================================================================
    // KIỂM TRA BỔ SUNG — nằm ngoài bảng quyết định
    // ================================================================

    /**
     * Mã nguồn gọi {@code toLowerCase()} trước khi so sánh, nên đuôi viết hoa phải được
     * chấp nhận. Không đưa vào bảng vì đây là biến thể trình bày của C2, không phải một
     * rule độc lập — nếu tách ra thì bảng phải nhân đôi số cột mà không thêm hành động mới.
     */
    @Test
    @Order(9)
    @DisplayName("Bo sung | duoi viet hoa .PDF van duoc chap nhan (nho toLowerCase)")
    void boSung_duoiVietHoa_duocChapNhan() throws Exception {
        choTepLa("BANGDIEM.PDF", DUNG_LUONG_HOP_LE);

        String duongDan = onboardingService.saveTranscript(nguoiDung(), file);

        assertThat(duongDan).endsWith(".pdf");
    }
}
