package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.DashboardService;
import com.itech.learnspace.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SseService sseService;
    private final AuthService authService;

    public DashboardController(DashboardService dashboardService, SseService sseService,
                               AuthService authService) {
        this.dashboardService = dashboardService;
        this.sseService = sseService;
        this.authService = authService;
    }

    private void checkTeacher() {
        SysUser user = authService.currentUser();
        if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅教师可访问看板");
        }
    }

    /** 教师所教班级列表 */
    @GetMapping("/classes")
    public ApiResponse<List<SysClass>> classes() {
        checkTeacher();
        SysUser user = authService.currentUser();
        if ("ADMIN".equals(user.getRole())) {
            return ApiResponse.ok(new java.util.ArrayList<SysClass>());
        }
        return ApiResponse.ok(dashboardService.getTeacherClasses(user.getId()));
    }

    /** 班级学情看板数据（REST 查询） */
    @GetMapping("/data/{classId}")
    public ApiResponse<Map<String, Object>> data(@PathVariable Long classId) {
        checkTeacher();
        SysUser user = authService.currentUser();
        if ("TEACHER".equals(user.getRole())) {
            dashboardService.checkTeacherOwnsClass(user.getId(), classId);
        }
        return ApiResponse.ok(dashboardService.buildDashboardData(classId));
    }

    /**
     * SSE 实时推送接口（核心亮点）
     * 教师看板页面用 EventSource 连接此接口，学生提交后自动收到更新
     * 注意：EventSource 不支持自定义请求头，所以 Token 通过 URL 参数传递
     */
    @GetMapping(value = "/sse/{classId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse(@PathVariable Long classId) {
        checkTeacher();
        SysUser user = authService.currentUser();
        if ("TEACHER".equals(user.getRole())) {
            dashboardService.checkTeacherOwnsClass(user.getId(), classId);
        }
        return sseService.subscribe(classId);
    }

    /** 全班完成矩阵 */
    @GetMapping("/matrix/{classId}")
    public ApiResponse<Map<String, Object>> matrix(@PathVariable Long classId) {
        checkTeacher();
        SysUser user = authService.currentUser();
        if ("TEACHER".equals(user.getRole())) {
            dashboardService.checkTeacherOwnsClass(user.getId(), classId);
        }
        return ApiResponse.ok(dashboardService.buildMatrix(classId));
    }

    /** 班级积分排行 */
    @GetMapping("/ranking/{classId}")
    public ApiResponse<List<Map<String, Object>>> ranking(@PathVariable Long classId) {
        return ApiResponse.ok(dashboardService.getClassRanking(classId));
    }

    /** 课时活动维度看板（全班学习情况看板） */
    @GetMapping("/lesson/{lessonId}/class/{classId}")
    public ApiResponse<Map<String, Object>> lessonActivityDashboard(
            @PathVariable Long lessonId,
            @PathVariable Long classId) {
        checkTeacher();
        SysUser user = authService.currentUser();
        if ("TEACHER".equals(user.getRole())) {
            dashboardService.checkTeacherOwnsClass(user.getId(), classId);
        }
        return ApiResponse.ok(dashboardService.buildLessonActivityDashboard(lessonId, classId));
    }

    /** 获取或生成班级登录码（教师选班后展示给学生） */
    @GetMapping("/class/{classId}/login-code")
    public ApiResponse<Map<String, Object>> classLoginCode(@PathVariable Long classId) {
        checkTeacher();
        SysUser user = authService.currentUser();
        dashboardService.checkTeacherOwnsClass(user.getId(), classId);
        return ApiResponse.ok(authService.getOrGenerateClassLoginCode(classId, user.getId()));
    }

    /** 刷新班级登录码 */
    @PostMapping("/class/{classId}/login-code/refresh")
    public ApiResponse<Map<String, Object>> refreshClassLoginCode(@PathVariable Long classId) {
        checkTeacher();
        SysUser user = authService.currentUser();
        dashboardService.checkTeacherOwnsClass(user.getId(), classId);
        return ApiResponse.ok(authService.refreshClassLoginCode(classId, user.getId()));
    }
}
