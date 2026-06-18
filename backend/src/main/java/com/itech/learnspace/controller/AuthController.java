package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.dto.ClassCodeLoginRequest;
import com.itech.learnspace.dto.LoginRequest;
import com.itech.learnspace.service.AuthService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 统一登录接口，返回 token 和角色信息 */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.ok(authService.currentUserProfile());
    }

    /** 班级码：查询可选学生名单（无需登录） */
    @GetMapping("/class-code/{code}/students")
    public ApiResponse<Map<String, Object>> classCodeStudents(@PathVariable String code) {
        return ApiResponse.ok(authService.getStudentsByClassCode(code));
    }

    /** 班级码：学生选名登录（无需密码） */
    @PostMapping("/class-code/login")
    public ApiResponse<Map<String, Object>> classCodeLogin(@Valid @RequestBody ClassCodeLoginRequest request) {
        return ApiResponse.ok(authService.loginByClassCode(request.getCode(), request.getStudentId()));
    }
}
