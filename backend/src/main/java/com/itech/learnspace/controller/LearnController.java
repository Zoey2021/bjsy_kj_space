package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.dto.SubmitRequest;
import com.itech.learnspace.dto.VisitRequest;
import com.itech.learnspace.entity.LearnSubmission;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.LearnService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/learn")
public class LearnController {

    private final LearnService learnService;
    private final AuthService authService;

    public LearnController(LearnService learnService, AuthService authService) {
        this.learnService = learnService;
        this.authService = authService;
    }

    /**
     * 学生提交学习任务
     * 提交成功后后端会通过 SSE 推送，教师看板自动更新
     */
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
}
