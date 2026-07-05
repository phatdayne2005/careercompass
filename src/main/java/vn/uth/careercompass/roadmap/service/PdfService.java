package vn.uth.careercompass.roadmap.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.dto.SkillSummaryDTO;

import java.awt.Color;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Xuất báo cáo Skill Gap ra PDF thật bằng OpenPDF (FR3.3).
 * Trả về đường dẫn file PDF đã lưu để {@code SkillGapReport.pdfPath} tham chiếu + cho tải về.
 */
@Service
@RequiredArgsConstructor
public class PdfService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Color BLUE = new Color(0x3B, 0x82, 0xF6);
    private static final Color SLATE = new Color(0x33, 0x41, 0x55);

    @Value("${app.reports.skill-gap-dir:reports/skill-gap}")
    private String reportDirectory;

    public String generateSkillGapReport(User user, SkillGapResultDTO result) {
        try {
            Path directory = Path.of(reportDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);

            String fileName = "skill-gap-user-" + user.getId() + "-" + LocalDateTime.now().format(FILE_TIME) + ".pdf";
            Path reportPath = directory.resolve(fileName);

            BaseFont base = loadUnicodeBaseFont();
            Font title = new Font(base, 20, Font.BOLD, BLUE);
            Font heading = new Font(base, 13, Font.BOLD, SLATE);
            Font body = new Font(base, 11, Font.NORMAL, Color.DARK_GRAY);

            Document doc = new Document(PageSize.A4, 50, 50, 55, 55);
            try (OutputStream out = Files.newOutputStream(reportPath)) {
                PdfWriter.getInstance(doc, out);
                doc.open();

                doc.add(new Paragraph("CareerCompass — Báo cáo Skill Gap", title));
                doc.add(spacer(8));
                doc.add(new Paragraph("Sinh viên: " + safe(user.getFullName()), body));
                doc.add(new Paragraph("Email: " + safe(user.getEmail()), body));
                doc.add(new Paragraph("Lộ trình: " + safe(result.getTemplate().getName()), body));
                doc.add(new Paragraph("Ngày xuất: " + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), body));
                doc.add(spacer(14));

                doc.add(new Paragraph("Tổng quan", heading));
                doc.add(new Paragraph("• Kỹ năng yêu cầu: " + result.getRequiredSkillCount(), body));
                doc.add(new Paragraph("• Đã đạt: " + result.getMatchedSkillCount(), body));
                doc.add(new Paragraph("• Còn thiếu: " + result.getMissingSkillCount(), body));
                doc.add(new Paragraph(String.format("• Mức khớp: %.2f%%", result.getMatchPercent()), body));
                doc.add(spacer(14));

                doc.add(new Paragraph("Kỹ năng đã đạt", heading));
                doc.add(new Paragraph(skillLines(result.getMatchedSkills()), body));
                doc.add(spacer(10));

                doc.add(new Paragraph("Kỹ năng cần học (ưu tiên)", heading));
                doc.add(new Paragraph(skillLines(result.getMissingSkills()), body));

                doc.close();
            }
            return reportPath.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo báo cáo skill gap PDF", e);
        }
    }

    /** Ưu tiên font Unicode (Arial trên Windows) để render tiếng Việt; fallback Helvetica. */
    private BaseFont loadUnicodeBaseFont() throws Exception {
        for (String path : new String[]{"C:/Windows/Fonts/arial.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"}) {
            try {
                if (Files.exists(Path.of(path))) {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) {
                // thử font tiếp theo
            }
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
    }

    private Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(height);
        return p;
    }

    private String skillLines(List<SkillSummaryDTO> skills) {
        if (skills == null || skills.isEmpty()) {
            return "— Không có —";
        }
        StringBuilder sb = new StringBuilder();
        for (SkillSummaryDTO s : skills) {
            sb.append("• ").append(s.getName());
            if (s.getCategory() != null) {
                sb.append("  (").append(s.getCategory()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
