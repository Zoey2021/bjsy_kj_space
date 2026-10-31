package com.itech.learnspace.dto;

import lombok.Data;

import java.util.List;

@Data
public class TeacherClassMatchRequest {
    private List<Long> classIds;
}
