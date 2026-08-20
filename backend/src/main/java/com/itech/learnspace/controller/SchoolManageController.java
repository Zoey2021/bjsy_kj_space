package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.dto.ClassSaveRequest;
import com.itech.learnspace.dto.StudentSaveRequest;
import com.itech.learnspace.dto.TeacherSaveRequest;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.SchoolManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/manage")
public class SchoolManageController {

    private final SchoolManageService schoolManageService;
    private final AuthService authService;

    public SchoolManageController(SchoolManageService schoolManageService, AuthService authService) {
        this.schoolManageService = schoolManageService;
        this.authService = authService;
    }

    private SysUser checkTeacherOrAdmin() {
        SysUser user = authService.currentUser();
        if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅教师或管理员可操作");
        }
        return user;
    }

    // ----- 教师 -----

    @GetMapping("/teachers")
    public ApiResponse<List<Map<String, Object>>> teachers() {
        checkTeacherOrAdmin();
        return ApiResponse.ok(schoolManageService.listTeachers());
    }

    @PostMapping("/teachers")
    public ApiResponse<Map<String, Object>> createTeacher(@RequestBody TeacherSaveRequest request) {
        checkTeacherOrAdmin();
        return ApiResponse.ok("创建成功", schoolManageService.createTeacher(request));
    }

    @PutMapping("/teachers/{id}")
    public ApiResponse<Map<String, Object>> updateTeacher(@PathVariable Long id,
                                                          @RequestBody TeacherSaveRequest request) {
        checkTeacherOrAdmin();
        return ApiResponse.ok("已保存", schoolManageService.updateTeacher(id, request));
    }

    @DeleteMapping("/teachers/{id}")
    public ApiResponse<String> deleteTeacher(@PathVariable Long id) {
        checkTeacherOrAdmin();
        schoolManageService.deleteTeacher(id);
        return ApiResponse.ok("已删除", "ok");
    }

    @PutMapping("/teachers/{id}/reset-password")
    public ApiResponse<Map<String, Object>> resetTeacherPassword(@PathVariable Long id) {
        checkTeacherOrAdmin();
        return ApiResponse.ok("密码已重置", schoolManageService.resetTeacherPassword(id));
    }

    // ----- 班级 -----

    @GetMapping("/classes")
    public ApiResponse<List<Map<String, Object>>> classes() {
        checkTeacherOrAdmin();
        return ApiResponse.ok(schoolManageService.listClasses());
    }

    @PostMapping("/classes")
    public ApiResponse<Map<String, Object>> createClass(@RequestBody ClassSaveRequest request) {
        SysUser op = checkTeacherOrAdmin();
        return ApiResponse.ok("创建成功", schoolManageService.createClass(request, op));
    }

    @PutMapping("/classes/{id}")
    public ApiResponse<Map<String, Object>> updateClass(@PathVariable Long id,
                                                        @RequestBody ClassSaveRequest request) {
        checkTeacherOrAdmin();
        return ApiResponse.ok("已保存", schoolManageService.updateClass(id, request));
    }

    @DeleteMapping("/classes/{id}")
    public ApiResponse<String> deleteClass(@PathVariable Long id) {
        checkTeacherOrAdmin();
        schoolManageService.deleteClass(id);
        return ApiResponse.ok("已删除", "ok");
    }

    // ----- 学生 -----

    @GetMapping("/students")
    public ApiResponse<List<Map<String, Object>>> students(
            @RequestParam(required = false) Long classId) {
        checkTeacherOrAdmin();
        return ApiResponse.ok(schoolManageService.listStudents(classId));
    }

    @PostMapping("/students")
    public ApiResponse<Map<String, Object>> createStudent(@RequestBody StudentSaveRequest request) {
        checkTeacherOrAdmin();
        return ApiResponse.ok("创建成功", schoolManageService.createStudent(request));
    }

    @PutMapping("/students/{id}")
    public ApiResponse<Map<String, Object>> updateStudent(@PathVariable Long id,
                                                          @RequestBody StudentSaveRequest request) {
        checkTeacherOrAdmin();
        return ApiResponse.ok("已保存", schoolManageService.updateStudent(id, request));
    }

    @DeleteMapping("/students/{id}")
    public ApiResponse<String> deleteStudent(@PathVariable Long id) {
        checkTeacherOrAdmin();
        schoolManageService.deleteStudent(id);
        return ApiResponse.ok("已删除", "ok");
    }

    @PutMapping("/students/{id}/reset-password")
    public ApiResponse<Map<String, Object>> resetStudentPassword(@PathVariable Long id) {
        checkTeacherOrAdmin();
        return ApiResponse.ok("密码已重置", schoolManageService.resetStudentPassword(id));
    }
}
