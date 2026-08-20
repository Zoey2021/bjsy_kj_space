package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class StudentSaveRequest {
    private String username;
    private String password;
    private String realName;
    private Long classId;
    private Integer status;
}
