package com.itech.learnspace.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itech.learnspace.entity.CourseLesson;
import com.itech.learnspace.entity.CourseTask;
import com.itech.learnspace.entity.LessonScaffold;
import com.itech.learnspace.repository.CourseLessonRepository;
import com.itech.learnspace.repository.CourseTaskRepository;
import com.itech.learnspace.repository.LessonScaffoldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动时更新「六年级上册 · 第2课 抽象与建模」三个探究标题、课堂小测、学习评价，
 * 并写入本课专用 ABC 支架。
 */
@Component
@Order(22)
public class G6UpLesson2Seeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(G6UpLesson2Seeder.class);
    private static final String LESSON_CONTENT =
            "<p>本课学习<strong>抽象与建模</strong>：从鸡兔同笼出发，经历「问题描述 → 表格抽象 → 代入验证」，再迁移到百钱买百鸡。</p>"
                    + "<p>请从左侧依次完成<strong>探究一 情景与问题描述、探究二 表格抽象与验证、探究三 百钱买百鸡</strong>，完成后做<strong>课堂小测</strong>和<strong>学习评价</strong>。</p>";
    private static final String SCAFFOLD_MARKER = "鸡这一行已经示范";
    private static final String QUIZ_MARKER = "头数成立、脚数不成立";
    private static final String EVAL_MARKER = "把一组数代入两个方程做验证";

    private static final Map<Integer, String> TITLES = new LinkedHashMap<Integer, String>();
    static {
        TITLES.put(1, "情景与问题描述");
        TITLES.put(2, "表格抽象与验证");
        TITLES.put(3, "百钱买百鸡");
    }

    private final JdbcTemplate jdbcTemplate;
    private final CourseLessonRepository lessonRepository;
    private final CourseTaskRepository taskRepository;
    private final LessonScaffoldRepository scaffoldRepository;
    private final ObjectMapper objectMapper;

    public G6UpLesson2Seeder(JdbcTemplate jdbcTemplate,
                             CourseLessonRepository lessonRepository,
                             CourseTaskRepository taskRepository,
                             LessonScaffoldRepository scaffoldRepository,
                             ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.lessonRepository = lessonRepository;
        this.taskRepository = taskRepository;
        this.scaffoldRepository = scaffoldRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long lessonId = findLessonId();
        if (lessonId == null) {
            log.warn("未找到「六年级上册 / 第一单元 算法的实现 / 第2课 抽象与建模」，跳过更新");
            return;
        }
        CourseLesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson != null && (lesson.getContent() == null || !lesson.getContent().contains("百钱买百鸡"))) {
            lesson.setContent(LESSON_CONTENT);
            lessonRepository.save(lesson);
        }
        for (CourseTask task : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if (!"EXTERNAL".equals(task.getTaskType()) || task.getConfigJson() == null) {
                continue;
            }
            if (patchWorkspace(task)) {
                taskRepository.save(task);
                log.info("已更新六年级上第2课工作台（探究标题/小测/评价）：lessonId={}", lessonId);
            }
        }
        seedScaffold(lessonId);
    }

    private boolean patchWorkspace(CourseTask task) {
        try {
            JsonNode root = objectMapper.readTree(task.getConfigJson());
            if (!(root instanceof ObjectNode) || !root.has("activities")) {
                return false;
            }
            ObjectNode obj = (ObjectNode) root;
            JsonNode acts = obj.get("activities");
            if (!(acts instanceof ArrayNode)) {
                return false;
            }
            boolean changed = false;
            for (JsonNode act : acts) {
                if (!(act instanceof ObjectNode)) {
                    continue;
                }
                int index = act.path("index").asInt(0);
                String title = TITLES.get(index);
                if (title != null && !title.equals(act.path("title").asText())) {
                    ((ObjectNode) act).put("title", title);
                    ((ObjectNode) act).put("path", "/lessons/g6-up-lesson2/index.html");
                    changed = true;
                }
            }
            JsonNode quizNode = readJson("seed/g6-up-lesson2-quiz.json");
            String currentQuiz = obj.has("quiz") ? obj.get("quiz").toString() : "";
            if (quizNode != null && !currentQuiz.contains(QUIZ_MARKER)) {
                obj.set("quiz", quizNode);
                changed = true;
            }
            JsonNode workspace = readJson("seed/g6-up-lesson2-workspace-meta.json");
            String currentEval = obj.has("evaluationForm") ? obj.get("evaluationForm").toString() : "";
            if (workspace != null && !currentEval.contains(EVAL_MARKER)) {
                if (workspace.has("objectives")) {
                    obj.set("objectives", workspace.get("objectives"));
                }
                if (workspace.has("introMap")) {
                    obj.set("introMap", workspace.get("introMap"));
                }
                if (workspace.has("evaluationForm")) {
                    obj.set("evaluationForm", workspace.get("evaluationForm"));
                }
                changed = true;
            }
            String introText = obj.has("introMap") ? obj.get("introMap").toString() : "";
            if (workspace != null && workspace.has("introMap") && !introText.contains("课堂小测和学习评价")) {
                obj.set("introMap", workspace.get("introMap"));
                changed = true;
            }
            if (!changed) {
                return false;
            }
            task.setConfigJson(objectMapper.writeValueAsString(obj));
            task.setDescription("六年级上第2课学生工作台：情景与问题描述、表格抽象与验证、百钱买百鸡");
            return true;
        } catch (Exception e) {
            log.warn("更新六年级上第2课工作台失败: {}", e.getMessage());
            return false;
        }
    }

    private void seedScaffold(Long lessonId) {
        String itemsJson = readText("seed/g6-up-lesson2-scaffold.json");
        if (!StringUtils.hasText(itemsJson)) {
            return;
        }
        LessonScaffold row = scaffoldRepository.findByLessonId(lessonId).orElse(null);
        if (row != null && "published".equals(row.getStatus())
                && StringUtils.hasText(row.getItemsJson())
                && row.getItemsJson().contains(SCAFFOLD_MARKER)
                && row.getItemsJson().contains("用变量表示总头数")) {
            return;
        }
        boolean created = row == null;
        if (row == null) {
            row = new LessonScaffold();
            row.setLessonId(lessonId);
        }
        row.setItemsJson(itemsJson);
        row.setStatus("published");
        scaffoldRepository.save(row);
        log.info("{}六年级上第2课 ABC 支架并已发布：lessonId={}", created ? "已写入" : "已更新", lessonId);
    }

    private JsonNode readJson(String path) {
        String text = readText(path);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            log.warn("解析 {} 失败: {}", path, e.getMessage());
            return null;
        }
    }

    private String readText(String path) {
        try (java.io.InputStream in = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.warn("读取 {} 失败: {}", path, e.getMessage());
            return null;
        }
    }

    private Long findLessonId() {
        String sql = "SELECT cl.id FROM course_lesson cl "
                + "INNER JOIN course_unit cu ON cl.unit_id = cu.id "
                + "INNER JOIN course_grade cg ON cu.grade_id = cg.id "
                + "WHERE cg.name = ? AND cg.textbook_type = ? AND cu.name = ? AND cl.title = ? LIMIT 1";
        List<Long> ids = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong(1),
                "六年级上册", "MAIN", "第一单元 算法的实现", "第2课 抽象与建模");
        return ids.isEmpty() ? null : ids.get(0);
    }
}
