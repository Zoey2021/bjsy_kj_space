package com.itech.learnspace.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ScaffoldGenerateRequest {
    private Long lessonId;
    private Long templateId;
    /** 活动序号，或字符串 all */
    private Object activityIndex;
    private Map<String, Object> tierDefinitions;
}
