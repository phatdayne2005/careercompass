package vn.uth.careercompass.roadmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressUpdateResponse {
    private Long skillNodeId;
    private ProgressStatus status;
    private Double completionPercent;
}
