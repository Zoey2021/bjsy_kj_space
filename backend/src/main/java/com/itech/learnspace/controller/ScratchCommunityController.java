package com.itech.learnspace.controller;

import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.ScratchCommunityService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scratch")
public class ScratchCommunityController {

    private final ScratchCommunityService scratchCommunityService;
    private final AuthService authService;

    public ScratchCommunityController(ScratchCommunityService scratchCommunityService, AuthService authService) {
        this.scratchCommunityService = scratchCommunityService;
        this.authService = authService;
    }

    @GetMapping("/projects")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(value = "sort", defaultValue = "hot") String sort) {
        return ApiResponse.ok(scratchCommunityService.list(sort, authService.currentUser()));
    }

    @GetMapping("/mine")
    public ApiResponse<List<Map<String, Object>>> mine() {
        return ApiResponse.ok(scratchCommunityService.mine(authService.currentUser()));
    }

    @GetMapping("/projects/{id}")
    public ApiResponse<Map<String, Object>> detail(
            @PathVariable("id") long id,
            HttpServletRequest request) {
        return ApiResponse.ok(scratchCommunityService.detail(id, authService.currentUser(), publicOrigin(request)));
    }

    /**
     * 已发布作品的 .sb3：公开可读，供本站播放器 fetch（不带登录头也能下）。
     */
    @GetMapping("/projects/{id}/sb3")
    public ResponseEntity<Resource> downloadPublishedSb3(@PathVariable("id") long id) {
        Path file = scratchCommunityService.resolvePublishedSb3File(id);
        FileSystemResource body = new FileSystemResource(file.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"project-" + id + ".sb3\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header("Access-Control-Allow-Origin", "*")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.toFile().length())
                .body(body);
    }

    /** 教师审核时试玩任意状态作品 */
    @GetMapping("/teacher/projects/{id}/sb3")
    public ResponseEntity<Resource> downloadTeacherSb3(@PathVariable("id") long id) {
        requireTeacher();
        Path file = scratchCommunityService.resolveAnySb3File(id);
        FileSystemResource body = new FileSystemResource(file.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"project-" + id + ".sb3\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.toFile().length())
                .body(body);
    }

    @PostMapping("/projects/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable("id") long id) {
        return ApiResponse.ok(scratchCommunityService.toggleLike(id, authService.currentUser()));
    }

    @PostMapping("/projects")
    public ApiResponse<Map<String, Object>> submit(
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("已提交审核", scratchCommunityService.submit(authService.currentUser(), title, file));
    }

    /** 教师直接上传并发布到编程作品社区 */
    @PostMapping("/teacher/publish")
    public ApiResponse<Map<String, Object>> teacherPublish(
            @RequestParam("title") String title,
            @RequestParam("file") MultipartFile file) {
        requireTeacher();
        return ApiResponse.ok("已发布", scratchCommunityService.teacherPublish(authService.currentUser(), title, file));
    }

    @GetMapping("/teacher/projects")
    public ApiResponse<List<Map<String, Object>>> teacherList(
            @RequestParam(value = "status", required = false) String status) {
        requireTeacher();
        return ApiResponse.ok(scratchCommunityService.teacherList(status));
    }

    @GetMapping("/teacher/counts")
    public ApiResponse<Map<String, Object>> teacherCounts() {
        requireTeacher();
        return ApiResponse.ok(scratchCommunityService.teacherCounts());
    }

    @PostMapping("/teacher/projects/{id}/review")
    public ApiResponse<Map<String, Object>> review(
            @PathVariable("id") long id,
            @RequestBody Map<String, String> body) {
        requireTeacher();
        String action = body == null ? null : body.get("action");
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok(scratchCommunityService.review(id, authService.currentUser(), action, reason));
    }

    private void requireTeacher() {
        SysUser user = authService.currentUser();
        if (user == null || !("TEACHER".equals(user.getRole()) || "ADMIN".equals(user.getRole()))) {
            throw new BusinessException(403, "仅教师可操作");
        }
    }

    private String publicOrigin(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isEmpty()) {
            host = request.getHeader("Host");
        }
        if (host == null || host.isEmpty()) {
            return "";
        }
        if (proto == null || proto.isEmpty()) {
            proto = request.getScheme();
        }
        return proto + "://" + host.split(",")[0].trim();
    }
}
