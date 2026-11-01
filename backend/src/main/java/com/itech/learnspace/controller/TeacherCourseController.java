package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.dto.LessonSaveRequest;
import com.itech.learnspace.dto.GenerateActivitiesRequest;
import com.itech.learnspace.dto.PublishActivityRequest;
import com.itech.learnspace.dto.ResourceSaveRequest;
import com.itech.learnspace.entity.CourseLesson;
import com.itech.learnspace.entity.CourseResource;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.TeacherCourseService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/course")
public class TeacherCourseController {

    private final TeacherCourseService teacherCourseService;
    private final AuthService authService;
    private final com.itech.learnspace.service.AccountAccessService accountAccessService;

    public TeacherCourseController(TeacherCourseService teacherCourseService, AuthService authService,
                                   com.itech.learnspace.service.AccountAccessService accountAccessService) {
        this.teacherCourseService = teacherCourseService;
        this.authService = authService;
        this.accountAccessService = accountAccessService;
    }

    private void checkTeacher() {
        SysUser user = authService.currentUser();
        if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅教师可编辑课程");
        }
    }

    @GetMapping("/textbooks")
    public ApiResponse<List<Map<String, Object>>> textbooks(@RequestParam(defaultValue = "MAIN") String type) {
        checkTeacher();
        SysUser user = authService.currentUser();
        return ApiResponse.ok(accountAccessService.filterTextbooks(user, teacherCourseService.listTextbooks(type)));
    }

    @GetMapping("/grade/{gradeId}/outline")
    public ApiResponse<Map<String, Object>> outline(@PathVariable Long gradeId) {
        checkTeacher();
        SysUser user = authService.currentUser();
        Map<String, Object> outline = teacherCourseService.getGradeOutline(gradeId);
        accountAccessService.assertBook(user, outline);
        return ApiResponse.ok(outline);
    }

    @GetMapping("/lesson/{lessonId}")
    public ApiResponse<Map<String, Object>> lessonDetail(@PathVariable Long lessonId) {
        checkTeacher();
        SysUser user = authService.currentUser();
        Map<String, Object> data = teacherCourseService.getLessonEditorData(lessonId);
        Object grade = data.get("grade");
        if (grade instanceof com.itech.learnspace.entity.CourseGrade) {
            com.itech.learnspace.entity.CourseGrade book = (com.itech.learnspace.entity.CourseGrade) grade;
            accountAccessService.assertBook(user, book.getName(), book.getDescription(), book.getTextbookType());
        }
        return ApiResponse.ok(data);
    }

    @PostMapping("/lesson")
    public ApiResponse<CourseLesson> createLesson(@RequestBody LessonSaveRequest req) {
        checkTeacher();
        return ApiResponse.ok(teacherCourseService.createLesson(req));
    }

    @PutMapping("/lesson/{lessonId}")
    public ApiResponse<CourseLesson> updateLesson(@PathVariable Long lessonId, @RequestBody LessonSaveRequest req) {
        checkTeacher();
        return ApiResponse.ok(teacherCourseService.updateLesson(lessonId, req));
    }

    @PostMapping("/resource")
    public ApiResponse<CourseResource> createResource(@RequestBody ResourceSaveRequest req) {
        checkTeacher();
        return ApiResponse.ok(teacherCourseService.saveResource(req));
    }

    @PutMapping("/resource/{id}")
    public ApiResponse<CourseResource> updateResource(@PathVariable Long id, @RequestBody ResourceSaveRequest req) {
        checkTeacher();
        return ApiResponse.ok(teacherCourseService.updateResource(id, req));
    }

    @DeleteMapping("/resource/{id}")
    public ApiResponse<Void> deleteResource(@PathVariable Long id) {
        checkTeacher();
        teacherCourseService.deleteResource(id);
        return ApiResponse.ok(null);
    }

    /** 某班在某课时的提交与正确率统计 */
    @GetMapping("/lesson/{lessonId}/stats")
    public ApiResponse<Map<String, Object>> lessonStats(
            @PathVariable Long lessonId,
            @RequestParam Long classId) {
        checkTeacher();
        return ApiResponse.ok(teacherCourseService.getLessonClassStats(lessonId, classId));
    }

    /** 按勾选活动生成互动页草稿 */
    @PostMapping("/lesson/{lessonId}/generate-activities")
    public ApiResponse<Map<String, Object>> generateActivities(
            @PathVariable Long lessonId,
            @RequestBody GenerateActivitiesRequest req) {
        checkTeacher();
        SysUser user = authService.currentUser();
        return ApiResponse.ok(teacherCourseService.generateActivitySlots(lessonId, req, user.getId()));
    }

    /** 确认后同步到班级学生端 */
    @PostMapping("/lesson/{lessonId}/publish-activity")
    public ApiResponse<Map<String, Object>> publishActivity(
            @PathVariable Long lessonId,
            @RequestBody PublishActivityRequest req) {
        checkTeacher();
        SysUser user = authService.currentUser();
        return ApiResponse.ok(teacherCourseService.publishActivity(lessonId, req, user.getId()));
    }

    /** 上传飞象老师 HTML 到活动槽 */
    @PostMapping("/lesson/{lessonId}/upload-feixiang")
    public ApiResponse<Map<String, String>> uploadFeixiang(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "slotIndex", defaultValue = "1") int slotIndex) {
        checkTeacher();
        return ApiResponse.ok(teacherCourseService.uploadFeixiangHtml(lessonId, file, slotIndex));
    }

    /** 课时配置：读取当前已发布探究 */
    @GetMapping("/lesson/{lessonId}/html-config")
    public ApiResponse<Map<String, Object>> lessonHtmlConfig(@PathVariable Long lessonId) {
        checkTeacher();
        return ApiResponse.ok(teacherCourseService.getLessonHtmlConfig(lessonId));
    }

    /** 课时配置：修改某一探究标题 */
    @PutMapping("/lesson/{lessonId}/activity/{seq}/title")
    public ApiResponse<Map<String, Object>> updateActivityTitle(
            @PathVariable Long lessonId,
            @PathVariable int seq,
            @RequestBody Map<String, Object> body) {
        checkTeacher();
        String title = body == null || body.get("title") == null ? "" : String.valueOf(body.get("title"));
        return ApiResponse.ok("标题已保存",
                teacherCourseService.updateLessonActivityTitle(lessonId, seq, title));
    }

    /** 课时配置：保存学习目标等元信息 */
    @PutMapping("/lesson/{lessonId}/html-meta")
    public ApiResponse<Map<String, Object>> updateHtmlMeta(
            @PathVariable Long lessonId,
            @RequestBody(required = false) Map<String, Object> body) {
        checkTeacher();
        return ApiResponse.ok("已保存",
                teacherCourseService.updateLessonHtmlMeta(lessonId, body));
    }

    /** 课时配置：单独替换某一探究 HTML */
    @PostMapping("/lesson/{lessonId}/activity/{seq}/replace-html")
    public ApiResponse<Map<String, Object>> replaceActivityHtml(
            @PathVariable Long lessonId,
            @PathVariable int seq,
            @RequestParam("file") MultipartFile file) {
        checkTeacher();
        return ApiResponse.ok("该探究已更新",
                teacherCourseService.replaceLessonActivityHtml(lessonId, seq, file));
    }

    /** 上传整课 HTML 并拆成 2～3 个探究草稿 */
    @PostMapping("/lesson/{lessonId}/upload-split-html")
    public ApiResponse<Map<String, Object>> uploadSplitHtml(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file) {
        checkTeacher();
        return ApiResponse.ok(teacherCourseService.uploadAndSplitHtml(lessonId, file));
    }

    /** 确认预览后发布到该课时（学生课程地图可见） */
    @PostMapping("/lesson/{lessonId}/publish-html-pack")
    public ApiResponse<Map<String, Object>> publishHtmlPack(
            @PathVariable Long lessonId,
            @RequestBody(required = false) Map<String, Object> body) {
        checkTeacher();
        SysUser user = authService.currentUser();
        return ApiResponse.ok("已发布到学生课程地图",
                teacherCourseService.publishHtmlPack(lessonId, body, user.getId()));
    }
}
