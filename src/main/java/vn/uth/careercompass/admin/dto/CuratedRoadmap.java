package vn.uth.careercompass.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Format lộ trình CURATE của riêng dự án (đặt trong {@code resources/data/roadmaps/*.json}).
 * Map thẳng 1-1 vào schema document: 1 file = 1 CareerRole + 1 SkillTreeTemplate + N SkillNode.
 *
 * <p>Khác với `RoadmapJson` (định dạng thô của roadmap.sh, chỉ dùng làm nguồn cho scraper):
 * format này mỗi node là 1 <b>skill cụ thể</b> (Java, SQL, Docker...) — đúng khái niệm Skill của
 * onboarding/skill-gap/portfolio — kèm tier + link học sẵn (≥2/node).</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CuratedRoadmap {
    private String role;             // tên CareerRole (unique)
    private String description;
    private String salaryRange;
    private String demand;
    private List<Node> nodes;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Node {
        private Integer tier;        // 1 Nền tảng · 2 Cốt lõi · 3 Nâng cao
        private String skill;        // tên Skill cụ thể (dùng chung catalog, dedup theo tên)
        private String category;     // nhóm skill (Language, Framework, Tools...)
        private String parent;       // tên skill node cha trong CÙNG roadmap (null nếu là gốc)
        private List<Link> links;    // >=2 tài liệu học
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Link {
        private String title;
        private String url;
        private String type;         // DOCUMENTATION | VIDEO | COURSE | ARTICLE...
    }
}
