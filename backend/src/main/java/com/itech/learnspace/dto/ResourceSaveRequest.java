package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class ResourceSaveRequest {
    private Long lessonId;
    private String title;
    private String resType;
    private String contentText;
    private String contentUrl;
    private Integer sortOrder;
}
