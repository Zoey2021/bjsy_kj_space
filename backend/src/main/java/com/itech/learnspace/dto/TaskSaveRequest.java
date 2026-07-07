package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class TaskSaveRequest {
    private String title;
    private String description;
    private String taskType;
    private String configJson;
    private Integer maxScore;
    private Integer sortOrder;
}
