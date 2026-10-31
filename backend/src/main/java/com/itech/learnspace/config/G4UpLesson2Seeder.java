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
 * 启动时绑定「四年级上册 · 第2课 多样的数据」学生工作台
 * （环节1 身份证数据、环节2 五种形式、环节3 存储介质）。
 */
@Component
@Order(22)
public class G4UpLesson2Seeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(G4UpLesson2Seeder.class);
    private static final String LESSON_PATH = "/lessons/g4-up-lesson2/index.html";
    private static final String PLACEHOLDER = "请按教师课堂安排完成学习";
    private static final String LESSON_CONTENT =
            "<p>本课学习<strong>多样的数据</strong>：在同一对象上找出多种数据，辨认数字、文本、图片、音频、视频五种形式，并了解存储介质从古到今的变化。</p>"
                    + "<p>请从左侧依次完成<strong>环节1、环节2、环节3</strong>，然后填写<strong>学习评价</strong>并作答<strong>课堂小测</strong>。</p>";

    private final JdbcTemplate jdbcTemplate;
    private final CourseLessonRepository lessonRepository;
    private final CourseResourceRepository resourceRepository;
    private final CourseTaskRepository taskRepository;

    public G4UpLesson2Seeder(JdbcTemplate jdbcTemplate,
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
            log.warn("未找到「四年级上册 / 第一单元 泛在的数据 / 第2课 多样的数据」，跳过工作台绑定");
            return;
        }

        CourseLesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return;
        }

        String content = lesson.getContent();
        if (content == null || content.contains(PLACEHOLDER)) {
            lesson.setContent(LESSON_CONTENT);
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
            res.setTitle("多样的数据探究活动");
            res.setResType("WEB");
            res.setContentUrl(LESSON_PATH);
            res.setSortOrder(1);
            resourceRepository.save(res);
            log.info("已绑定四年级上第2课 WEB 资源：lessonId={}", lessonId);
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
            task.setTitle("多样的数据探究");
            task.setDescription("四年级上第2课学生工作台：环节1身份证数据、环节2五种形式、环节3存储介质、学习评价、课堂小测");
            task.setTaskType("EXTERNAL");
            task.setConfigJson(configJson);
            task.setMaxScore(100);
            task.setSortOrder(1);
            taskRepository.save(task);
            log.info("已绑定四年级上第2课 EXTERNAL 工作台：lessonId={}", lessonId);
        } else if (configJson != null && !configJson.equals(external.getConfigJson())) {
            external.setTitle("多样的数据探究");
            external.setDescription("四年级上第2课学生工作台：环节1身份证数据、环节2五种形式、环节3存储介质、学习评价、课堂小测");
            external.setConfigJson(configJson);
            external.setMaxScore(100);
            taskRepository.save(external);
            log.info("已更新四年级上第2课工作台配置：lessonId={}", lessonId);
        }
    }

    private String readWorkspaceJson() {
        try (java.io.InputStream in = new ClassPathResource("seed/g4-up-lesson2-workspace.json").getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.error("读取四年级上第2课工作台配置失败", e);
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
                "四年级上册", "MAIN", "第一单元 泛在的数据", "第2课 多样的数据");
        return ids.isEmpty() ? null : ids.get(0);
    }
}
