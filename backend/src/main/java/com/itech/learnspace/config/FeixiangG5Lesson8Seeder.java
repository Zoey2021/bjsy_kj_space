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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时确保「五年级下册 · 第8课」已绑定飞象探究单（无需手动执行迁移 SQL）。
 */
@Component
public class FeixiangG5Lesson8Seeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FeixiangG5Lesson8Seeder.class);
    private static final String LESSON_PATH = "/lessons/g5-lesson8/index.html";
    private static final String CONFIG_JSON = "{\"source\":\"feixiang\",\"path\":\"/lessons/g5-lesson8/index.html\"}";

    private final JdbcTemplate jdbcTemplate;
    private final CourseLessonRepository lessonRepository;
    private final CourseResourceRepository resourceRepository;
    private final CourseTaskRepository taskRepository;

    public FeixiangG5Lesson8Seeder(JdbcTemplate jdbcTemplate,
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
            log.warn("未找到「五年级下册 / 第二单元 控制系统 / 第8课 体验控制系统」，跳过飞象探究单绑定");
            return;
        }

        CourseLesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            return;
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
            res.setTitle("体验控制系统探究单");
            res.setResType("WEB");
            res.setContentUrl(LESSON_PATH);
            res.setSortOrder(1);
            resourceRepository.save(res);
            log.info("已绑定飞象 WEB 资源：lessonId={}", lessonId);
        }

        boolean hasExternal = false;
        for (CourseTask t : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if ("EXTERNAL".equals(t.getTaskType())) {
                hasExternal = true;
                break;
            }
        }
        if (!hasExternal) {
            CourseTask task = new CourseTask();
            task.setLessonId(lessonId);
            task.setTitle("探究单互动挑战");
            task.setDescription("飞象老师探究单：体验控制系统");
            task.setTaskType("EXTERNAL");
            task.setConfigJson(CONFIG_JSON);
            task.setMaxScore(100);
            task.setSortOrder(1);
            taskRepository.save(task);
            log.info("已绑定飞象 EXTERNAL 任务：lessonId={}", lessonId);
        }
    }

    private Long findLessonId() {
        String sql = "SELECT cl.id FROM course_lesson cl "
                + "INNER JOIN course_unit cu ON cl.unit_id = cu.id "
                + "INNER JOIN course_grade cg ON cu.grade_id = cg.id "
                + "WHERE cg.name = ? AND cu.name = ? AND cl.title = ? LIMIT 1";
        List<Long> ids = jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong(1),
                "五年级下册", "第二单元 控制系统", "第8课 体验控制系统");
        return ids.isEmpty() ? null : ids.get(0);
    }
}
