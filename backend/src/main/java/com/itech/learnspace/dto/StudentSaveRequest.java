package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class StudentSaveRequest {
    private String username;
    private String password;
    private String realName;
    private Long classId;
    private Integer status;
    /** 学号，新增时可只填学号，由系统生成账号 */
    private String studentNo;
}
