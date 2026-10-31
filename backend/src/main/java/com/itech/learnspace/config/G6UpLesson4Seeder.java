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
 * 启动时把「六年级上册 · 第4课」改成四个 Python 探究，并发布 ABC 支架。
 */
@Component
@Order(24)
public class G6UpLesson4Seeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(G6UpLesson4Seeder.class);
    private static final String LESSON_CONTENT =
            "<p>本课用 <strong>Python</strong> 把算法跑起来：先写第一行 <strong>print()</strong>，再把鸡兔同笼写成程序，计算三角形的周长，最后用同样的办法算出盲盒里的隐藏款。</p>"
                    + "<p>请从左侧完成<strong>探究一 第一行代码、探究二 鸡兔同笼程序、探究三 三角形计算、探究四 抽盲盒</strong>，然后做<strong>课堂小测</strong>和<strong>学习评价</strong>。</p>";
    private static final String SCAFFOLD_MARKER = "hello, world";
    private static final String EVAL_MARKER = "第一行代码";
    private static final String QUIZ_MARKER = "hello, world!";

    private static final Map<Integer, String> TITLES = new LinkedHashMap<Integer, String>();
    static {
        TITLES.put(1, "第一行代码");
        TITLES.put(2, "鸡兔同笼程序");
        TITLES.put(3, "三角形计算");
        TITLES.put(4, "抽盲盒");
    }

    private final JdbcTemplate jdbcTemplate;
    private final CourseLessonRepository lessonRepository;
    private final CourseTaskRepository taskRepository;
    private final LessonScaffoldRepository scaffoldRepository;
    private final ObjectMapper objectMapper;

    public G6UpLesson4Seeder(JdbcTemplate jdbcTemplate,
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
            log.warn("未找到六年级上册第4课，跳过更新");
            return;
        }
        CourseLesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson != null && (lesson.getContent() == null || !lesson.getContent().contains("抽盲盒"))) {
            lesson.setContent(LESSON_CONTENT);
            lessonRepository.save(lesson);
        }
        for (CourseTask task : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if (!"EXTERNAL".equals(task.getTaskType()) || task.getConfigJson() == null) {
                continue;
            }
            if (patchWorkspace(task)) {
                taskRepository.save(task);
                log.info("已更新六年级上第4课工作台：lessonId={}", lessonId);
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
            boolean hasFour = false;
            for (JsonNode act : acts) {
                if (!(act instanceof ObjectNode)) {
                    continue;
                }
                int index = act.path("index").asInt(0);
                if (index == 4) {
                    hasFour = true;
                }
                String title = TITLES.get(index);
                if (title == null) {
                    continue;
                }
                boolean same = title.equals(act.path("title").asText())
                        && "/lessons/g6-up-lesson4/index.html".equals(act.path("path").asText())
                        && act.path("unlocked").asBoolean(false);
                if (!same) {
                    ObjectNode one = (ObjectNode) act;
                    one.put("title", title);
                    one.put("path", "/lessons/g6-up-lesson4/index.html");
                    one.put("unlocked", true);
                    changed = true;
                }
            }
            if (!hasFour) {
                ObjectNode act4 = objectMapper.createObjectNode();
                act4.put("index", 4);
                act4.put("title", "抽盲盒");
                act4.put("step", 4);
                act4.put("path", "/lessons/g6-up-lesson4/index.html");
                act4.put("unlocked", true);
                ((ArrayNode) acts).add(act4);
                changed = true;
            }
            JsonNode quizNode = readJson("seed/g6-up-lesson4-quiz.json");
            String currentQuiz = obj.has("quiz") ? obj.get("quiz").toString() : "";
            if (quizNode != null && (currentQuiz.contains("面积") || !currentQuiz.contains(QUIZ_MARKER))) {
                obj.set("quiz", quizNode);
                changed = true;
            }
            JsonNode workspace = readJson("seed/g6-up-lesson4-workspace-meta.json");
            String currentEval = obj.has("evaluationForm") ? obj.get("evaluationForm").toString() : "";
            if (workspace != null && (currentEval.contains("面积") || !currentEval.contains("抽盲盒"))) {
                if (workspace.has("objectives")) {
                    obj.set("objectives", workspace.get("objectives"));
                }
                if (workspace.has("evaluationHint")) {
                    obj.set("evaluationHint", workspace.get("evaluationHint"));
                }
                if (workspace.has("introMap")) {
                    obj.set("introMap", workspace.get("introMap"));
                }
                if (workspace.has("evaluationForm")) {
                    obj.set("evaluationForm", workspace.get("evaluationForm"));
                }
                changed = true;
            }
            if (!changed) {
                return false;
            }
            if (workspace != null && workspace.has("lessonTitle") == false) {
                obj.put("lessonTitle", "第4课 用程序设计语言描述算法");
            }
            task.setConfigJson(objectMapper.writeValueAsString(obj));
            task.setDescription("六年级上第4课学生工作台：第一行代码、鸡兔同笼程序、三角形计算、抽盲盒");
            return true;
        } catch (Exception e) {
            log.warn("更新六年级上第4课工作台失败: {}", e.getMessage());
            return false;
        }
    }

    private void seedScaffold(Long lessonId) {
        String itemsJson = readText("seed/g6-up-lesson4-scaffold.json");
        if (!StringUtils.hasText(itemsJson)) {
            return;
        }
        LessonScaffold row = scaffoldRepository.findByLessonId(lessonId).orElse(null);
        if (row != null && "published".equals(row.getStatus())
                && StringUtils.hasText(row.getItemsJson())
                && row.getItemsJson().contains(SCAFFOLD_MARKER)
                && row.getItemsJson().contains("自己写程序")
                && row.getItemsJson().contains("抽盲盒")) {
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
        log.info("{}六年级上第4课 ABC 支架并已发布：lessonId={}", created ? "已写入" : "已更新", lessonId);
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
                "六年级上册", "MAIN", "第一单元 算法的实现", "第4课 用程序设计语言描述算法");
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        ids = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong(1),
                "六年级上册", "MAIN", "第一单元 算法的实现", "第4课 算法的程序体验");
        return ids.isEmpty() ? null : ids.get(0);
    }
}
