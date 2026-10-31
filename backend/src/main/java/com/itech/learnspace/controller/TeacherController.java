package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.dto.InterveneRequest;
import com.itech.learnspace.dto.MallOpenRequest;
import com.itech.learnspace.dto.ParkReviewRequest;
import com.itech.learnspace.dto.PointsRulesRequest;
import com.itech.learnspace.dto.SetCurrentLessonRequest;
import com.itech.learnspace.dto.TierOverrideRequest;
import com.itech.learnspace.entity.LearnNotification;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.NotificationService;
import com.itech.learnspace.service.ParkService;
import com.itech.learnspace.service.PointsMallService;
import com.itech.learnspace.service.PretestG4Service;
import com.itech.learnspace.service.PretestG6Service;
import com.itech.learnspace.service.ScaffoldService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final NotificationService notificationService;
    private final ParkService parkService;
    private final PretestG4Service pretestG4Service;
    private final PretestG6Service pretestG6Service;
    private final AuthService authService;
    private final PointsMallService pointsMallService;
    private final ScaffoldService scaffoldService;
    private final com.itech.learnspace.service.AccountAccessService accountAccessService;

    public TeacherController(NotificationService notificationService, ParkService parkService,
                             PretestG4Service pretestG4Service, PretestG6Service pretestG6Service,
                             AuthService authService, PointsMallService pointsMallService,
                             ScaffoldService scaffoldService,
                             com.itech.learnspace.service.AccountAccessService accountAccessService) {
        this.notificationService = notificationService;
        this.parkService = parkService;
        this.pretestG4Service = pretestG4Service;
        this.pretestG6Service = pretestG6Service;
        this.authService = authService;
        this.pointsMallService = pointsMallService;
        this.scaffoldService = scaffoldService;
        this.accountAccessService = accountAccessService;
    }

    private SysUser checkTeacher() {
        SysUser user = authService.currentUser();
        if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅教师可操作");
        }
        accountAccessService.assertClassData(user);
        return user;
    }

    /** 教师向学生发送干预消息 */
    @PostMapping("/intervene")
    public ApiResponse<LearnNotification> intervene(@RequestBody InterveneRequest request) {
        SysUser teacher = checkTeacher();
        return ApiResponse.ok("发送成功", notificationService.intervene(request, teacher.getId()));
    }

    /** 标记班级当前进行中的课时 */
    @PutMapping("/class/{classId}/current-lesson")
    public ApiResponse<String> setCurrentLesson(@PathVariable Long classId,
                                                @RequestBody SetCurrentLessonRequest body) {
        SysUser teacher = checkTeacher();
        notificationService.setCurrentLesson(classId, body.getLessonId(), teacher.getId());
        return ApiResponse.ok("已设为当前课时", "ok");
    }

    /** 游学乐园申请列表 */
    @GetMapping("/park/applications")
    public ApiResponse<List<Map<String, Object>>> parkApplications(@RequestParam Long classId,
                                                                    @RequestParam(required = false) Long lessonId) {
        SysUser teacher = checkTeacher();
        return ApiResponse.ok(parkService.listApplications(classId, lessonId, teacher.getId()));
    }

    /** 审批游学乐园申请：approve / reject / revoke */
    @PostMapping("/park/review")
    public ApiResponse<Map<String, Object>> parkReview(@RequestBody ParkReviewRequest request) {
        SysUser teacher = checkTeacher();
        return ApiResponse.ok("操作成功", parkService.review(request, teacher.getId()));
    }

    /** 一键授权或一键关闭本班游学乐园 */
    @PostMapping("/park/review-class")
    public ApiResponse<Map<String, Object>> parkReviewClass(@RequestBody Map<String, Object> body) {
        SysUser teacher = checkTeacher();
        Object classId = body.get("classId");
        Object lessonId = body.get("lessonId");
        if (classId == null || lessonId == null) {
            throw new BusinessException("请选择班级和课时");
        }
        return ApiResponse.ok("操作成功", parkService.reviewClass(
                Long.valueOf(String.valueOf(classId)),
                Long.valueOf(String.valueOf(lessonId)),
                body.get("action") == null ? "" : String.valueOf(body.get("action")),
                teacher.getId()));
    }

    /** 游学乐园游戏开关与参数 */
    @GetMapping("/park/settings")
    public ApiResponse<Map<String, Object>> parkSettings() {
        checkTeacher();
        return ApiResponse.ok(parkService.getSettings());
    }

    @PutMapping("/park/settings")
    public ApiResponse<Map<String, Object>> saveParkSettings(@RequestBody Map<String, Object> body) {
        checkTeacher();
        return ApiResponse.ok("已保存", parkService.saveSettings(body));
    }

    /** 游学乐园游玩人数与时长 */
    @GetMapping("/park/plays")
    public ApiResponse<Map<String, Object>> parkPlays(@RequestParam Long classId,
                                                       @RequestParam(required = false) Long lessonId) {
        SysUser teacher = checkTeacher();
        return ApiResponse.ok(parkService.playStats(classId, lessonId, teacher.getId()));
    }

    @GetMapping("/pretest/g4")
    public ApiResponse<Map<String, Object>> pretestG4(@RequestParam Long classId) {
        SysUser teacher = checkTeacher();
        return ApiResponse.ok(pretestG4Service.classOverview(teacher.getId(), teacher.getRole(), classId));
    }

    @GetMapping("/pretest/g6")
    public ApiResponse<Map<String, Object>> pretestG6(@RequestParam Long classId) {
        SysUser teacher = checkTeacher();
        return ApiResponse.ok(pretestG6Service.classOverview(teacher.getId(), teacher.getRole(), classId));
    }

    @DeleteMapping("/pretest/g6")
    public ApiResponse<Integer> clearPretestG6(@RequestParam Long classId) {
        SysUser teacher = checkTeacher();
        int deleted = pretestG6Service.clearClass(teacher.getId(), teacher.getRole(), classId);
        return ApiResponse.ok("已清空 " + deleted + " 条提交", deleted);
    }

    @GetMapping("/points/rules")
    public ApiResponse<Map<String, Object>> pointsRules() {
        checkTeacher();
        return ApiResponse.ok(pointsMallService.getRules());
    }

    @PutMapping("/points/rules")
    public ApiResponse<Map<String, Object>> savePointsRules(@RequestBody PointsRulesRequest request) {
        checkTeacher();
        return ApiResponse.ok("规则已保存", pointsMallService.saveRules(request));
    }

    @GetMapping("/mall")
    public ApiResponse<Map<String, Object>> mall(@RequestParam(required = false) Long classId) {
        checkTeacher();
        return ApiResponse.ok(pointsMallService.teacherMall(classId));
    }

    @GetMapping("/tiers")
    public ApiResponse<Map<String, Object>> classTiers(@RequestParam Long classId) {
        SysUser teacher = checkTeacher();
        return ApiResponse.ok(scaffoldService.classTiers(teacher.getId(), teacher.getRole(), classId));
    }

    @PostMapping("/tiers/override")
    public ApiResponse<Map<String, Object>> overrideTier(@RequestBody TierOverrideRequest request) {
        SysUser teacher = checkTeacher();
        return ApiResponse.ok("档位已更新", scaffoldService.overrideTier(teacher.getId(), teacher.getRole(), request));
    }

    @PutMapping("/mall/open")
    public ApiResponse<Map<String, Object>> mallOpen(@RequestBody MallOpenRequest request) {
        SysUser teacher = checkTeacher();
        Map<String, Object> result = pointsMallService.setApplyOpen(teacher.getId(), teacher.getRole(), request);
        boolean open = Boolean.TRUE.equals(result.get("applyOpen"));
        return ApiResponse.ok(open ? "已开放兑换申请" : "已关闭兑换申请", result);
    }

    @PostMapping("/points/reset")
    public ApiResponse<Map<String, Object>> resetClassPoints(@RequestParam Long classId) {
        SysUser teacher = checkTeacher();
        Map<String, Object> result = pointsMallService.resetClassPoints(teacher.getId(), teacher.getRole(), classId);
        return ApiResponse.ok("已重置本班积分", result);
    }
}
