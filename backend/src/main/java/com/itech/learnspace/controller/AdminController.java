package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.dto.UserCreateRequest;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AdminService;
import com.itech.learnspace.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;

    public AdminController(AdminService adminService, AuthService authService) {
        this.adminService = adminService;
        this.authService = authService;
    }

    private void checkAdmin() {
        if (!"ADMIN".equals(authService.currentUser().getRole())) {
            throw new BusinessException(403, "仅管理员可操作");
        }
    }

    @GetMapping("/users")
    public ApiResponse<List<SysUser>> users() {
        checkAdmin();
        return ApiResponse.ok(adminService.listUsers());
    }

    @PostMapping("/users")
    public ApiResponse<SysUser> createUser(@RequestBody UserCreateRequest request) {
        checkAdmin();
        return ApiResponse.ok(adminService.createUser(request));
    }

    @PutMapping("/users/{id}/toggle")
    public ApiResponse<SysUser> toggle(@PathVariable Long id) {
        checkAdmin();
        return ApiResponse.ok(adminService.toggleStatus(id));
    }
}
