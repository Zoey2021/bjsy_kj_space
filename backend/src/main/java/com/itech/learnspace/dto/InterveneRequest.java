package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class InterveneRequest {
    /** remind | guide | broadcast */
    private String type;
    private Long targetStudentId;
    private Long classId;
    private Long lessonId;
    private Integer activityIndex;
    private String message;
    private String content;
}
