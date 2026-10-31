package com.itech.learnspace.config;

import com.itech.learnspace.entity.CourseGrade;
import com.itech.learnspace.entity.CourseLesson;
import com.itech.learnspace.entity.CourseUnit;
import com.itech.learnspace.repository.CourseGradeRepository;
import com.itech.learnspace.repository.CourseLessonRepository;
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
 * 校本课程「人工智能启蒙」上下册目录：二年级 AI 科创课，各 15 课。
 */
@Component
@Order(27)
public class AiEnlightenSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiEnlightenSeeder.class);

    private static final class VolumeDef {
        final String volumeName;
        final String description;
        final List<ModuleDef> modules;

        VolumeDef(String volumeName, String description, List<ModuleDef> modules) {
            this.volumeName = volumeName;
            this.description = description;
            this.modules = modules;
        }
    }

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

    private static final List<VolumeDef> VOLUMES = Arrays.asList(
            new VolumeDef("上册", "人工智能启蒙 · AI初体验·认识智能新朋友（上册）", Arrays.asList(
                    new ModuleDef("ai-meet", "第一板块 · 认识智能新朋友", "从生活里的智能伙伴认起。", new String[]{
                            "AI来敲门——生活里的智能小伙伴",
                            "AI的“小耳朵”——语音识别真神奇",
                            "AI的“大眼睛”——图像识别小能手",
                            "AI的“好记性”——大数据藏秘密"
                    }),
                    new ModuleDef("ai-think", "第二板块 · 规则与思维", "知道 AI 和人的不同，并练习顺序与判断。", new String[]{
                            "AI和人类大不同",
                            "AI要守“小规则”",
                            "一步步做事——顺序思维小训练",
                            "如果…就…——AI的判断小魔法"
                    }),
                    new ModuleDef("ai-safe", "第三板块 · 学习与安全", "把 AI 当成帮手，同时守住小秘密。", new String[]{
                            "AI分类小达人",
                            "找规律——AI的超能力",
                            "AI是我们的学习小帮手",
                            "AI安全小卫士——保护小秘密"
                    }),
                    new ModuleDef("ai-show", "第四板块 · 设计与展示", "知道 AI 也会出错，并完成上册小报。", new String[]{
                            "AI也会“小马虎”",
                            "我是小小AI设计师",
                            "上册成果展示——我的AI小报"
                    })
            )),
            new VolumeDef("下册", "人工智能启蒙 · 我是AI小玩家·探秘逻辑与未来（下册）", Arrays.asList(
                    new ModuleDef("ai-play", "第一板块 · 动手体验", "自己扮演语音助手和图像侦探。", new String[]{
                            "我是小小语音助手",
                            "我是图像识别小侦探",
                            "训练“猜拳AI”——数据的魔力",
                            "AI走迷宫——指令大挑战"
                    }),
                    new ModuleDef("ai-learn", "第二板块 · 数据与学习", "用 AI 看数据、学汉字、找规律。", new String[]{
                            "班级AI投票器——数据分析小实践",
                            "AI帮我们学汉字——找偏旁小能手",
                            "AI口算小老师——找数学规律",
                            "AI观察大自然——动植物小百科"
                    }),
                    new ModuleDef("ai-life", "第三板块 · 生活应用", "把 AI 用到环保、作息和家里。", new String[]{
                            "AI环保小卫士——垃圾分类我最行",
                            "AI时间小管家——制定作息表",
                            "未来的家——AI智能家居畅想",
                            "未来的学校——AI老师来了"
                    }),
                    new ModuleDef("ai-future", "第四板块 · 未来与结业", "做一项小发明，并完成结业发布。", new String[]{
                            "我的环保AI小发明",
                            "正确使用AI——AI的利与弊",
                            "下册结业课——AI小小发布会"
                    })
            ))
    );

    private final CourseGradeRepository gradeRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseLessonRepository lessonRepository;

    public AiEnlightenSeeder(CourseGradeRepository gradeRepository,
                             CourseUnitRepository unitRepository,
                             CourseLessonRepository lessonRepository) {
        this.gradeRepository = gradeRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (VolumeDef volume : VOLUMES) {
            CourseGrade grade = findVolume(volume.volumeName);
            if (grade == null) {
                log.warn("未找到校本课程「人工智能启蒙 · {}」，跳过目录种植", volume.volumeName);
                continue;
            }
            if (!volume.description.equals(grade.getDescription())) {
                grade.setDescription(volume.description);
                gradeRepository.save(grade);
            }
            int lessonNo = 1;
            int unitOrder = 1;
            for (ModuleDef module : volume.modules) {
                CourseUnit unit = findOrCreateUnit(grade.getId(), unitOrder, module);
                int inUnit = 1;
                for (String title : module.lessons) {
                    findOrCreateLesson(unit.getId(), inUnit, "第" + lessonNo + "课 " + title);
                    inUnit++;
                    lessonNo++;
                }
                unitOrder++;
            }
            log.info("人工智能启蒙{}目录已就绪：{} 课", volume.volumeName, lessonNo - 1);
        }
    }

    private CourseGrade findVolume(String volumeName) {
        for (CourseGrade g : gradeRepository.findByTextbookTypeOrderBySortOrderAsc("SCHOOL")) {
            String desc = g.getDescription() == null ? "" : g.getDescription();
            String name = g.getName() == null ? "" : g.getName();
            if (desc.contains("人工智能启蒙") && volumeName.equals(name)) {
                return g;
            }
        }
        return null;
    }

    private CourseUnit findOrCreateUnit(Long gradeId, int sortOrder, ModuleDef module) {
        for (CourseUnit u : unitRepository.findByGradeIdOrderBySortOrderAsc(gradeId)) {
            String desc = u.getDescription() == null ? "" : u.getDescription();
            if (desc.startsWith(module.key)) {
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

    private void findOrCreateLesson(Long unitId, int sortOrder, String title) {
        for (CourseLesson lesson : lessonRepository.findByUnitIdOrderBySortOrderAsc(unitId)) {
            if (sortOrder == (lesson.getSortOrder() == null ? 0 : lesson.getSortOrder())) {
                lesson.setTitle(title);
                if (lesson.getDurationMin() == null) {
                    lesson.setDurationMin(40);
                }
                if (lesson.getStatus() == null) {
                    lesson.setStatus(1);
                }
                lessonRepository.save(lesson);
                return;
            }
        }
        CourseLesson lesson = new CourseLesson();
        lesson.setUnitId(unitId);
        lesson.setTitle(title);
        lesson.setSortOrder(sortOrder);
        lesson.setDurationMin(40);
        lesson.setStatus(1);
        lesson.setContent("<p>本课属于人工智能启蒙。课件准备好后即可在这里打开。</p>");
        lessonRepository.save(lesson);
    }
}
