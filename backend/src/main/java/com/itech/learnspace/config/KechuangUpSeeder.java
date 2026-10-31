package com.itech.learnspace.config;

import com.itech.learnspace.entity.CourseGrade;
import com.itech.learnspace.entity.CourseLesson;
import com.itech.learnspace.entity.CourseResource;
import com.itech.learnspace.entity.CourseUnit;
import com.itech.learnspace.repository.CourseGradeRepository;
import com.itech.learnspace.repository.CourseLessonRepository;
import com.itech.learnspace.repository.CourseResourceRepository;
import com.itech.learnspace.repository.CourseUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 校本课程「科创启航 · 上册」：3D打印 / 机器人 / 天文 / 人工智能，共 15 课。
 */
@Component
@Order(23)
public class KechuangUpSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KechuangUpSeeder.class);

    private static final class ModuleDef {
        final String key;
        final String name;
        final String blurb;
        final String[] lessons;

        ModuleDef(String key, String name, String blurb, String[] lessons) {
            this.key = key;
            this.name = name;
            this.blurb = blurb;
            this.lessons = lessons;
        }
    }

    private static final List<ModuleDef> MODULES = Arrays.asList(
            new ModuleDef("print3d", "第一板块 · 3D打印", "从认识到软件，再到近距离体验打印。", new String[]{
                    "神奇的3D打印（1）",
                    "神奇的3D打印（2）",
                    "3D软件早知道",
                    "3D打印零距离"
            }),
            new ModuleDef("robot", "第二板块 · 机器人", "了解机器人的过去、现在和未来。", new String[]{
                    "机器人的历史发展",
                    "医用机器人与我们的生活",
                    "工业机器人与我们的生活",
                    "人型机器人与未来发展"
            }),
            new ModuleDef("astro", "第三板块 · 天文", "认识太阳系，走近日历与星空。", new String[]{
                    "和天文学交个朋友吧",
                    "谁是太阳系的老二",
                    "日历是怎么来的",
                    "太阳系里还有谁"
            }),
            new ModuleDef("ai", "第四板块 · 人工智能", "认识人工智能、计算机视觉与语音识别。", new String[]{
                    "什么是人工智能",
                    "计算机视觉",
                    "语音识别"
            })
    );

    private final CourseGradeRepository gradeRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseResourceRepository resourceRepository;

    public KechuangUpSeeder(CourseGradeRepository gradeRepository,
                            CourseUnitRepository unitRepository,
                            CourseLessonRepository lessonRepository,
                            CourseResourceRepository resourceRepository) {
        this.gradeRepository = gradeRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        CourseGrade grade = findKechuangUp();
        if (grade == null) {
            log.warn("未找到校本课程「科创启航 · 二年级上册」，跳过目录种植");
            return;
        }

        int lessonNo = 1;
        int unitOrder = 1;
        for (ModuleDef module : MODULES) {
            CourseUnit unit = findOrCreateUnit(grade.getId(), unitOrder, module);
            int inUnit = 1;
            for (String title : module.lessons) {
                CourseLesson lesson = findOrCreateLesson(unit.getId(), inUnit, "第" + lessonNo + "课  " + title);
                ensurePptResource(lesson.getId(), lessonNo, title);
                inUnit++;
                lessonNo++;
            }
            unitOrder++;
        }
        log.info("科创启航上册目录已就绪：4 个板块、{} 课", lessonNo - 1);
    }

    private CourseGrade findKechuangUp() {
        for (CourseGrade g : gradeRepository.findByTextbookTypeOrderBySortOrderAsc("SCHOOL")) {
            String desc = g.getDescription() == null ? "" : g.getDescription();
            String name = g.getName() == null ? "" : g.getName();
            if (desc.contains("科创启航") && name.contains("上册")) {
                return g;
            }
        }
        return null;
    }

    private CourseUnit findOrCreateUnit(Long gradeId, int sortOrder, ModuleDef module) {
        for (CourseUnit u : unitRepository.findByGradeIdOrderBySortOrderAsc(gradeId)) {
            String desc = u.getDescription() == null ? "" : u.getDescription();
            String name = u.getName() == null ? "" : u.getName();
            if (desc.startsWith(module.key) || name.contains(module.name.substring(module.name.indexOf('·') + 1).trim())) {
                u.setName(module.name);
                u.setSortOrder(sortOrder);
                u.setDescription(module.key + "|" + module.blurb);
                return unitRepository.save(u);
            }
        }
        CourseUnit unit = new CourseUnit();
        unit.setGradeId(gradeId);
        unit.setName(module.name);
        unit.setSortOrder(sortOrder);
        unit.setDescription(module.key + "|" + module.blurb);
        return unitRepository.save(unit);
    }

    private CourseLesson findOrCreateLesson(Long unitId, int sortOrder, String title) {
        for (CourseLesson lesson : lessonRepository.findByUnitIdOrderBySortOrderAsc(unitId)) {
            if (sortOrder == (lesson.getSortOrder() == null ? 0 : lesson.getSortOrder())) {
                lesson.setTitle(title);
                if (lesson.getDurationMin() == null) {
                    lesson.setDurationMin(40);
                }
                return lessonRepository.save(lesson);
            }
        }
        CourseLesson lesson = new CourseLesson();
        lesson.setUnitId(unitId);
        lesson.setTitle(title);
        lesson.setSortOrder(sortOrder);
        lesson.setDurationMin(40);
        lesson.setStatus(1);
        lesson.setContent("<p>本课为校本课程课件课，点击后可直接查看教学 PPT。</p>");
        return lessonRepository.save(lesson);
    }

    private void ensurePptResource(Long lessonId, int lessonNo, String title) {
        String url = String.format("/school/kechuang-up/lesson-%02d.pptx", lessonNo);
        for (CourseResource r : resourceRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if ("PPT".equalsIgnoreCase(r.getResType())) {
                r.setTitle(title + " 课件");
                r.setContentUrl(url);
                resourceRepository.save(r);
                return;
            }
        }
        CourseResource res = new CourseResource();
        res.setLessonId(lessonId);
        res.setTitle(title + " 课件");
        res.setResType("PPT");
        res.setContentUrl(url);
        res.setSortOrder(1);
        resourceRepository.save(res);
    }
}
