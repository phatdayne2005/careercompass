package vn.uth.careercompass.roadmap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdateRequest {
    private Long skillNodeId;
    private ProgressStatus status;
}
