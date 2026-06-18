package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String username;
    private String password;
    private String realName;
    private String role;
    private Long classId;
}
