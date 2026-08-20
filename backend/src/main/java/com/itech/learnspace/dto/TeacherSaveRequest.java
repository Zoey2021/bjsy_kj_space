package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class TeacherSaveRequest {
    private String username;
    private String password;
    private String realName;
    private Integer status;
}
