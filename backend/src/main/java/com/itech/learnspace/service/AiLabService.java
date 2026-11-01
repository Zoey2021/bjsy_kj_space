package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.config.ActivityProperties;
import com.itech.learnspace.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 人工智能实验室：四馆内容配置（教师配置，学生端展示）。
 * 配置存 MySQL；HTML 写入 lesson-generated/ai-lab/，经 /lessons/generated/ai-lab/ 访问。
 */
@Service
public class AiLabService {

    private static final String[] HALL_IDS = new String[] { "experience", "train", "create", "agent" };

    private final JdbcTemplate jdbcTemplate;
    private final ActivityProperties activityProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiLabService(JdbcTemplate jdbcTemplate, ActivityProperties activityProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.activityProperties = activityProperties;
    }

    @PostConstruct
    public void ensureTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS learn_ai_lab_config ("
                + "id INT NOT NULL,"
                + "config_json TEXT NOT NULL,"
                + "updated_at DATETIME NULL,"
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public Map<String, Object> getConfig() {
        return normalizeConfig(readRaw());
    }

    /** 学生端可见配置（与教师端结构一致） */
    public Map<String, Object> getPublicConfig() {
        return getConfig();
    }

    public Map<String, Object> saveConfig(Map<String, Object> body) {
        Map<String, Object> clean = normalizeConfig(body);
        try {
            String json = objectMapper.writeValueAsString(clean);
            jdbcTemplate.update(
                    "INSERT INTO learn_ai_lab_config (id, config_json, updated_at) VALUES (1, ?, NOW()) "
                            + "ON DUPLICATE KEY UPDATE config_json = VALUES(config_json), updated_at = NOW()",
                    json);
        } catch (Exception e) {
            throw new BusinessException("AI 实验室配置保存失败");
        }
        return clean;
    }

    /**
     * 小派老师聊天：上传图片，返回可供扣子访问的公开 URL。
     */
    public Map<String, Object> uploadChatImage(MultipartFile file, String publicOrigin) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }
        String original = file.getOriginalFilename();
        String lower = original == null ? "" : original.toLowerCase(Locale.ROOT);
        String ext = ".jpg";
        if (lower.endsWith(".png")) {
            ext = ".png";
        } else if (lower.endsWith(".webp")) {
            ext = ".webp";
        } else if (lower.endsWith(".gif")) {
            ext = ".gif";
        } else if (lower.endsWith(".jpeg") || lower.endsWith(".jpg")) {
            ext = ".jpg";
        } else {
            String ct = file.getContentType();
            if (ct != null && ct.contains("png")) {
                ext = ".png";
            } else if (ct != null && ct.contains("webp")) {
                ext = ".webp";
            } else if (ct == null || !ct.startsWith("image/")) {
                throw new BusinessException("仅支持图片文件");
            }
        }
        if (file.getSize() > 8L * 1024 * 1024) {
            throw new BusinessException("图片请小于 8MB");
        }

        String fileName = "chat-" + System.currentTimeMillis() + ext;
        Path dir = Paths.get(activityProperties.getHtmlOutputDir(), "ai-lab", "uploads");
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("保存图片失败：" + e.getMessage());
        }

        String path = "/lessons/generated/ai-lab/uploads/" + fileName;
        String origin = publicOrigin == null ? "" : publicOrigin.trim();
        while (origin.endsWith("/")) {
            origin = origin.substring(0, origin.length() - 1);
        }
        String url = StringUtils.hasText(origin) ? (origin + path) : path;

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("path", path);
        result.put("url", url);
        result.put("fileName", fileName);
        return result;
    }

    /**
     * 上传某一馆的 HTML，返回可公开访问路径，并自动追加一条内容项。
     */
    public Map<String, Object> uploadHallHtml(String hallId, MultipartFile file, String title) {
        String hall = normalizeHallId(hallId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择 HTML 文件");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase(Locale.ROOT).endsWith(".html")) {
            throw new BusinessException("仅支持 .html 文件");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("读取文件失败：" + e.getMessage());
        }
        if (bytes.length == 0) {
            throw new BusinessException("HTML 内容为空");
        }

        String safeName = "item-" + System.currentTimeMillis() + ".html";
        Path dir = Paths.get(activityProperties.getHtmlOutputDir(), "ai-lab", hall);
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(safeName), bytes);
        } catch (IOException e) {
            throw new BusinessException("保存失败：" + e.getMessage());
        }

        String publicPath = "/lessons/generated/ai-lab/" + hall + "/" + safeName;
        String itemTitle = StringUtils.hasText(title)
                ? title.trim()
                : original.replaceAll("(?i)\\.html$", "");

        Map<String, Object> cfg = getConfig();
        @SuppressWarnings("unchecked")
        Map<String, Object> halls = (Map<String, Object>) cfg.get("halls");
        @SuppressWarnings("unchecked")
        Map<String, Object> hallCfg = (Map<String, Object>) halls.get(hall);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) hallCfg.get("items");
        if (items == null) {
            items = new ArrayList<Map<String, Object>>();
            hallCfg.put("items", items);
        }
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", "html-" + System.currentTimeMillis());
        item.put("title", itemTitle);
        item.put("desc", "教师上传的 HTML 应用");
        item.put("type", "html");
        item.put("url", publicPath);
        item.put("enabled", Boolean.TRUE);
        items.add(item);
        saveConfig(cfg);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("hallId", hall);
        result.put("path", publicPath);
        result.put("item", item);
        result.put("config", cfg);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readRaw() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT config_json FROM learn_ai_lab_config WHERE id = 1 LIMIT 1");
            if (rows == null || rows.isEmpty() || rows.get(0).get("config_json") == null) {
                return defaultConfig();
            }
            String json = String.valueOf(rows.get(0).get("config_json"));
            if (!StringUtils.hasText(json)) {
                return defaultConfig();
            }
            Map<String, Object> cfg = objectMapper.readValue(json, Map.class);
            return cfg == null ? defaultConfig() : cfg;
        } catch (Exception e) {
            return defaultConfig();
        }
    }

    private Map<String, Object> defaultConfig() {
        Map<String, Object> cfg = new LinkedHashMap<String, Object>();
        Map<String, Object> halls = new LinkedHashMap<String, Object>();
        halls.put("experience", emptyHall("体验馆", "玩现成的 AI 小应用"));
        halls.put("train", emptyHall("训练馆", "上传数据、标注并训练"));
        halls.put("create", emptyHall("创造营", "用 AI 生成文字和图片"));
        Map<String, Object> agent = emptyHall("小助手", "和学习小助手对话");
        agent.put("embedUrl", "");
        agent.put("botId", "");
        agent.put("token", "");
        agent.put("assistants", defaultAssistants());
        halls.put("agent", agent);
        cfg.put("halls", halls);
        cfg.put("updatedHint", "教师可在「AI实验室」控制各馆开放，并上传应用链接或 HTML");
        return cfg;
    }

    private static Map<String, Object> emptyHall(String name, String desc) {
        Map<String, Object> hall = new LinkedHashMap<String, Object>();
        hall.put("name", name);
        hall.put("desc", desc);
        // 默认对学生开放；教师可在后台关闭
        hall.put("enabled", Boolean.TRUE);
        hall.put("items", new ArrayList<Map<String, Object>>());
        return hall;
    }

    private static List<Map<String, Object>> defaultAssistants() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> suanfa = new LinkedHashMap<String, Object>();
        suanfa.put("id", "suanfa");
        suanfa.put("enabled", Boolean.TRUE);
        list.add(suanfa);
        return list;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeConfig(Map<String, Object> body) {
        Map<String, Object> base = defaultConfig();
        if (body == null) {
            return base;
        }
        Object hallsObj = body.get("halls");
        if (!(hallsObj instanceof Map)) {
            return base;
        }
        Map<String, Object> inHalls = (Map<String, Object>) hallsObj;
        Map<String, Object> outHalls = (Map<String, Object>) base.get("halls");
        for (String hallId : HALL_IDS) {
            Object raw = inHalls.get(hallId);
            if (!(raw instanceof Map)) {
                continue;
            }
            Map<String, Object> src = (Map<String, Object>) raw;
            Map<String, Object> dst = (Map<String, Object>) outHalls.get(hallId);
            if (src.get("name") != null && StringUtils.hasText(String.valueOf(src.get("name")))) {
                dst.put("name", String.valueOf(src.get("name")).trim());
            }
            if (src.get("desc") != null) {
                dst.put("desc", String.valueOf(src.get("desc")).trim());
            }
            if (src.containsKey("enabled")) {
                dst.put("enabled", Boolean.parseBoolean(String.valueOf(src.get("enabled"))));
            }
            dst.put("items", normalizeItems(src.get("items")));
            if ("agent".equals(hallId)) {
                dst.put("embedUrl", str(src.get("embedUrl")));
                dst.put("botId", str(src.get("botId")));
                dst.put("token", str(src.get("token")));
                dst.put("assistants", normalizeAssistants(src.get("assistants")));
            }
        }
        return base;
    }

    /**
     * 预置智能体开关（如算法小助手）。未知 id 忽略；缺省时保持默认开启。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeAssistants(Object raw) {
        Map<String, Boolean> enabledById = new LinkedHashMap<String, Boolean>();
        for (Map<String, Object> def : defaultAssistants()) {
            enabledById.put(String.valueOf(def.get("id")), Boolean.TRUE);
        }
        if (raw instanceof List) {
            for (Object row : (List<?>) raw) {
                if (!(row instanceof Map)) {
                    continue;
                }
                Map<String, Object> src = (Map<String, Object>) row;
                String id = str(src.get("id"));
                if (!enabledById.containsKey(id)) {
                    continue;
                }
                Object en = src.get("enabled");
                enabledById.put(id, en == null || Boolean.parseBoolean(String.valueOf(en)));
            }
        }
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Boolean> e : enabledById.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", e.getKey());
            row.put("enabled", e.getValue());
            out.add(row);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeItems(Object raw) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (!(raw instanceof List)) {
            return out;
        }
        for (Object row : (List<?>) raw) {
            if (!(row instanceof Map)) {
                continue;
            }
            Map<String, Object> src = (Map<String, Object>) row;
            String title = str(src.get("title"));
            String url = str(src.get("url"));
            if (!StringUtils.hasText(title) && !StringUtils.hasText(url)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            String id = str(src.get("id"));
            item.put("id", StringUtils.hasText(id) ? id : ("item-" + UUID.randomUUID().toString().substring(0, 8)));
            item.put("title", StringUtils.hasText(title) ? title : "未命名应用");
            item.put("desc", str(src.get("desc")));
            String type = str(src.get("type")).toLowerCase(Locale.ROOT);
            if (!"html".equals(type) && !"embed".equals(type) && !"link".equals(type)) {
                type = url.contains("/lessons/generated/") ? "html" : "link";
            }
            item.put("type", type);
            item.put("url", url);
            Object en = src.get("enabled");
            item.put("enabled", en == null || Boolean.parseBoolean(String.valueOf(en)));
            out.add(item);
        }
        return out;
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String normalizeHallId(String hallId) {
        if (!StringUtils.hasText(hallId)) {
            throw new BusinessException("馆别无效");
        }
        String id = hallId.trim().toLowerCase(Locale.ROOT);
        for (String h : HALL_IDS) {
            if (h.equals(id)) {
                return h;
            }
        }
        throw new BusinessException("馆别无效");
    }
}
