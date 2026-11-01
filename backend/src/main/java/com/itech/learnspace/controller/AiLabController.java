package com.itech.learnspace.controller;

import com.itech.learnspace.config.CozeChatProperties;
import com.itech.learnspace.dto.ApiResponse;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.service.AiLabService;
import com.itech.learnspace.service.AuthService;
import com.itech.learnspace.service.CozeChatProxyService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@RestController
public class AiLabController {

    private final AiLabService aiLabService;
    private final AuthService authService;
    private final CozeChatProxyService cozeChatProxyService;
    private final CozeChatProperties cozeChatProperties;

    public AiLabController(AiLabService aiLabService, AuthService authService,
                           CozeChatProxyService cozeChatProxyService,
                           CozeChatProperties cozeChatProperties) {
        this.aiLabService = aiLabService;
        this.authService = authService;
        this.cozeChatProxyService = cozeChatProxyService;
        this.cozeChatProperties = cozeChatProperties;
    }

    private void checkTeacher() {
        SysUser user = authService.currentUser();
        if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "仅教师可配置 AI 实验室");
        }
    }

    /** 学生 / 教师均可读取当前四馆内容 */
    @GetMapping("/api/ai-lab/config")
    public ApiResponse<Map<String, Object>> publicConfig() {
        return ApiResponse.ok(aiLabService.getPublicConfig());
    }

    @GetMapping("/api/teacher/ai-lab/config")
    public ApiResponse<Map<String, Object>> teacherConfig() {
        checkTeacher();
        return ApiResponse.ok(aiLabService.getConfig());
    }

    @PutMapping("/api/teacher/ai-lab/config")
    public ApiResponse<Map<String, Object>> saveConfig(@RequestBody(required = false) Map<String, Object> body) {
        checkTeacher();
        return ApiResponse.ok("已保存", aiLabService.saveConfig(body));
    }

    @PostMapping("/api/teacher/ai-lab/{hallId}/upload-html")
    public ApiResponse<Map<String, Object>> uploadHtml(
            @PathVariable String hallId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        checkTeacher();
        return ApiResponse.ok("已上传并加入本馆",
                aiLabService.uploadHallHtml(hallId, file, title));
    }

    /**
     * 小派老师：上传聊天图片，返回公开 URL（供扣子 upload_file 拉取）。
     */
    @PostMapping("/api/ai-lab/upload")
    public ApiResponse<Map<String, Object>> uploadChatImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        authService.currentUser();
        return ApiResponse.ok(aiLabService.uploadChatImage(file, resolvePublicOrigin(request)));
    }

    /**
     * 小派老师对话代理（流式）。
     * 学生/教师登录后可调用；扣子 Token 由服务端注入。
     */
    @PostMapping("/api/ai-lab/coze/chat")
    public void cozeChat(HttpServletRequest request, HttpServletResponse response) throws Exception {
        authService.currentUser();
        InputStream in = request.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            bos.write(buf, 0, n);
        }
        cozeChatProxyService.proxyStream(bos.toByteArray(), response);
    }

    /**
     * 优先用配置的公网根（https://域名），避免反代后变成 http:// 导致扣子拉不到图。
     */
    private String resolvePublicOrigin(HttpServletRequest request) {
        String configured = cozeChatProperties.getPublicOrigin();
        if (StringUtils.hasText(configured)) {
            String origin = configured.trim();
            while (origin.endsWith("/")) {
                origin = origin.substring(0, origin.length() - 1);
            }
            return origin;
        }
        String proto = request.getHeader("X-Forwarded-Proto");
        if (!StringUtils.hasText(proto)) {
            proto = request.getScheme();
        }
        // 生产站点强制 https，避免宝塔→容器是 http 时把图片 URL 写成 http
        if ("http".equalsIgnoreCase(proto) && String.valueOf(request.getHeader("Host")).contains("bjsysmartedu.cn")) {
            proto = "https";
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (!StringUtils.hasText(host)) {
            host = request.getHeader("Host");
        }
        if (!StringUtils.hasText(host)) {
            return "https://bjsysmartedu.cn";
        }
        int comma = host.indexOf(',');
        if (comma > 0) {
            host = host.substring(0, comma).trim();
        }
        return proto + "://" + host.trim();
    }
}
