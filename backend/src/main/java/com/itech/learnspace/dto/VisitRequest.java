package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class VisitRequest {
    private Long lessonId;
    private String pageUrl;
    private Integer durationSec;
}
