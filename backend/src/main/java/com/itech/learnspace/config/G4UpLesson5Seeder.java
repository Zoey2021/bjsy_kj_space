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
 * 启动时绑定「四年级上册 · 第5课 数据获取」学生工作台。
 * 探究一：情境配对；探究二：设计问卷；探究三：收回汇总。
 */
@Component
@Order(28)
public class G4UpLesson5Seeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(G4UpLesson5Seeder.class);
    private static final String LESSON_PATH = "/lessons/g4-up-lesson5/index.html";
    private static final String PLACEHOLDER = "请按教师课堂安排完成学习";
    private static final String LESSON_CONTENT =
            "<p>本课学习<strong>数据获取</strong>：区分现场记录、问卷调查、网络获取和设备采集，按顺序设计一份合格问卷，并把收回的答卷汇总成数据。</p>"
                    + "<p>请从左侧依次完成<strong>探究一、探究二、探究三</strong>，然后作答<strong>课堂小测</strong>并填写<strong>学习评价</strong>。</p>";
    private static final String TASK_TITLE = "数据获取探究";
    private static final String TASK_DESC = "四年级上第5课学生工作台：探究一情境配对、探究二设计问卷、探究三收回汇总、学习评价、课堂小测";

    private final JdbcTemplate jdbcTemplate;
    private final CourseLessonRepository lessonRepository;
    private final CourseResourceRepository resourceRepository;
    private final CourseTaskRepository taskRepository;

    public G4UpLesson5Seeder(JdbcTemplate jdbcTemplate,
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
            log.warn("未找到「四年级上册 / 第二单元 数据证明观点 / 第5课 数据获取」，跳过工作台绑定");
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
            res.setTitle("数据获取探究活动");
            res.setResType("WEB");
            res.setContentUrl(LESSON_PATH);
            res.setSortOrder(1);
            resourceRepository.save(res);
            log.info("已绑定四年级上第5课 WEB 资源：lessonId={}", lessonId);
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
            log.info("已绑定四年级上第5课 EXTERNAL 工作台：lessonId={}", lessonId);
        } else if (configJson != null && !configJson.equals(external.getConfigJson())) {
            external.setTitle(TASK_TITLE);
            external.setDescription(TASK_DESC);
            external.setConfigJson(configJson);
            external.setMaxScore(100);
            taskRepository.save(external);
            log.info("已更新四年级上第5课工作台配置：lessonId={}", lessonId);
        }
    }

    private String readWorkspaceJson() {
        try (java.io.InputStream in = new ClassPathResource("seed/g4-up-lesson5-workspace.json").getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.error("读取四年级上第5课工作台配置失败", e);
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
                "四年级上册", "MAIN", "第二单元 数据证明观点", "第5课 数据获取");
        return ids.isEmpty() ? null : ids.get(0);
    }
}
