package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.dto.InterveneRequest;
import com.itech.learnspace.dto.SetCurrentLessonRequest;
import com.itech.learnspace.entity.LearnNotification;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final NotificationService notificationService;
    private final AuthService authService;

    public TeacherController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    private SysUser checkTeacher() {
        SysUser user = authService.currentUser();
        if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅教师可操作");
        }
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
}
