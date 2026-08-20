package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class ClassSaveRequest {
    private String name;
    private String gradeName;
    private Long teacherId;
}
