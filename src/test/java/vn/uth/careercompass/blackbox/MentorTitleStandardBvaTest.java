package vn.uth.careercompass.blackbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.mentor.entity.MentorSession;
import vn.uth.careercompass.mentor.repository.ChatMessageRepository;
import vn.uth.careercompass.mentor.repository.MentorSessionRepository;
import vn.uth.careercompass.mentor.service.LlmClient;
import vn.uth.careercompass.mentor.service.MentorService;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KỸ THUẬT: Phân tích giá trị biên trình bày theo ĐÚNG mẫu slide 23 và 24.
 *
 * <p>Đối tượng: ngưỡng cắt tiêu đề phiên trò chuyện trong {@code MentorService.sendMessage()}:
 * <pre>
 *   session.setTitle(userText.length() &gt; 60 ? userText.substring(0, 60) + "…" : userText);
 * </pre>
 *
 * <p>Đây là ngưỡng <b>cứng trong mã nguồn</b>, không phải ràng buộc {@code @Size} trên DTO —
 * toàn bộ {@code @Size} của dự án đều nằm ở {@code RegisterFormDTO} và đã được phủ ở
 * {@code RegisterStandardBvaTest}. Muốn tìm thêm biên phải soi các ngưỡng cắt chuỗi trong
 * tầng service, và 60 là ngưỡng sạch nhất: một biến, một phép so sánh, kết quả quan sát được
 * trực tiếp (chuỗi tiêu đề) mà không cần gọi dịch vụ ngoài.
 *
 * <p><b>Vì sao đáng kiểm:</b> lỗi lệch một đơn vị ở {@code substring} là lỗi kinh điển.
 * Với chuỗi dài đúng 60 ký tự: cắt hay không cắt? Mã dùng {@code &gt;} chứ không phải
 * {@code &gt;=}, nên 60 ký tự phải giữ NGUYÊN VĂN. Chỉ có case ở đúng biên mới phân biệt được
 * hai cách viết đó — case nominal 30 ký tự thì cả hai đều cho cùng kết quả.
 *
 * <p><b>Biến duy nhất đang xét</b> (n = 1) là độ dài {@code userText}:
 * <pre>
 *   min  = 1     (câu hỏi ngắn nhất có nghĩa)
 *   min+ = 2
 *   nom  = 30
 *   max- = 59
 *   max  = 60    (ngưỡng cắt)
 * </pre>
 *
 * <p>Số test case: Standard BVA = 4n + 1 = 4×1 + 1 = <b>5</b>.
 * Robustness BVA = 6n + 1 = 6×1 + 1 = <b>7</b> (thêm min- = 0 và max+ = 61).
 *
 * <p><b>Ghi nhận ở TC6 (min- = 0):</b> chuỗi rỗng làm tiêu đề thành rỗng. Không xếp là
 * khiếm khuyết vì chính điều kiện {@code isBlank()} ngay phía trên sẽ đặt lại tiêu đề ở tin
 * nhắn kế tiếp — hệ thống tự phục hồi. Ngoài ra tầng controller đã chặn tin nhắn rỗng.
 */
@ExtendWith(MockitoExtension.class)
// sessionRepo.save / messageRepo.save được gọi nhưng không cần kiểm giá trị trả về;
// nới strictness để khỏi phải khai báo stub thừa cho từng case.
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Gia tri bien - nguong cat tieu de phien tro chuyen, 60 ky tu")
// Giữ đúng thứ tự Standard rồi mới Robustness, khớp trình tự bảng trong báo cáo.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MentorTitleStandardBvaTest {

    /** Ngưỡng cắt khai trong MentorService. */
    private static final int NGUONG_CAT = 60;
    /** Ký tự thay thế mà mã nguồn nối thêm khi cắt — dấu ba chấm một ký tự U+2026. */
    private static final String DAU_CAT = "…";

    @Mock
    private MentorSessionRepository sessionRepo;
    @Mock
    private ChatMessageRepository messageRepo;
    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private MentorService mentorService;

    /** Phiên vừa tạo luôn mang tiêu đề mặc định, nên nhánh đặt tiêu đề chắc chắn chạy. */
    private MentorSession phienMoi() {
        MentorSession session = new MentorSession();
        session.setTitle("Cuộc trò chuyện mới");
        return session;
    }

    private String cauHoiDai(int soKyTu) {
        return "a".repeat(soKyTu);
    }

    // ================================================================
    // STANDARD BVA — 4n + 1 = 5 case, TẤT CẢ đều giữ nguyên văn (clean test cases)
    // ================================================================

    /** Cột: số hiệu case · độ dài userText · giá trị biên đang xét. */
    static Stream<Arguments> standardBvaCases() {
        return Stream.of(
                Arguments.of(1, 1, "do dai = min"),
                Arguments.of(2, 2, "do dai = min+"),
                Arguments.of(3, 30, "do dai = nom"),
                Arguments.of(4, 59, "do dai = max-"),
                Arguments.of(5, 60, "do dai = max (dung nguong cat)")
        );
    }

    @ParameterizedTest(name = "TC{0} | {2} | userText = {1} ky tu -> giu NGUYEN VAN")
    @MethodSource("standardBvaCases")
    @Order(1)
    @DisplayName("Standard BVA | 4n+1 = 5 case | tieu de phai giu nguyen van, khong cat")
    void standardBva_khongCat(int tc, int doDai, String bienDangXet) {
        MentorSession session = phienMoi();
        String cauHoi = cauHoiDai(doDai);

        mentorService.sendMessage(new User(), session, cauHoi);

        assertThat(session.getTitle())
                .as("TC%d - %s", tc, bienDangXet)
                .isEqualTo(cauHoi)
                .hasSize(doDai)
                .doesNotContain(DAU_CAT);
    }

    // ================================================================
    // ROBUSTNESS BVA — thêm min- và max+ (dirty test cases)
    // ================================================================

    /**
     * TC6 — min- = 0: chuỗi rỗng vẫn đi qua nhánh "không cắt", tiêu đề thành rỗng.
     * Đây là hành vi đã biết, không phải khiếm khuyết (xem Javadoc lớp).
     */
    @ParameterizedTest(name = "TC{0} | {2} | userText = {1} ky tu")
    @MethodSource("robustnessBvaCases")
    @Order(2)
    @DisplayName("Robustness BVA | 6n+1 = 7 case | them min- va max+")
    void robustnessBva_ngoaiBien(int tc, int doDai, String bienDangXet, boolean phaiCat) {
        MentorSession session = phienMoi();
        String cauHoi = cauHoiDai(doDai);

        mentorService.sendMessage(new User(), session, cauHoi);

        if (phaiCat) {
            // Cắt đúng 60 ký tự đầu rồi nối dấu ba chấm -> tổng cộng 61 ký tự.
            assertThat(session.getTitle())
                    .as("TC%d - %s", tc, bienDangXet)
                    .isEqualTo(cauHoi.substring(0, NGUONG_CAT) + DAU_CAT)
                    .hasSize(NGUONG_CAT + 1)
                    .endsWith(DAU_CAT);
        } else {
            assertThat(session.getTitle())
                    .as("TC%d - %s", tc, bienDangXet)
                    .isEqualTo(cauHoi)
                    .hasSize(doDai);
        }
    }

    static Stream<Arguments> robustnessBvaCases() {
        return Stream.of(
                Arguments.of(6, 0, "do dai = min- (chuoi rong)", false),
                Arguments.of(7, 61, "do dai = max+ (phai bi cat)", true)
        );
    }

    // ================================================================
    // KIỂM TRA BỔ SUNG — nằm ngoài bảng giá trị biên
    // ================================================================

    /**
     * Ngưỡng 60 chỉ được áp dụng khi tiêu đề còn là mặc định. Phiên đã có tiêu đề thật thì
     * tin nhắn dài bao nhiêu cũng không được ghi đè — nếu không, mọi câu hỏi sau đều đổi
     * tên phiên và người dùng không tìm lại được cuộc trò chuyện cũ.
     */
    @ParameterizedTest(name = "Bo sung | phien da co tieu de -> khong ghi de du userText {0} ky tu")
    @MethodSource("boSungDoDai")
    @Order(3)
    @DisplayName("Bo sung | tieu de da dat thi nguong 60 khong con tac dung")
    void boSung_tieuDeDaDat_khongGhiDe(int doDai) {
        MentorSession session = new MentorSession();
        session.setTitle("Lo trinh hoc Java Backend");

        mentorService.sendMessage(new User(), session, cauHoiDai(doDai));

        assertThat(session.getTitle()).isEqualTo("Lo trinh hoc Java Backend");
    }

    static Stream<Arguments> boSungDoDai() {
        return Stream.of(Arguments.of(60), Arguments.of(61));
    }
}
