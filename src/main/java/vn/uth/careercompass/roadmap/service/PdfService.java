package vn.uth.careercompass.roadmap.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.dto.SkillSummaryDTO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${app.reports.skill-gap-dir:reports/skill-gap}")
    private String reportDirectory;

    public String generateSkillGapReport(User user, SkillGapResultDTO result) {
        try {
            Path directory = Path.of(reportDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);

            String fileName = "skill-gap-user-" + user.getId() + "-" + LocalDateTime.now().format(FILE_TIME) + ".txt";
            Path reportPath = directory.resolve(fileName);
            Files.writeString(reportPath, buildReportContent(user, result), StandardCharsets.UTF_8);
            return reportPath.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo báo cáo skill gap", e);
        }
    }

    private String buildReportContent(User user, SkillGapResultDTO result) {
        return """
                CAREERCOMPASS - SKILL GAP REPORT

                Student: %s
                Email: %s
                Roadmap: %s

                Required skills: %d
                Matched skills: %d
                Missing skills: %d
                Match percent: %.2f%%

                Matched:
                %s

                Missing:
                %s
                """.formatted(
                user.getFullName(),
                user.getEmail(),
                result.getTemplate().getName(),
                result.getRequiredSkillCount(),
                result.getMatchedSkillCount(),
                result.getMissingSkillCount(),
                result.getMatchPercent(),
                formatSkillList(result.getMatchedSkills()),
                formatSkillList(result.getMissingSkills())
        );
    }

    private String formatSkillList(java.util.List<SkillSummaryDTO> skills) {
        if (skills == null || skills.isEmpty()) {
            return "- None";
        }
        return skills.stream()
                .map(skill -> "- " + skill.getName())
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
