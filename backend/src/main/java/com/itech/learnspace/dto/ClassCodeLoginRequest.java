package com.itech.learnspace.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ClassCodeLoginRequest {

    @NotBlank(message = "班级码不能为空")
    private String code;

    @NotNull(message = "请选择学生")
    private Long studentId;
}
