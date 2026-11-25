package com.itech.learnspace.config;

import com.itech.learnspace.entity.CourseLesson;
import com.itech.learnspace.entity.CourseTask;
import com.itech.learnspace.repository.CourseLessonRepository;
import com.itech.learnspace.repository.CourseTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 启动时在「六年级上册 · 第一单元」第7课后挂载「第一单元小测」。
 */
@Component
@Order(33)
public class G6UpUnit1QuizSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(G6UpUnit1QuizSeeder.class);
    private static final String LESSON_TITLE = "第一单元小测";
    private static final String LESSON_CONTENT =
            "<p>本模块为<strong>第一单元小测</strong>。请按要求完成作答并提交；"
                    + "教师批改后，你将看到等级（A/B/C/D）与评语，不显示具体分数。</p>";
    private static final String TASK_TITLE = "第一单元小测作答";
    private static final String TASK_DESC = "六年级上第一单元小测：学生提交作答，教师批改打分，学生仅见等级";

    private final JdbcTemplate jdbcTemplate;
    private final CourseLessonRepository lessonRepository;
    private final CourseTaskRepository taskRepository;

    public G6UpUnit1QuizSeeder(JdbcTemplate jdbcTemplate,
                               CourseLessonRepository lessonRepository,
                               CourseTaskRepository taskRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.lessonRepository = lessonRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long unitId = findUnitId();
        if (unitId == null) {
            log.warn("未找到「六年级上册 / 第一单元 算法的实现」，跳过单元小测挂载");
            return;
        }

        Long lessonId = findQuizLessonId(unitId);
        if (lessonId == null) {
            int sortOrder = nextSortOrder(unitId);
            CourseLesson lesson = new CourseLesson();
            lesson.setUnitId(unitId);
            lesson.setTitle(LESSON_TITLE);
            lesson.setContent(LESSON_CONTENT);
            lesson.setSortOrder(sortOrder);
            lesson.setDurationMin(40);
            lesson.setStatus(1);
            lesson = lessonRepository.save(lesson);
            lessonId = lesson.getId();
            log.info("已创建「第一单元小测」课时：lessonId={}, unitId={}, sortOrder={}", lessonId, unitId, sortOrder);
        } else {
            CourseLesson lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson != null) {
                boolean dirty = false;
                if (lesson.getContent() == null || !lesson.getContent().contains("第一单元小测")) {
                    lesson.setContent(LESSON_CONTENT);
                    dirty = true;
                }
                if (!LESSON_TITLE.equals(lesson.getTitle())) {
                    lesson.setTitle(LESSON_TITLE);
                    dirty = true;
                }
                if (dirty) {
                    lessonRepository.save(lesson);
                }
            }
        }

        String configJson = readQuizJson();
        if (configJson == null) {
            log.error("无法读取单元小测试卷配置，跳过 EXTERNAL 绑定");
            return;
        }

        CourseTask external = null;
        for (CourseTask t : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if ("EXTERNAL".equals(t.getTaskType())) {
                external = t;
                break;
            }
        }
        if (external == null) {
            CourseTask task = new CourseTask();
            task.setLessonId(lessonId);
            task.setTitle(TASK_TITLE);
            task.setDescription(TASK_DESC);
            task.setTaskType("EXTERNAL");
            task.setConfigJson(configJson);
            task.setMaxScore(100);
            task.setSortOrder(1);
            taskRepository.save(task);
            log.info("已绑定第一单元小测 EXTERNAL：lessonId={}", lessonId);
        } else if (!configJson.equals(external.getConfigJson())) {
            external.setTitle(TASK_TITLE);
            external.setDescription(TASK_DESC);
            external.setConfigJson(configJson);
            external.setMaxScore(100);
            taskRepository.save(external);
            log.info("已更新第一单元小测试卷配置：lessonId={}", lessonId);
        }
    }

    private Long findUnitId() {
        String sql = "SELECT cu.id FROM course_unit cu "
                + "INNER JOIN course_grade cg ON cu.grade_id = cg.id "
                + "WHERE cg.name = ? AND cg.textbook_type = ? AND cu.name = ? LIMIT 1";
        List<Long> ids = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong(1),
                "六年级上册", "MAIN", "第一单元 算法的实现");
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long findQuizLessonId(Long unitId) {
        String sql = "SELECT id FROM course_lesson WHERE unit_id = ? AND title = ? LIMIT 1";
        List<Long> ids = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong(1),
                unitId, LESSON_TITLE);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private int nextSortOrder(Long unitId) {
        // 尽量排在第7课之后
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT title, sort_order FROM course_lesson WHERE unit_id = ? ORDER BY sort_order ASC, id ASC",
                unitId);
        Integer after7 = null;
        int max = 0;
        for (Map<String, Object> row : rows) {
            String title = String.valueOf(row.get("title"));
            Object so = row.get("sort_order");
            int order = so instanceof Number ? ((Number) so).intValue() : 0;
            if (order > max) {
                max = order;
            }
            if (title != null && title.contains("第7课")) {
                after7 = order + 1;
            }
        }
        if (after7 != null) {
            return after7;
        }
        return max + 1;
    }

    private String readQuizJson() {
        try (java.io.InputStream in = new ClassPathResource("seed/g6-up-unit1-quiz.json").getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.error("读取第一单元小测试卷配置失败", e);
            return null;
        }
    }
}
