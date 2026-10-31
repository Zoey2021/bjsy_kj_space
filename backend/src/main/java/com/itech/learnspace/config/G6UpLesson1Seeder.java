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
 * 启动时更新「六年级上册 · 第1课 算法与问题解决」三个环节标题，
 * 并写入本课专用 ABC 支架（不走百炼）。
 */
@Component
@Order(21)
public class G6UpLesson1Seeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(G6UpLesson1Seeder.class);
    private static final String LESSON_CONTENT =
            "<p>本课学习<strong>算法与问题解决</strong>：了解人工实施与计算机实现两种途径，经历「问题分析 → 抽象与建模 → 设计算法 → 验证与优化」过程。</p>"
                    + "<p>请从左侧依次完成<strong>环节1 问题分析与建模、环节2 算法设计、环节3 算法验证与优化</strong>，然后先做<strong>课堂小测</strong>，再做<strong>学习评价</strong>。</p>";
    private static final String SCAFFOLD_MARKER = "步骤1已经示范";
    private static final String QUIZ_MARKER = "课本程序用 if tp";

    private static final Map<Integer, String> TITLES = new LinkedHashMap<Integer, String>();
    static {
        TITLES.put(1, "问题分析与建模");
        TITLES.put(2, "算法设计");
        TITLES.put(3, "算法验证与优化");
    }

    private final JdbcTemplate jdbcTemplate;
    private final CourseLessonRepository lessonRepository;
    private final CourseTaskRepository taskRepository;
    private final LessonScaffoldRepository scaffoldRepository;
    private final ObjectMapper objectMapper;

    public G6UpLesson1Seeder(JdbcTemplate jdbcTemplate,
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
            log.warn("未找到「六年级上册 / 第一单元 算法的实现 / 第1课 算法与问题解决」，跳过环节标题更新");
            return;
        }
        CourseLesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson != null) {
            String content = lesson.getContent();
            if (content == null || !content.contains("算法设计")
                    || (content.contains("完成后做<strong>课堂小测</strong>") && !content.contains("学习评价"))) {
                lesson.setContent(LESSON_CONTENT);
                lessonRepository.save(lesson);
            }
        }
        for (CourseTask task : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if (!"EXTERNAL".equals(task.getTaskType()) || task.getConfigJson() == null) {
                continue;
            }
            if (patchWorkspace(task)) {
                taskRepository.save(task);
                log.info("已更新六年级上第1课工作台（环节标题/课堂小测）：lessonId={}", lessonId);
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
                    changed = true;
                }
            }
            JsonNode quizNode = readQuizJson();
            if (quizNode != null) {
                String currentQuiz = obj.has("quiz") ? obj.get("quiz").toString() : "";
                if (!currentQuiz.contains(QUIZ_MARKER)) {
                    obj.set("quiz", quizNode);
                    changed = true;
                }
            }
            if (!changed) {
                return false;
            }
            obj.put("objectives",
                    "我能说出人工实施与计算机实现两种途径，能从投票问题中提取关键信息并用变量建模，能对照流程图理解算法步骤，并验证、优化、拓展投票统计程序。");
            task.setConfigJson(objectMapper.writeValueAsString(obj));
            task.setDescription("六年级上第1课学生工作台：问题分析与建模、算法设计、算法验证与优化");
            return true;
        } catch (Exception e) {
            log.warn("更新六年级上第1课工作台失败: {}", e.getMessage());
            return false;
        }
    }

    private void seedScaffold(Long lessonId) {
        String itemsJson = readScaffoldJson();
        if (!StringUtils.hasText(itemsJson)) {
            return;
        }
        LessonScaffold row = scaffoldRepository.findByLessonId(lessonId).orElse(null);
        boolean ours = row != null && StringUtils.hasText(row.getItemsJson())
                && row.getItemsJson().contains(SCAFFOLD_MARKER);
        boolean titlesFresh = row != null && StringUtils.hasText(row.getItemsJson())
                && row.getItemsJson().contains("算法设计")
                && row.getItemsJson().contains("会走到 else");
        if (ours && titlesFresh && "published".equals(row.getStatus())) {
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
        log.info("{}六年级上第1课 ABC 支架并已发布：lessonId={}",
                created ? "已写入" : (ours ? "已发布已有的" : "已替换百炼草稿为"), lessonId);
    }

    private JsonNode readQuizJson() {
        try (java.io.InputStream in = new ClassPathResource("seed/g6-up-lesson1-quiz.json").getInputStream()) {
            return objectMapper.readTree(StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim());
        } catch (Exception e) {
            log.warn("读取六年级上第1课课堂小测失败: {}", e.getMessage());
            return null;
        }
    }

    private String readScaffoldJson() {
        try (java.io.InputStream in = new ClassPathResource("seed/g6-up-lesson1-scaffold.json").getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.warn("读取六年级上第1课支架失败: {}", e.getMessage());
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
                "六年级上册", "MAIN", "第一单元 算法的实现", "第1课 算法与问题解决");
        return ids.isEmpty() ? null : ids.get(0);
    }
}
