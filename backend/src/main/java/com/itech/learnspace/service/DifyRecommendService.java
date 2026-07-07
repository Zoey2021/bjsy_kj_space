package com.itech.learnspace.service;

import com.itech.learnspace.config.DifyProperties;
import com.itech.learnspace.entity.CourseGrade;
import com.itech.learnspace.entity.CourseLesson;
import com.itech.learnspace.entity.CourseUnit;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.CourseGradeRepository;
import com.itech.learnspace.repository.CourseLessonRepository;
import com.itech.learnspace.repository.CourseUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class DifyRecommendService {

    private final DifyProperties difyProperties;
    private final DifyWorkflowClient workflowClient;
    private final CourseLessonRepository lessonRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseGradeRepository gradeRepository;

    public DifyRecommendService(DifyProperties difyProperties,
                                  DifyWorkflowClient workflowClient,
                                  CourseLessonRepository lessonRepository,
                                  CourseUnitRepository unitRepository,
                                  CourseGradeRepository gradeRepository) {
        this.difyProperties = difyProperties;
        this.workflowClient = workflowClient;
        this.lessonRepository = lessonRepository;
        this.unitRepository = unitRepository;
        this.gradeRepository = gradeRepository;
    }

    public Map<String, Object> recommendActivities(Long lessonId, Long teacherId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        CourseUnit unit = unitRepository.findById(lesson.getUnitId())
                .orElseThrow(() -> new BusinessException("单元不存在"));
        CourseGrade grade = gradeRepository.findById(unit.getGradeId())
                .orElseThrow(() -> new BusinessException("教材不存在"));

        if (!StringUtils.hasText(difyProperties.getRecommendApiKey())) {
            throw new BusinessException("未配置 DIFY_RECOMMEND_API_KEY，请在环境变量或 application.yml 中设置");
        }

        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put("grade_name", grade.getName());
        inputs.put("lesson_title", unit.getName() + " " + lesson.getTitle());

        String raw = workflowClient.runWorkflow(
                difyProperties.getRecommendApiKey(),
                difyProperties.getRecommendAppMode(),
                inputs,
                "teacher-" + teacherId);

        if (!StringUtils.hasText(raw)) {
            throw new BusinessException("AI 未返回内容，请检查 Dify 是否已启动且 API Key 正确");
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("rawContent", raw);
        result.put("gradeName", grade.getName());
        result.put("lessonTitle", lesson.getTitle());
        return result;
    }
}
