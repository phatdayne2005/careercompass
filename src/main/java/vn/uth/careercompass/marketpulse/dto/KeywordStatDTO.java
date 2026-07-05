package vn.uth.careercompass.marketpulse.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KeywordStatDTO {
    private String keyword;
    private int count;
}