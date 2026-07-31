package vn.uth.careercompass.onboarding.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.uth.careercompass.mentor.service.LlmClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link TranscriptAnalysisService}.
 *
 * <p>Service này trích text từ PDF (OpenPDF) rồi nhờ {@link LlmClient} (Gemini) phân tích.
 * Ta MOCK LlmClient để KHÔNG gọi API thật. Riêng phần trích PDF là I/O nội bộ (private
 * method, không mock được) nên với các nhánh cần "PDF có text thật" ta tự sinh 1 file PDF
 * nhỏ bằng OpenPDF trong {@code @TempDir} làm fixture.
 *
 * <p>Điểm mấu chốt cần test: service NUỐT mọi lỗi và trả {@code null} để luồng upload
 * transcript không bị chặn (path null / không phải PDF / PDF hỏng / LLM ném lỗi).
 */
@ExtendWith(MockitoExtension.class)
class TranscriptAnalysisServiceTest {

    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private TranscriptAnalysisService transcriptAnalysisService;

    @TempDir
    Path tempDir;

    // ------------------------------------------------------------
    // Helper: sinh 1 PDF thật có text cho trước (dùng chính OpenPDF của dự án).
    // ------------------------------------------------------------
    private Path createPdfWithText(String text) throws Exception {
        Path pdf = tempDir.resolve("transcript_" + UUID.randomUUID() + ".pdf");
        Document document = new Document();
        PdfWriter.getInstance(document, Files.newOutputStream(pdf));
        document.open();
        document.add(new Paragraph(text));
        document.close();
        return pdf;
    }

    // ============================================================
    // Các nhánh trả null MÀ KHÔNG cần đụng tới LlmClient
    // ============================================================

    @Test
    void analyze_whenPathIsNull_returnsNullAndSkipsLlm() {
        // Given: path null -> extractPdfText trả null ngay -> analyze trả null
        // When
        String result = transcriptAnalysisService.analyze(null);

        // Then
        assertThat(result).isNull();
        // Không được gọi LLM khi chưa có text.
        verify(llmClient, never()).ask(anyString());
    }

    @Test
    void analyze_whenNotPdf_returnsNullAndSkipsLlm() {
        // Given: file ảnh .png -> cần OCR, ngoài phạm vi -> extractPdfText trả null
        Path pngPath = Paths.get("bang_diem.png");

        // When
        String result = transcriptAnalysisService.analyze(pngPath);

        // Then
        assertThat(result).isNull();
        verify(llmClient, never()).ask(anyString());
    }

    @Test
    void analyze_whenPdfUnreadable_returnsNullAndSkipsLlm() {
        // Given: đuôi .pdf nhưng file không tồn tại -> Files.readAllBytes ném lỗi
        // -> extractPdfText bắt exception, trả null -> analyze trả null (nuốt lỗi).
        Path missingPdf = tempDir.resolve("khong_ton_tai.pdf");

        // When
        String result = transcriptAnalysisService.analyze(missingPdf);

        // Then
        assertThat(result).isNull();
        verify(llmClient, never()).ask(anyString());
    }

    // ============================================================
    // Các nhánh CÓ text thật -> gọi LlmClient (dùng PDF fixture)
    // ============================================================

    @Test
    void analyze_whenPdfHasText_returnsLlmAnalysis() throws Exception {
        // Given: PDF có nội dung đọc được (dùng ASCII để tránh vướng font base-14).
        String content = "Sinh vien nganh CNTT co diem trung binh cao";
        Path pdf = createPdfWithText(content);
        when(llmClient.ask(anyString())).thenReturn("Ban co the theo huong Backend.");

        // When
        String result = transcriptAnalysisService.analyze(pdf);

        // Then: trả nguyên văn kết quả LLM
        assertThat(result).isEqualTo("Ban co the theo huong Backend.");
        // Và: prompt gửi cho LLM phải chứa text vừa trích từ PDF.
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).ask(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains(content);
    }

    @Test
    void analyze_whenLlmThrows_returnsNull() throws Exception {
        // Given: PDF hợp lệ nhưng LlmClient ném lỗi (vd Gemini 503 sau khi hết retry)
        Path pdf = createPdfWithText("Diem cac mon lap trinh deu tot");
        when(llmClient.ask(anyString())).thenThrow(new RuntimeException("LLM overloaded"));

        // When
        String result = transcriptAnalysisService.analyze(pdf);

        // Then: service NUỐT lỗi và trả null để không chặn luồng upload.
        assertThat(result).isNull();
    }
}
