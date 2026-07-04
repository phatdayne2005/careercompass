package vn.uth.careercompass.roadmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một tài liệu học của node roadmap (đọc từ bảng learning_resources do P7 seed).
 * Dùng để render panel "Tài nguyên học ≥2 link" (FR2.3) trên trang roadmap.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapResourceDTO {
    private String title;
    private String url;
    private String type;   // DOCUMENTATION | VIDEO | COURSE | ARTICLE...
}
