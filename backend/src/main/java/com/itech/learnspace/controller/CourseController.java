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
    private final com.itech.learnspace.service.AccountAccessService accountAccessService;

    public CourseController(CourseService courseService, AuthService authService,
                            com.itech.learnspace.service.AccountAccessService accountAccessService) {
        this.courseService = courseService;
        this.authService = authService;
        this.accountAccessService = accountAccessService;
    }

    /** 教材册列表（配套 MAIN / 校本 SCHOOL） */
    @GetMapping("/textbooks")
    public ApiResponse<List<Map<String, Object>>> textbooks(@RequestParam(defaultValue = "MAIN") String type) {
        SysUser user = authService.currentUser();
        return ApiResponse.ok(accountAccessService.filterTextbooks(user, courseService.listTextbooks(type)));
    }

    /** 某一册下的单元与课时树 */
    @GetMapping("/grade/{gradeId}/outline")
    public ApiResponse<Map<String, Object>> gradeOutline(@PathVariable Long gradeId) {
        SysUser user = authService.currentUser();
        Long studentId = "STUDENT".equals(user.getRole()) ? user.getId() : null;
        Map<String, Object> outline = courseService.getGradeOutline(gradeId, studentId);
        accountAccessService.assertBook(user, outline);
        return ApiResponse.ok(outline);
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
        Map<String, Object> detail = courseService.getLessonDetail(lessonId, studentId);
        accountAccessService.assertBook(user,
                detail.get("gradeName") == null ? "" : String.valueOf(detail.get("gradeName")),
                detail.get("gradeDescription") == null ? "" : String.valueOf(detail.get("gradeDescription")),
                detail.get("textbookType") == null ? "" : String.valueOf(detail.get("textbookType")));
        return ApiResponse.ok(detail);
    }
}
