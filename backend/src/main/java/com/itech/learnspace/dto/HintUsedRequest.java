package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class HintUsedRequest {
    private Long lessonId;
    private Integer activityIndex;
    private Integer hintIndex;
}
