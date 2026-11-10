package com.itech.learnspace.config;

import com.itech.learnspace.entity.CourseLesson;
import com.itech.learnspace.entity.CourseResource;
import com.itech.learnspace.entity.CourseTask;
import com.itech.learnspace.repository.CourseLessonRepository;
import com.itech.learnspace.repository.CourseResourceRepository;
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

/**
 * 启动时绑定「六年级上册 · 第7课 用人工智能辅助算法实现」学生工作台。
 */
@Component
@Order(32)
public class G6UpLesson7Seeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(G6UpLesson7Seeder.class);
    private static final String LESSON_PATH = "/lessons/g6-up-lesson7/index.html";
    private static final String PLACEHOLDER = "请按教师课堂安排完成学习";
    private static final String LESSON_CONTENT =
            "<p>本课学习<strong>用人工智能辅助算法实现</strong>：体验猜数字并梳理步骤，读懂循环与条件判断，"
                    + "用<strong>算法小助手</strong>生成并完善程序，再参与观点投票。</p>"
                    + "<p>请依次完成<strong>探究一～探究四</strong>，然后作答<strong>课堂小测</strong>并填写<strong>学习评价</strong>。</p>";
    private static final String TASK_TITLE = "猜数字 AI 辅助实现探究";
    private static final String TASK_DESC = "六年级上第7课：探究一游戏规则、探究二读代码、探究三算法小助手、探究四观点投票、学习评价、课堂小测";

    private final JdbcTemplate jdbcTemplate;
    private final CourseLessonRepository lessonRepository;
    private final CourseResourceRepository resourceRepository;
    private final CourseTaskRepository taskRepository;

    public G6UpLesson7Seeder(JdbcTemplate jdbcTemplate,
                             CourseLessonRepository lessonRepository,
                             CourseResourceRepository resourceRepository,
                             CourseTaskRepository taskRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.lessonRepository = lessonRepository;
        this.resourceRepository = resourceRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long lessonId = findLessonId();
        if (lessonId == null) {
            log.warn("未找到「六年级上册 / 第一单元 算法的实现 / 第7课 用人工智能辅助算法实现」，跳过工作台绑定");
            return;
        }

        CourseLesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return;
        }

        String content = lesson.getContent();
        if (content == null || content.contains(PLACEHOLDER) || !content.contains("人工智能辅助")) {
            lesson.setContent(LESSON_CONTENT);
            if (lesson.getTitle() == null || lesson.getTitle().contains("猜数字算法验证")) {
                lesson.setTitle("第7课 用人工智能辅助算法实现");
            }
            lessonRepository.save(lesson);
        }

        boolean hasWeb = false;
        for (CourseResource r : resourceRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if ("WEB".equals(r.getResType()) && LESSON_PATH.equals(r.getContentUrl())) {
                hasWeb = true;
                break;
            }
        }
        if (!hasWeb) {
            CourseResource res = new CourseResource();
            res.setLessonId(lessonId);
            res.setTitle("猜数字 AI 辅助实现探究活动");
            res.setResType("WEB");
            res.setContentUrl(LESSON_PATH);
            res.setSortOrder(1);
            resourceRepository.save(res);
            log.info("已绑定六年级上第7课 WEB 资源：lessonId={}", lessonId);
        }

        String configJson = readWorkspaceJson();
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
            log.info("已绑定六年级上第7课 EXTERNAL 工作台：lessonId={}", lessonId);
        } else if (configJson != null && !configJson.equals(external.getConfigJson())) {
            external.setTitle(TASK_TITLE);
            external.setDescription(TASK_DESC);
            external.setConfigJson(configJson);
            external.setMaxScore(100);
            taskRepository.save(external);
            log.info("已更新六年级上第7课工作台配置：lessonId={}", lessonId);
        }
    }

    private String readWorkspaceJson() {
        try (java.io.InputStream in = new ClassPathResource("seed/g6-up-lesson7-workspace.json").getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.error("读取六年级上第7课工作台配置失败", e);
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
                "六年级上册", "MAIN", "第一单元 算法的实现", "第7课 用人工智能辅助算法实现");
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        // 兼容旧标题
        ids = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong(1),
                "六年级上册", "MAIN", "第一单元 算法的实现", "第7课 猜数字算法验证");
        return ids.isEmpty() ? null : ids.get(0);
    }
}
