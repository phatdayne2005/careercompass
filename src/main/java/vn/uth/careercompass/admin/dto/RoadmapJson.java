package vn.uth.careercompass.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * DTO ánh xạ file roadmap xuất từ roadmap.sh (đặt trong {@code resources/data/*.json}).
 * Chỉ đọc các trường cần thiết để dựng SkillTree; {@code @JsonIgnoreProperties} bỏ qua phần dư.
 * Thuộc gói P7 (Admin/Counselor) — nguồn dữ liệu để seed SkillTreeTemplate + SkillNode.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoadmapJson {
    private TitleInfo title;
    private String description;
    private String slug;
    private List<NodeInfo> nodes;
    private List<EdgeInfo> edges;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TitleInfo {
        private String page;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeInfo {
        private String id;
        private String type;
        private NodeData data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeData {
        private String label;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EdgeInfo {
        private String source;
        private String target;
    }
}
