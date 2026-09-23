package vn.uth.careercompass.mentor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Kiểm thử máy khách gọi mô hình ngôn ngữ lớn (FR1.2, AC-1.2.2, NFR-R01).
 *
 * Trước lớp này LlmClient mới phủ 16% dòng và 0% nhánh — phần chưa phủ chính là vòng thử
 * lại, thứ quyết định ứng dụng có chịu nổi lúc Gemini quá tải hay không. Gói miễn phí của
 * Gemini trả 503 và 429 khá thường xuyên, nên vòng này chạy thật trong lúc dùng.
 *
 * Điểm phân biệt quan trọng: 503 và 429 ĐƯỢC thử lại, còn 400 thì KHÔNG — thử lại một
 * request sai định dạng chỉ tốn thêm 7 giây rồi vẫn hỏng. Khối catch bắt đúng
 * HttpServerErrorException và HttpClientErrorException.TooManyRequests; nếu ai đó nới
 * thành HttpClientErrorException thì mọi lỗi 4xx đều bị thử lại vô ích.
 *
 * MockRestServiceServer thay cho máy chủ Gemini thật nên không tốn hạn mức gọi API và
 * không cần mạng. RestClient được LlmClient tự dựng trong hàm khởi tạo nên phải thay bằng
 * ReflectionTestUtils — đây là chỗ duy nhất trong bộ kiểm thử phải làm vậy.
 */
@DisplayName("FR1.2 — Máy khách gọi mô hình ngôn ngữ lớn")
class LlmClientTest {

    private static final String DUONG_DAN =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent";

    private static final String TRA_LOI_THANH_CONG = """
            {"candidates":[{"content":{"parts":[{"text":"Em nên học Java và Spring Boot."}]}}]}
            """;

    private LlmClient llmClient;
    private MockRestServiceServer server;

    @BeforeEach
    void chuanBi() {
        llmClient = new LlmClient();
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(llmClient, "restClient", builder.build());
        ReflectionTestUtils.setField(llmClient, "apiKey", "khoa-test");
        ReflectionTestUtils.setField(llmClient, "model", "gemini-test");
    }

    @Test
    @DisplayName("Gọi thành công thì bóc đúng đoạn văn bản trong phản hồi lồng nhau")
    void goiThanhCong_bocDungVanBan() {
        server.expect(requestTo(DUONG_DAN))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "khoa-test"))
                // Câu hỏi phải nằm đúng chỗ contents[0].parts[0].text theo đặc tả Gemini.
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("Em nên học gì?"))
                .andRespond(withSuccess(TRA_LOI_THANH_CONG, MediaType.APPLICATION_JSON));

        String ketQua = llmClient.ask("Em nên học gì?");

        assertThat(ketQua).isEqualTo("Em nên học Java và Spring Boot.");
        server.verify();
    }

    @Test
    @DisplayName("Gemini trả 503 rồi thành công thì kết quả vẫn về, người dùng không thấy lỗi")
    void quaTaiRoiThanhCong_ketQuaVanVe() {
        server.expect(requestTo(DUONG_DAN))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(DUONG_DAN))
                .andRespond(withSuccess(TRA_LOI_THANH_CONG, MediaType.APPLICATION_JSON));

        assertThat(llmClient.ask("Em nên học gì?"))
                .isEqualTo("Em nên học Java và Spring Boot.");
        server.verify();
    }

    @Test
    @DisplayName("Gemini trả 429 (vượt hạn mức) cũng được thử lại")
    void vuotHanMuc_duocThuLai() {
        server.expect(requestTo(DUONG_DAN))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(DUONG_DAN))
                .andRespond(withSuccess(TRA_LOI_THANH_CONG, MediaType.APPLICATION_JSON));

        assertThat(llmClient.ask("Em nên học gì?")).isNotBlank();
        server.verify();
    }

    @Test
    @DisplayName("AC-1.2.2 · Khoá API sai trả 400 thì hỏng ngay, KHÔNG thử lại vô ích")
    void khoaApiSai_hongNgayKhongThuLai() {
        // Đúng MỘT lần gọi: 400 nằm ngoài khối catch nên thoát vòng lặp lập tức.
        server.expect(times(1), requestTo(DUONG_DAN))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> llmClient.ask("Em nên học gì?"))
                .isInstanceOf(HttpClientErrorException.BadRequest.class);

        // Nếu khối catch bị nới thành HttpClientErrorException thì server.verify() sẽ đỏ
        // vì có thêm ba lần gọi thừa.
        server.verify();
    }

    @Test
    @DisplayName("NFR-R01 · Gemini hỏng suốt thì ném lỗi cuối để tầng trên chuyển sang phương án dự phòng")
    void hongSuot_nemLoiCuoiChoTangTrenXuLy() {
        server.expect(times(4), requestTo(DUONG_DAN))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> llmClient.ask("Em nên học gì?"))
                .as("MentorService bắt ngoại lệ này để trả thông điệp dự phòng (AC-1.1.2)")
                .isInstanceOf(org.springframework.web.client.HttpServerErrorException.class);

        // Đúng bốn lần thử, không nhiều hơn không ít hơn.
        server.verify();
    }

    @org.junit.jupiter.api.Test
    @DisplayName("Luồng bị ngắt giữa lúc chờ backoff thì dừng lịch sự, không nuốt tín hiệu ngắt")
    void biNgatGiuaLucChoBackoff_giuLaiCoNgat() {
        // Khi ứng dụng tắt, Spring ngắt các luồng đang chờ. sleepQuietly bắt
        // InterruptedException rồi ĐẶT LẠI cờ ngắt — nuốt cờ đi là luồng không bao giờ
        // dừng được và quá trình tắt máy treo.
        server.expect(times(4), requestTo(DUONG_DAN))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> llmClient.ask("Em nên học gì?"))
                    .isInstanceOf(org.springframework.web.client.HttpServerErrorException.class);

            assertThat(Thread.currentThread().isInterrupted())
                    .as("cờ ngắt phải được đặt lại sau khi bắt InterruptedException")
                    .isTrue();
        } finally {
            // Dọn cờ để không ảnh hưởng phép kiểm chạy sau trên cùng luồng.
            Thread.interrupted();
        }
    }
}
