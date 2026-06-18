package com.itech.learnspace.service;

import com.itech.learnspace.entity.*;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.*;
import com.itech.learnspace.util.TextbookPdfHelper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CourseService {

    private final CourseGradeRepository gradeRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseResourceRepository resourceRepository;
    private final CourseTaskRepository taskRepository;
    private final LearnProgressRepository progressRepository;

    public CourseService(CourseGradeRepository gradeRepository, CourseUnitRepository unitRepository,
                         CourseLessonRepository lessonRepository, CourseResourceRepository resourceRepository,
                         CourseTaskRepository taskRepository, LearnProgressRepository progressRepository) {
        this.gradeRepository = gradeRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
        this.resourceRepository = resourceRepository;
        this.taskRepository = taskRepository;
        this.progressRepository = progressRepository;
    }

    /**
     * 课程地图：年级 → 单元 → 课时，附带学生学习状态（全量，管理端或兼容用）
     */
    public List<Map<String, Object>> getCourseMap(Long studentId) {
        List<CourseGrade> grades = gradeRepository.findAllByOrderBySortOrderAsc();
        Map<Long, LearnProgress> progressMap = buildProgressMap(studentId);

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (CourseGrade grade : grades) {
            result.add(buildGradeTree(grade, progressMap));
        }
        return result;
    }

    /** 配套 / 校本：仅返回册次列表与封面（用于首页教材入口） */
    public List<Map<String, Object>> listTextbooks(String textbookType) {
        List<CourseGrade> list = gradeRepository.findByTextbookTypeOrderBySortOrderAsc(textbookType);
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (CourseGrade g : list) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("id", g.getId());
            m.put("name", g.getName());
            m.put("description", g.getDescription());
            m.put("coverUrl", g.getCoverUrl());
            m.put("pdfUrl", TextbookPdfHelper.resolvePdfUrl(g));
            m.put("textbookType", g.getTextbookType());
            m.put("sortOrder", g.getSortOrder());
            out.add(m);
        }
        return out;
    }

    /** 某一册下的单元与课时（学生点进某本教材后） */
    public Map<String, Object> getGradeOutline(Long gradeId, Long studentId) {
        CourseGrade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new BusinessException("教材不存在"));
        return buildGradeTree(grade, buildProgressMap(studentId));
    }

    private Map<Long, LearnProgress> buildProgressMap(Long studentId) {
        Map<Long, LearnProgress> progressMap = new HashMap<Long, LearnProgress>();
        if (studentId != null) {
            for (LearnProgress p : progressRepository.findByStudentId(studentId)) {
                progressMap.put(p.getLessonId(), p);
            }
        }
        return progressMap;
    }

    private Map<String, Object> buildGradeTree(CourseGrade grade, Map<Long, LearnProgress> progressMap) {
        Map<String, Object> gradeMap = new HashMap<String, Object>();
        gradeMap.put("id", grade.getId());
        gradeMap.put("name", grade.getName());
        gradeMap.put("description", grade.getDescription());
        gradeMap.put("textbookType", grade.getTextbookType());
        gradeMap.put("coverUrl", grade.getCoverUrl());
        gradeMap.put("pdfUrl", TextbookPdfHelper.resolvePdfUrl(grade));

        List<Map<String, Object>> units = new ArrayList<Map<String, Object>>();
        for (CourseUnit unit : unitRepository.findByGradeIdOrderBySortOrderAsc(grade.getId())) {
            Map<String, Object> unitMap = new HashMap<String, Object>();
            unitMap.put("id", unit.getId());
            unitMap.put("name", unit.getName());

            List<Map<String, Object>> lessons = new ArrayList<Map<String, Object>>();
            for (CourseLesson lesson : lessonRepository.findByUnitIdOrderBySortOrderAsc(unit.getId())) {
                Map<String, Object> lessonMap = new HashMap<String, Object>();
                lessonMap.put("id", lesson.getId());
                lessonMap.put("title", lesson.getTitle());
                lessonMap.put("durationMin", lesson.getDurationMin());

                LearnProgress progress = progressMap.get(lesson.getId());
                lessonMap.put("status", progress != null ? progress.getStatus() : "NOT_STARTED");
                lessonMap.put("progressPercent", progress != null ? progress.getProgressPercent() : 0);
                lessons.add(lessonMap);
            }
            unitMap.put("lessons", lessons);
            units.add(unitMap);
        }
        gradeMap.put("units", units);
        return gradeMap;
    }

    /** 课时详情：资源 + 任务列表 */
    public Map<String, Object> getLessonDetail(Long lessonId, Long studentId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("lesson", lesson);
        result.put("resources", resourceRepository.findByLessonIdOrderBySortOrderAsc(lessonId));
        result.put("tasks", taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId));

        if (studentId != null) {
            LearnProgress progress = progressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                    .orElse(null);
            result.put("progress", progress);
        }
        return result;
    }
}
