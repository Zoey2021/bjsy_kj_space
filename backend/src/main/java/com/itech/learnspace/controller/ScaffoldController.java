package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.dto.ScaffoldGenerateRequest;
import com.itech.learnspace.dto.ScaffoldPublishRequest;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.ScaffoldService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/scaffold")
public class ScaffoldController {

    private final AuthService authService;
    private final ScaffoldService scaffoldService;

    public ScaffoldController(AuthService authService, ScaffoldService scaffoldService) {
        this.authService = authService;
        this.scaffoldService = scaffoldService;
    }

    private SysUser checkTeacher() {
        SysUser user = authService.currentUser();
        if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅教师可操作");
        }
        return user;
    }

    @GetMapping("/templates")
    public ApiResponse<List<Map<String, Object>>> templates() {
        checkTeacher();
        return ApiResponse.ok(scaffoldService.listTemplates());
    }

    @PostMapping("/templates")
    public ApiResponse<Map<String, Object>> saveTemplate(@RequestBody Map<String, Object> body) {
        checkTeacher();
        return ApiResponse.ok("模板已保存", scaffoldService.saveTemplate(body));
    }

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody ScaffoldGenerateRequest request) {
        checkTeacher();
        return ApiResponse.ok("草稿已生成", scaffoldService.generate(request));
    }

    @GetMapping("/{lessonId:\\d+}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long lessonId) {
        checkTeacher();
        return ApiResponse.ok(scaffoldService.getLessonScaffold(lessonId));
    }

    @PostMapping("/{lessonId:\\d+}")
    public ApiResponse<Map<String, Object>> saveDraft(@PathVariable Long lessonId,
                                                      @RequestBody ScaffoldPublishRequest request) {
        checkTeacher();
        return ApiResponse.ok("草稿已保存", scaffoldService.saveDraft(lessonId, request));
    }

    @PostMapping("/{lessonId:\\d+}/publish")
    public ApiResponse<Map<String, Object>> publish(@PathVariable Long lessonId,
                                                    @RequestBody ScaffoldPublishRequest request) {
        checkTeacher();
        return ApiResponse.ok("已发布，学生将按各自任务学习", scaffoldService.publish(lessonId, request));
    }
}
