package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class LessonSaveRequest {
    private Long unitId;
    private String title;
    private String content;
    private Integer sortOrder;
    private Integer durationMin;
}
