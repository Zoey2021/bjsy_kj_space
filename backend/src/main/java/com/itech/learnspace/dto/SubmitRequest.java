package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class SubmitRequest {
    private Long taskId;
    private String contentJson;
    private Integer studySeconds;
}
