package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itech.learnspace.config.CozeChatProperties;
import com.itech.learnspace.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 将学生端 OpenAI 风格对话请求转换为扣子生产接口 /stream_run，并流式透传 SSE。
 * Token 仅在服务端注入，不暴露给前端。
 */
@Service
public class CozeChatProxyService {

    private final CozeChatProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CozeChatProxyService(CozeChatProperties properties) {
        this.properties = properties;
    }

    public void proxyStream(byte[] requestBody, HttpServletResponse response) throws IOException {
        if (!StringUtils.hasText(properties.getApiUrl())) {
            throw new BusinessException(503, "小派老师尚未配置上游地址。请在服务器设置 COZE_CHAT_API_URL 后重启 backend。");
        }
        if (!StringUtils.hasText(properties.getToken())) {
            throw new BusinessException(503, "小派老师尚未配置访问令牌。请在服务器设置 COZE_CHAT_TOKEN 后重启 backend。");
        }
        if (requestBody == null || requestBody.length == 0) {
            throw new BusinessException("请求体为空");
        }

        byte[] upstreamBody = toStreamRunBody(requestBody);
        String upstreamUrl = resolveUpstreamUrl(properties.getApiUrl().trim());

        HttpURLConnection conn = null;
        try {
            URL url = new URL(upstreamUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(properties.getConnectTimeoutMs());
            conn.setReadTimeout(properties.getReadTimeoutMs());
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "text/event-stream, application/json");
            conn.setRequestProperty("Authorization", "Bearer " + properties.getToken().trim());

            OutputStream os = conn.getOutputStream();
            os.write(upstreamBody);
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (in == null) {
                response.setStatus(code > 0 ? code : 502);
                response.setContentType("application/json; charset=UTF-8");
                response.getOutputStream().write(("{\"message\":\"上游无响应\"}").getBytes(StandardCharsets.UTF_8));
                return;
            }

            // 鉴权失败时给前端可读提示（避免只显示上游英文）
            if (code == 401 || code == 403) {
                readAll(in);
                in.close();
                response.setStatus(502);
                response.setContentType("application/json; charset=UTF-8");
                String tip = "{\"message\":\"扣子访问令牌无效。请在服务器 .env 设置正确的 COZE_CHAT_TOKEN，并执行 docker compose up -d --force-recreate backend\"}";
                response.getOutputStream().write(tip.getBytes(StandardCharsets.UTF_8));
                return;
            }

            String contentType = conn.getContentType();
            if (StringUtils.hasText(contentType)) {
                response.setContentType(contentType);
            } else {
                response.setContentType("text/event-stream; charset=UTF-8");
            }
            response.setStatus(code);
            response.setHeader("Cache-Control", "no-cache, no-transform");
            response.setHeader("X-Accel-Buffering", "no");

            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
                out.flush();
            }
            in.close();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 兼容旧配置：若仍指向 /v1/chat/completions，自动改写为同域 /stream_run。
     */
    static String resolveUpstreamUrl(String configured) {
        if (!StringUtils.hasText(configured)) {
            return configured;
        }
        String u = configured.trim();
        if (u.endsWith("/v1/chat/completions")) {
            return u.substring(0, u.length() - "/v1/chat/completions".length()) + "/stream_run";
        }
        if (u.endsWith("/")) {
            return u + "stream_run";
        }
        if (!u.endsWith("/stream_run")) {
            // 仅给了根域名时补全
            if (u.matches("https?://[^/]+")) {
                return u + "/stream_run";
            }
        }
        return u;
    }

    /**
     * 前端 OpenAI 风格 → 扣子 /stream_run query。
     */
    byte[] toStreamRunBody(byte[] requestBody) throws IOException {
        JsonNode root = objectMapper.readTree(requestBody);
        // 已是 stream_run 形态则原样转发
        if ("query".equals(root.path("type").asText()) && root.has("content")) {
            return requestBody;
        }

        String sessionId = root.path("session_id").asText(null);
        if (!StringUtils.hasText(sessionId)) {
            sessionId = "web_" + System.currentTimeMillis();
        }

        JsonNode lastUser = findLastUserMessage(root.path("messages"));
        if (lastUser == null) {
            throw new BusinessException("缺少用户消息");
        }

        ArrayNode prompt = objectMapper.createArrayNode();
        JsonNode content = lastUser.path("content");
        if (content.isTextual()) {
            String text = content.asText("").trim();
            if (StringUtils.hasText(text)) {
                prompt.add(textPart(text));
            }
        } else if (content.isArray()) {
            for (JsonNode part : content) {
                String type = part.path("type").asText("");
                if ("text".equals(type)) {
                    String text = part.path("text").asText("");
                    if (!StringUtils.hasText(text) && part.path("content").isObject()) {
                        text = part.path("content").path("text").asText("");
                    }
                    if (StringUtils.hasText(text)) {
                        prompt.add(textPart(text));
                    }
                } else if ("image_url".equals(type) || "upload_file".equals(type)) {
                    String imageUrl = part.path("image_url").path("url").asText("");
                    if (!StringUtils.hasText(imageUrl)) {
                        imageUrl = part.path("url").asText("");
                    }
                    if (!StringUtils.hasText(imageUrl)) {
                        imageUrl = part.path("content").path("upload_file").path("url").asText("");
                    }
                    if (!StringUtils.hasText(imageUrl)) {
                        continue;
                    }
                    if (imageUrl.startsWith("data:")) {
                        throw new BusinessException("图片须先上传为公网 URL，不能直接传 base64");
                    }
                    prompt.add(uploadFilePart(imageUrl, guessFileName(imageUrl, part)));
                }
            }
        }

        if (prompt.size() == 0) {
            throw new BusinessException("消息内容为空");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", "query");
        body.put("session_id", sessionId);
        ObjectNode contentNode = body.putObject("content");
        ObjectNode query = contentNode.putObject("query");
        query.set("prompt", prompt);
        return objectMapper.writeValueAsBytes(body);
    }

    private static JsonNode findLastUserMessage(JsonNode messages) {
        if (messages == null || !messages.isArray() || messages.size() == 0) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            JsonNode m = messages.get(i);
            if ("user".equals(m.path("role").asText())) {
                return m;
            }
        }
        return messages.get(messages.size() - 1);
    }

    private ObjectNode textPart(String text) {
        ObjectNode part = objectMapper.createObjectNode();
        part.put("type", "text");
        part.putObject("content").put("text", text);
        return part;
    }

    private ObjectNode uploadFilePart(String url, String fileName) {
        ObjectNode part = objectMapper.createObjectNode();
        part.put("type", "upload_file");
        ObjectNode upload = part.putObject("content").putObject("upload_file");
        upload.put("file_name", fileName);
        upload.put("file_path", "");
        upload.put("url", url);
        return part;
    }

    private static String guessFileName(String imageUrl, JsonNode part) {
        String name = part.path("file_name").asText("");
        if (!StringUtils.hasText(name)) {
            name = part.path("content").path("upload_file").path("file_name").asText("");
        }
        if (StringUtils.hasText(name)) {
            return name;
        }
        try {
            String path = new URL(imageUrl).getPath();
            int slash = path.lastIndexOf('/');
            String base = slash >= 0 ? path.substring(slash + 1) : path;
            if (StringUtils.hasText(base) && base.contains(".")) {
                return base;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "image.png";
    }

    private static byte[] readAll(InputStream in) throws IOException {
        byte[] buf = new byte[4096];
        int n;
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        while ((n = in.read(buf)) >= 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
