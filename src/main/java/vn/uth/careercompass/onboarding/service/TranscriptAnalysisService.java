package vn.uth.careercompass.onboarding.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.mentor.service.LlmClient;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * "Scan CV" — trích text từ bảng điểm PDF rồi nhờ AI (Gemini) phân tích (FR1.3).
 * Dùng chung {@link LlmClient} của P3. Trích PDF bằng OpenPDF.
 *
 * <p>Trả {@code null} khi không phân tích được (file ảnh cần OCR / PDF không có text / LLM lỗi)
 * để luồng upload transcript KHÔNG bị chặn.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TranscriptAnalysisService {

    private final LlmClient llmClient;

    public String analyze(Path transcriptPath) {
        String text = extractPdfText(transcriptPath);
        if (text == null || text.isBlank()) {
            return null;
        }
        String content = text.length() > 6000 ? text.substring(0, 6000) : text;
        try {
            return llmClient.ask(
                    "Đây là nội dung bảng điểm của một sinh viên ngành Công nghệ thông tin:\n\n" + content +
                    "\n\nHãy phân tích ngắn gọn bằng tiếng Việt (3-5 câu): điểm mạnh học thuật, môn/lĩnh vực " +
                    "nổi bật, và gợi ý định hướng nghề nghiệp phù hợp cho sinh viên này.");
        } catch (Exception e) {
            log.warn("[TranscriptAnalysisService] LLM phân tích transcript lỗi: {}", e.toString());
            return null;
        }
    }

    private String extractPdfText(Path path) {
        if (path == null || !path.toString().toLowerCase().endsWith(".pdf")) {
            return null;   // ảnh png/jpg cần OCR — ngoài phạm vi
        }
        try {
            PdfReader reader = new PdfReader(Files.readAllBytes(path));
            try {
                PdfTextExtractor extractor = new PdfTextExtractor(reader);
                StringBuilder sb = new StringBuilder();
                int pages = Math.min(reader.getNumberOfPages(), 5);
                for (int i = 1; i <= pages; i++) {
                    sb.append(extractor.getTextFromPage(i)).append('\n');
                }
                return sb.toString();
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            log.warn("[TranscriptAnalysisService] Không trích được text PDF: {}", e.toString());
            return null;
        }
    }
}
