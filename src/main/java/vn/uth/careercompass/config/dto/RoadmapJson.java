package vn.uth.careercompass.config.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

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
