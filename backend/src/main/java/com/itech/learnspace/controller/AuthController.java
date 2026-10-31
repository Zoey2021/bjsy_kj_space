package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.dto.ClassCodeLoginRequest;
import com.itech.learnspace.dto.LoginRequest;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.DingTalkAuthService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final DingTalkAuthService dingTalkAuthService;

    public AuthController(AuthService authService, DingTalkAuthService dingTalkAuthService) {
        this.authService = authService;
        this.dingTalkAuthService = dingTalkAuthService;
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

    /** 学生修改自己的登录密码。2021级6班会记下新密码，供教师查看。 */
    @PostMapping("/change-password")
    public ApiResponse<Map<String, Object>> changePassword(@RequestBody Map<String, String> body) {
        String oldPassword = body == null ? null : body.get("oldPassword");
        String newPassword = body == null ? null : body.get("newPassword");
        return ApiResponse.ok(authService.changeOwnPassword(oldPassword, newPassword));
    }

    /** 班级码：查询可选学生名单（无需登录） */
    @GetMapping("/class-code/{code}/students")
    public ApiResponse<Map<String, Object>> classCodeStudents(@PathVariable String code) {
        return ApiResponse.ok(authService.getStudentsByClassCode(code));
    }

    /** 教师钉钉登录是否已配置，以及跳转地址 */
    @GetMapping("/dingtalk/url")
    public ApiResponse<Map<String, Object>> dingTalkUrl() {
        return ApiResponse.ok(dingTalkAuthService.authorizeInfo());
    }

    /** 钉钉回调。只接受本校组织内、且平台已开通的教师。 */
    @GetMapping("/dingtalk/callback")
    public void dingTalkCallback(@RequestParam(value = "authCode", required = false) String authCode,
                                 @RequestParam(value = "code", required = false) String code,
                                 @RequestParam(value = "state", required = false) String state,
                                 @RequestParam(value = "error", required = false) String error,
                                 HttpServletResponse response) throws IOException {
        String credential = authCode != null && authCode.length() > 0 ? authCode : code;
        response.sendRedirect(dingTalkAuthService.callbackRedirect(credential, state, error));
    }

    /** 班级码：学生选名登录（无需密码） */
    @PostMapping("/class-code/login")
    public ApiResponse<Map<String, Object>> classCodeLogin(@Valid @RequestBody ClassCodeLoginRequest request) {
        return ApiResponse.ok(authService.loginByClassCode(request.getCode(), request.getStudentId()));
    }
}
