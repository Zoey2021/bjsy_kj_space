package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.DifyRecommendService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final DifyRecommendService difyRecommendService;
    private final AuthService authService;

    public AiController(DifyRecommendService difyRecommendService, AuthService authService) {
        this.difyRecommendService = difyRecommendService;
        this.authService = authService;
    }

    private void checkTeacher() {
        SysUser user = authService.currentUser();
        if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅教师可使用 AI 推荐");
        }
    }

    @PostMapping("/recommend/{lessonId}")
    public ApiResponse<Map<String, Object>> recommend(@PathVariable Long lessonId) {
        checkTeacher();
        SysUser user = authService.currentUser();
        return ApiResponse.ok(difyRecommendService.recommendActivities(lessonId, user.getId()));
    }

    @PostMapping("/recommend/{lessonId}/adopt")
    public ApiResponse<Void> adopt(@PathVariable Long lessonId, @RequestBody Map<String, Object> body) {
        checkTeacher();
        return ApiResponse.ok(null);
    }
}
