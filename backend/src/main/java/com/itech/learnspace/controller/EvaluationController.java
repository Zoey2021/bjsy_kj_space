package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AccountAccessService;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.EvaluationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final AuthService authService;
    private final AccountAccessService accountAccessService;

    public EvaluationController(EvaluationService evaluationService,
                                AuthService authService,
                                AccountAccessService accountAccessService) {
        this.evaluationService = evaluationService;
        this.authService = authService;
        this.accountAccessService = accountAccessService;
    }

    private SysUser checkTeacher() {
        SysUser user = authService.currentUser();
        if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅教师可访问评价中心");
        }
        accountAccessService.assertClassData(user);
        return user;
    }

    /** 班级花名册 + 综合等级 */
    @GetMapping("/class/{classId}/roster")
    public ApiResponse<Map<String, Object>> roster(@PathVariable Long classId,
                                                   @RequestParam Long gradeId) {
        SysUser user = checkTeacher();
        evaluationService.assertOwnsClass(user.getId(), classId);
        return ApiResponse.ok(evaluationService.classRoster(classId, gradeId));
    }

    /** 学生跨课档案 */
    @GetMapping("/class/{classId}/student/{studentId}")
    public ApiResponse<Map<String, Object>> studentProfile(@PathVariable Long classId,
                                                           @PathVariable Long studentId,
                                                           @RequestParam Long gradeId) {
        SysUser user = checkTeacher();
        evaluationService.assertOwnsClass(user.getId(), classId);
        return ApiResponse.ok(evaluationService.studentProfile(classId, studentId, gradeId));
    }

    /** 单课过程档案 */
    @GetMapping("/class/{classId}/student/{studentId}/lesson/{lessonId}")
    public ApiResponse<Map<String, Object>> lessonDetail(@PathVariable Long classId,
                                                         @PathVariable Long studentId,
                                                         @PathVariable Long lessonId) {
        SysUser user = checkTeacher();
        evaluationService.assertOwnsClass(user.getId(), classId);
        return ApiResponse.ok(evaluationService.lessonDetail(classId, studentId, lessonId));
    }
}
