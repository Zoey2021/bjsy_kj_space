package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.dto.SubmitRequest;
import com.itech.learnspace.dto.VisitRequest;
import com.itech.learnspace.entity.LearnSubmission;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.LearnService;
import com.itech.learnspace.service.NotificationService;
import com.itech.learnspace.service.ParkService;
import com.itech.learnspace.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learn")
public class LearnController {

    private final LearnService learnService;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final ParkService parkService;
    private final SseService sseService;

    public LearnController(LearnService learnService, AuthService authService,
                           NotificationService notificationService, ParkService parkService,
                           SseService sseService) {
        this.learnService = learnService;
        this.authService = authService;
        this.notificationService = notificationService;
        this.parkService = parkService;
        this.sseService = sseService;
    }

    @PostMapping("/submit")
    public ApiResponse<LearnSubmission> submit(@RequestBody SubmitRequest request) {
        SysUser user = authService.currentUser();
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生可提交");
        }
        return ApiResponse.ok("提交成功", learnService.submit(request, user.getId()));
    }

    @PostMapping("/visit")
    public ApiResponse<String> visit(@RequestBody VisitRequest request) {
        SysUser user = authService.currentUser();
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生可记录访问");
        }
        learnService.recordVisit(request, user.getId());
        return ApiResponse.ok("记录成功", "ok");
    }

    @GetMapping("/records")
    public ApiResponse<Map<String, Object>> myRecords() {
        SysUser user = authService.currentUser();
        return ApiResponse.ok(learnService.getMyRecords(user.getId()));
    }

    @GetMapping("/records/lesson/{lessonId}")
    public ApiResponse<Map<String, Object>> lessonRecords(@PathVariable Long lessonId) {
        SysUser user = authService.currentUser();
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生可查看");
        }
        return ApiResponse.ok(learnService.getLessonRecords(lessonId, user.getId()));
    }

    /** 学生登录后获取班级当前课时 */
    @GetMapping("/current-lesson")
    public ApiResponse<Map<String, Object>> currentLesson() {
        SysUser user = authService.currentUser();
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生可访问");
        }
        return ApiResponse.ok(notificationService.getCurrentLessonForStudent(user.getId()));
    }

    /** 学生端 SSE：接收教师干预消息 */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter studentSse() {
        SysUser user = authService.currentUser();
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生可连接");
        }
        return sseService.subscribeStudent(user.getId());
    }

    /** 拉取离线期间通知 */
    @GetMapping("/notifications")
    public ApiResponse<List<Map<String, Object>>> notifications(
            @RequestParam(required = false) Long since) {
        SysUser user = authService.currentUser();
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生可访问");
        }
        return ApiResponse.ok(notificationService.getNotificationsSince(user.getId(), since));
    }

    /** 游学乐园：当前状态（是否可申请 / 是否已解锁） */
    @GetMapping("/park/status")
    public ApiResponse<Map<String, Object>> parkStatus() {
        SysUser user = authService.currentUser();
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生可访问");
        }
        return ApiResponse.ok(parkService.getStatus(user.getId()));
    }

    /** 游学乐园：学生提交开启申请 */
    @PostMapping("/park/apply")
    public ApiResponse<Map<String, Object>> parkApply() {
        SysUser user = authService.currentUser();
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException(403, "仅学生可申请");
        }
        return ApiResponse.ok("申请已提交", parkService.apply(user.getId()));
    }
}
