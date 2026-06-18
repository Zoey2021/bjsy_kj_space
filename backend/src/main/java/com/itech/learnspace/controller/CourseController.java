package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    private final CourseService courseService;
    private final AuthService authService;

    public CourseController(CourseService courseService, AuthService authService) {
        this.courseService = courseService;
        this.authService = authService;
    }

    /** 教材册列表（配套 MAIN / 校本 SCHOOL） */
    @GetMapping("/textbooks")
    public ApiResponse<List<Map<String, Object>>> textbooks(@RequestParam(defaultValue = "MAIN") String type) {
        return ApiResponse.ok(courseService.listTextbooks(type));
    }

    /** 某一册下的单元与课时树 */
    @GetMapping("/grade/{gradeId}/outline")
    public ApiResponse<Map<String, Object>> gradeOutline(@PathVariable Long gradeId) {
        SysUser user = authService.currentUser();
        Long studentId = "STUDENT".equals(user.getRole()) ? user.getId() : null;
        return ApiResponse.ok(courseService.getGradeOutline(gradeId, studentId));
    }

    /** 全量课程树（兼容） */
    @GetMapping("/map")
    public ApiResponse<List<Map<String, Object>>> getMap() {
        SysUser user = authService.currentUser();
        Long studentId = "STUDENT".equals(user.getRole()) ? user.getId() : null;
        return ApiResponse.ok(courseService.getCourseMap(studentId));
    }

    /** 课时详情（资源+任务） */
    @GetMapping("/lesson/{lessonId}")
    public ApiResponse<Map<String, Object>> getLesson(@PathVariable Long lessonId) {
        SysUser user = authService.currentUser();
        Long studentId = "STUDENT".equals(user.getRole()) ? user.getId() : null;
        return ApiResponse.ok(courseService.getLessonDetail(lessonId, studentId));
    }
}
