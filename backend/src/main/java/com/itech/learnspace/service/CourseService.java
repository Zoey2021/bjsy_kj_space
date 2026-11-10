package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itech.learnspace.entity.*;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.*;
import com.itech.learnspace.util.TextbookPdfHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class CourseService {

    private final CourseGradeRepository gradeRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseResourceRepository resourceRepository;
    private final CourseTaskRepository taskRepository;
    private final LearnProgressRepository progressRepository;
    private final LearnSubmissionRepository submissionRepository;
    private final LearnSubmissionLogRepository submissionLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CourseService(CourseGradeRepository gradeRepository, CourseUnitRepository unitRepository,
                         CourseLessonRepository lessonRepository, CourseResourceRepository resourceRepository,
                         CourseTaskRepository taskRepository, LearnProgressRepository progressRepository,
                         LearnSubmissionRepository submissionRepository,
                         LearnSubmissionLogRepository submissionLogRepository) {
        this.gradeRepository = gradeRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
        this.resourceRepository = resourceRepository;
        this.taskRepository = taskRepository;
        this.progressRepository = progressRepository;
        this.submissionRepository = submissionRepository;
        this.submissionLogRepository = submissionLogRepository;
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
            String pdfUrl = TextbookPdfHelper.resolvePdfUrl(g);
            m.put("pdfUrl", pdfUrl);
            m.put("textbookType", g.getTextbookType());
            m.put("sortOrder", g.getSortOrder());
            boolean hasPdf = pdfUrl != null && !pdfUrl.isEmpty();
            // 用 exists，勿 find 全量单元：首页教材入口对每册多一次全表会明显卡顿
            boolean hasUnits = unitRepository.existsByGradeId(g.getId());
            m.put("hasResource", hasPdf || hasUnits);
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
            unitMap.put("description", unit.getDescription());
            unitMap.put("sortOrder", unit.getSortOrder());

            List<Map<String, Object>> lessons = new ArrayList<Map<String, Object>>();
            for (CourseLesson lesson : lessonRepository.findByUnitIdOrderBySortOrderAsc(unit.getId())) {
                Map<String, Object> lessonMap = new HashMap<String, Object>();
                lessonMap.put("id", lesson.getId());
                lessonMap.put("title", lesson.getTitle());
                lessonMap.put("durationMin", lesson.getDurationMin());
                lessonMap.put("pptUrl", firstPptUrl(lesson.getId()));

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

    private String firstPptUrl(Long lessonId) {
        for (CourseResource r : resourceRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            String type = r.getResType() == null ? "" : r.getResType().toUpperCase();
            if ("PPT".equals(type) || "PPTX".equals(type) || "SLIDE".equals(type)) {
                return r.getContentUrl();
            }
        }
        return null;
    }

    /** 课时详情：资源 + 任务列表 */
    public Map<String, Object> getLessonDetail(Long lessonId, Long studentId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("lesson", lesson);
        result.put("resources", resourceRepository.findByLessonIdOrderBySortOrderAsc(lessonId));
        List<CourseTask> tasks = taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
        if (studentId != null) {
            attachStudentProgress(lessonId, studentId, tasks);
        }
        result.put("tasks", tasks);

        if (studentId != null) {
            LearnProgress progress = progressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                    .orElse(null);
            result.put("progress", progress);
        }

        CourseUnit unit = unitRepository.findById(lesson.getUnitId()).orElse(null);
        if (unit != null) {
            CourseGrade grade = gradeRepository.findById(unit.getGradeId()).orElse(null);
            if (grade != null) {
                result.put("gradeId", grade.getId());
                result.put("gradeName", grade.getName());
                result.put("gradeDescription", grade.getDescription());
                result.put("textbookType", grade.getTextbookType());
                result.put("textbookPdfUrl", TextbookPdfHelper.resolvePdfUrl(grade));
            }
        }
        return result;
    }

    /**
     * 把该学生已经保存的完成情况附在任务上。
     * 提交原文被后来的草稿盖掉时，仍从每次正式提交的记录里把探究、小测、评价补回来。
     */
    private void attachStudentProgress(Long lessonId, Long studentId, List<CourseTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        List<Long> taskIds = new ArrayList<Long>();
        for (CourseTask task : tasks) {
            taskIds.add(task.getId());
        }
        Map<Long, LearnSubmission> byTask = new HashMap<Long, LearnSubmission>();
        for (LearnSubmission sub : submissionRepository.findByTaskIdIn(taskIds)) {
            if (studentId.equals(sub.getStudentId())) {
                byTask.put(sub.getTaskId(), sub);
            }
        }
        List<LearnSubmissionLog> logs =
                submissionLogRepository.findByStudentIdAndLessonIdOrderByCreatedAtDesc(studentId, lessonId);
        for (CourseTask task : tasks) {
            LearnSubmission sub = byTask.get(task.getId());
            if (sub == null && (logs == null || logs.isEmpty())) {
                continue;
            }
            if (!"EXTERNAL".equals(task.getTaskType()) && sub == null) {
                continue;
            }
            String json = sub != null ? sub.getContentJson() : "{}";
            if ("EXTERNAL".equals(task.getTaskType())) {
                json = restoreProgressFromLogs(json, logs);
            }
            Map<String, Object> view = new HashMap<String, Object>();
            view.put("contentJson", json);
            if (sub != null) {
                view.put("status", sub.getStatus());
                view.put("score", sub.getScore());
            }
            task.setStudentSubmission(view);
        }
    }

    private String restoreProgressFromLogs(String contentJson, List<LearnSubmissionLog> logs) {
        ObjectNode node;
        try {
            JsonNode parsed = objectMapper.readTree(StringUtils.hasText(contentJson) ? contentJson : "{}");
            node = parsed instanceof ObjectNode ? (ObjectNode) parsed : objectMapper.createObjectNode();
        } catch (Exception ex) {
            node = objectMapper.createObjectNode();
        }
        LinkedHashSet<Integer> done = new LinkedHashSet<Integer>();
        collectIndexes(done, node.get("completedActivities"));
        boolean quiz = hasQuizMarker(node);
        boolean evaluation = hasEvalMarker(node);
        if (logs != null) {
            for (LearnSubmissionLog log : logs) {
                Integer idx = log.getActivityIndex();
                if (idx != null && idx > 0 && idx < 10) {
                    done.add(idx);
                }
                if (idx != null && idx == 99 || "quiz".equals(log.getActivityKey())) {
                    quiz = true;
                }
                if (idx != null && idx == 98 || "evaluation".equals(log.getActivityKey())) {
                    evaluation = true;
                }
                if (!StringUtils.hasText(log.getContentJson())) {
                    continue;
                }
                try {
                    JsonNode body = objectMapper.readTree(log.getContentJson());
                    collectIndexes(done, body.get("completedActivities"));
                    if (body.path("stepCompleted").asBoolean(false)) {
                        String type = body.path("type").asText("");
                        int act = body.path("activityIndex").asInt(0);
                        if (act > 0 && act < 10 && !"quiz".equals(type) && !"evaluation".equals(type)
                                && !"progress".equals(type) && !"handin".equals(type)) {
                            done.add(act);
                        }
                    }
                    if (hasQuizMarker(body)) {
                        quiz = true;
                        if (!node.has("quizScore") && body.has("quizScore")) {
                            node.set("quizScore", body.get("quizScore"));
                        }
                        if (!node.has("correctCount") && body.has("correctCount")) {
                            node.set("correctCount", body.get("correctCount"));
                        }
                        if (!node.has("quizAnswers") && body.has("quizAnswers")) {
                            node.set("quizAnswers", body.get("quizAnswers"));
                        }
                        if (!node.has("dimensionScores") && body.has("dimensionScores")) {
                            node.set("dimensionScores", body.get("dimensionScores"));
                        }
                    }
                    if (hasEvalMarker(body)) {
                        evaluation = true;
                        if (!node.has("evaluationAnswers") && body.has("evaluationAnswers")) {
                            node.set("evaluationAnswers", body.get("evaluationAnswers"));
                        }
                    }
                    if (body.path("handedIn").asBoolean(false)) {
                        node.put("handedIn", true);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (quiz) {
            done.add(99);
            node.put("quizSubmitted", true);
        }
        if (evaluation) {
            done.add(98);
            node.put("evaluationSubmitted", true);
        }
        ArrayNode arr = objectMapper.createArrayNode();
        for (Integer idx : done) {
            arr.add(idx);
        }
        node.set("completedActivities", arr);
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return contentJson == null ? "{}" : contentJson;
        }
    }

    private void collectIndexes(Set<Integer> done, JsonNode array) {
        if (array == null || !array.isArray()) {
            return;
        }
        for (JsonNode item : array) {
            int idx = item.asInt(0);
            if ((idx > 0 && idx < 10) || idx == 98 || idx == 99) {
                done.add(idx);
            }
        }
    }

    private boolean hasQuizMarker(JsonNode node) {
        if (node == null) {
            return false;
        }
        return node.has("quizScore") || node.has("dimensionScores") || "quiz".equals(node.path("type").asText())
                || node.path("quizSubmitted").asBoolean(false);
    }

    private boolean hasEvalMarker(JsonNode node) {
        if (node == null) {
            return false;
        }
        if (node.path("evaluationSubmitted").asBoolean(false)) {
            return true;
        }
        if ("evaluation".equals(node.path("type").asText())) {
            return true;
        }
        return node.has("evaluationAnswers") && node.get("evaluationAnswers").size() > 0;
    }
}
