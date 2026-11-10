package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.entity.*;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师端智能评价：过程档案 + 综合等级。
 * 数据复用 learn_progress / learn_submission / log / hint，不改学生端。
 */
@Service
public class EvaluationService {

    private static final String[] QUIZ_DIMENSIONS = {"知识掌握", "能力提升", "素养变化"};
    private static final String[] EVAL_ASPECTS = {"学习兴趣", "学习难度", "学习信心"};
    private static final int POINTS_PER_QUIZ_DIMENSION = 2;
    private static final int QUIZ_TOTAL_POINTS = 6;

    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final CourseGradeRepository gradeRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseTaskRepository taskRepository;
    private final LearnProgressRepository progressRepository;
    private final LearnSubmissionRepository submissionRepository;
    private final LearnSubmissionLogRepository submissionLogRepository;
    private final LearnHintLogRepository hintLogRepository;
    private final LearnPretestG6Repository pretestG6Repository;
    private final LearnPretestG4Repository pretestG4Repository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EvaluationService(SysUserRepository userRepository,
                             SysClassRepository classRepository,
                             CourseGradeRepository gradeRepository,
                             CourseUnitRepository unitRepository,
                             CourseLessonRepository lessonRepository,
                             CourseTaskRepository taskRepository,
                             LearnProgressRepository progressRepository,
                             LearnSubmissionRepository submissionRepository,
                             LearnSubmissionLogRepository submissionLogRepository,
                             LearnHintLogRepository hintLogRepository,
                             LearnPretestG6Repository pretestG6Repository,
                             LearnPretestG4Repository pretestG4Repository,
                             DashboardService dashboardService) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.gradeRepository = gradeRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
        this.taskRepository = taskRepository;
        this.progressRepository = progressRepository;
        this.submissionRepository = submissionRepository;
        this.submissionLogRepository = submissionLogRepository;
        this.hintLogRepository = hintLogRepository;
        this.pretestG6Repository = pretestG6Repository;
        this.pretestG4Repository = pretestG4Repository;
        this.dashboardService = dashboardService;
    }

    public void assertOwnsClass(Long teacherId, Long classId) {
        dashboardService.checkTeacherOwnsClass(teacherId, classId);
    }

    /** 班级花名册 + 所选教材册次下的综合等级摘要 */
    public Map<String, Object> classRoster(Long classId, Long gradeId) {
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        CourseGrade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new BusinessException("教材册次不存在"));
        List<LessonRef> lessons = listLessonsOfGrade(gradeId);
        List<SysUser> students = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1);
        students.sort(Comparator.comparing(u -> u.getRealName() == null ? "" : u.getRealName()));

        Map<Long, LearnProgress> progressIndex = indexProgress(students, lessons);
        Map<Long, LearnSubmission> submissionIndex = indexSubmissions(students, lessons);
        Map<Long, Integer> hintIndex = indexHints(students, lessons);

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        int excellent = 0, good = 0, pass = 0, weak = 0, noData = 0;
        for (SysUser student : students) {
            Map<String, Object> summary = summarizeStudent(
                    student, lessons, progressIndex, submissionIndex, hintIndex);
            rows.add(summary);
            String level = String.valueOf(summary.get("levelCode"));
            if ("EXCELLENT".equals(level)) {
                excellent++;
            } else if ("GOOD".equals(level)) {
                good++;
            } else if ("PASS".equals(level)) {
                pass++;
            } else if ("WEAK".equals(level)) {
                weak++;
            } else {
                noData++;
            }
        }

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("classId", classId);
        out.put("className", cls.getName());
        out.put("gradeId", gradeId);
        out.put("gradeName", grade.getName());
        out.put("lessonCount", lessons.size());
        out.put("studentCount", students.size());
        out.put("levelCounts", levelCounts(excellent, good, pass, weak, noData));
        out.put("students", rows);
        out.put("rubric", rubricText());
        return out;
    }

    /** 学生跨课档案 */
    public Map<String, Object> studentProfile(Long classId, Long studentId, Long gradeId) {
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        SysUser student = requireClassStudent(classId, studentId);
        CourseGrade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new BusinessException("教材册次不存在"));
        List<LessonRef> lessons = listLessonsOfGrade(gradeId);

        Map<Long, LearnProgress> progressIndex = indexProgress(
                Collections.singletonList(student), lessons);
        Map<Long, LearnSubmission> submissionIndex = indexSubmissions(
                Collections.singletonList(student), lessons);
        Map<Long, Integer> hintIndex = indexHints(
                Collections.singletonList(student), lessons);

        Map<String, Object> summary = summarizeStudent(
                student, lessons, progressIndex, submissionIndex, hintIndex);

        List<Map<String, Object>> lessonCards = new ArrayList<Map<String, Object>>();
        for (LessonRef lesson : lessons) {
            LearnProgress progress = progressIndex.get(key(studentId, lesson.lessonId));
            LearnSubmission sub = submissionIndex.get(key(studentId, lesson.lessonId));
            int hints = hintIndex.getOrDefault(key(studentId, lesson.lessonId), 0);
            lessonCards.add(buildLessonCard(lesson, progress, sub, hints));
        }

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("classId", classId);
        out.put("className", cls.getName());
        out.put("gradeId", gradeId);
        out.put("gradeName", grade.getName());
        out.put("student", studentBrief(student));
        out.put("summary", summary);
        out.put("baseline", buildBaseline(student));
        out.put("lessons", lessonCards);
        out.put("rubric", rubricText());
        return out;
    }

    /** 单课过程档案明细 */
    public Map<String, Object> lessonDetail(Long classId, Long studentId, Long lessonId) {
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        SysUser student = requireClassStudent(classId, studentId);
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        CourseUnit unit = unitRepository.findById(lesson.getUnitId())
                .orElseThrow(() -> new BusinessException("单元不存在"));
        CourseGrade grade = gradeRepository.findById(unit.getGradeId())
                .orElseThrow(() -> new BusinessException("教材册次不存在"));

        LearnProgress progress = progressRepository.findByStudentIdAndLessonId(studentId, lessonId).orElse(null);
        CourseTask workspace = findWorkspaceTask(lessonId);
        LearnSubmission sub = null;
        if (workspace != null) {
            sub = submissionRepository.findByTaskIdAndStudentId(workspace.getId(), studentId).orElse(null);
        }
        List<LearnHintLog> hints = hintLogRepository.findByStudentIdAndLessonId(studentId, lessonId);
        List<LearnSubmissionLog> logs = submissionLogRepository
                .findByStudentIdAndLessonIdOrderByCreatedAtDesc(studentId, lessonId);

        JsonNode cfg = workspace == null ? null : parseJson(workspace.getConfigJson());
        List<Map<String, Object>> activityDefs = buildActivityDefinitions(cfg);
        JsonNode content = parseContentJson(sub);

        Map<String, Object> card = buildLessonCard(
                new LessonRef(lessonId, lesson.getTitle(), unit.getName(), unit.getSortOrder(), lesson.getSortOrder()),
                progress, sub, hints.size());

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("classId", classId);
        out.put("className", cls.getName());
        out.put("gradeId", grade.getId());
        out.put("gradeName", grade.getName());
        out.put("unitName", unit.getName());
        out.put("student", studentBrief(student));
        out.put("lesson", card);
        out.put("progress", progressMap(progress));
        out.put("activities", buildActivityStatus(activityDefs, content, logs));
        out.put("quiz", buildQuizDetail(content, sub));
        out.put("evaluation", buildEvaluationDetail(content, sub));
        out.put("hints", buildHintSummary(hints));
        out.put("trajectory", buildTrajectory(logs));
        out.put("grade", gradeOfLesson(progress, sub, hints.size(), activityDefs.size()));
        out.put("rubric", rubricText());
        return out;
    }

    // ---------- 汇总与等级 ----------

    private Map<String, Object> summarizeStudent(SysUser student,
                                                List<LessonRef> lessons,
                                                Map<Long, LearnProgress> progressIndex,
                                                Map<Long, LearnSubmission> submissionIndex,
                                                Map<Long, Integer> hintIndex) {
        int started = 0;
        int completed = 0;
        int quizCount = 0;
        int quizSum = 0;
        int processSum = 0;
        int processCount = 0;
        int resultSum = 0;
        int resultCount = 0;
        int hintTotal = 0;
        int studySeconds = 0;

        for (LessonRef lesson : lessons) {
            Long k = key(student.getId(), lesson.lessonId);
            LearnProgress progress = progressIndex.get(k);
            LearnSubmission sub = submissionIndex.get(k);
            int hints = hintIndex.getOrDefault(k, 0);
            hintTotal += hints;
            if (progress != null) {
                if (progress.getStudySeconds() != null) {
                    studySeconds += progress.getStudySeconds();
                }
                if (!"NOT_STARTED".equals(nullTo(progress.getStatus(), "NOT_STARTED"))) {
                    started++;
                }
                if ("COMPLETED".equals(progress.getStatus())
                        || (progress.getProgressPercent() != null && progress.getProgressPercent() >= 100)) {
                    completed++;
                }
            } else if (sub != null) {
                started++;
            }

            Map<String, Object> g = gradeOfLesson(progress, sub, hints, estimateActivityCount(sub));
            if (Boolean.TRUE.equals(g.get("hasData"))) {
                processSum += (Integer) g.get("processScore");
                processCount++;
                if (g.get("resultScore") != null) {
                    resultSum += (Integer) g.get("resultScore");
                    resultCount++;
                }
            }
            Integer quizPercent = (Integer) g.get("quizPercent");
            if (quizPercent != null && quizPercent >= 0) {
                quizCount++;
                quizSum += quizPercent;
            }
        }

        Integer avgProcess = processCount > 0 ? Math.round(processSum * 1f / processCount) : null;
        Integer avgResult = resultCount > 0 ? Math.round(resultSum * 1f / resultCount) : null;
        Integer composite = null;
        if (avgProcess != null && avgResult != null) {
            composite = Math.round(avgProcess * 0.6f + avgResult * 0.4f);
        } else if (avgProcess != null) {
            composite = avgProcess;
        } else if (avgResult != null) {
            composite = avgResult;
        }
        String levelCode = levelCodeOf(composite, processCount + resultCount > 0);
        String levelLabel = levelLabelOf(levelCode);

        Map<String, Object> row = studentBrief(student);
        row.put("startedLessons", started);
        row.put("completedLessons", completed);
        row.put("lessonCount", lessons.size());
        row.put("completionRate", lessons.isEmpty() ? 0
                : Math.round(completed * 1000.0 / lessons.size()) / 10.0);
        row.put("quizCount", quizCount);
        row.put("avgQuizPercent", quizCount > 0 ? Math.round(quizSum * 1f / quizCount) : null);
        row.put("processScore", avgProcess);
        row.put("resultScore", avgResult);
        row.put("compositeScore", composite);
        row.put("levelCode", levelCode);
        row.put("levelLabel", levelLabel);
        row.put("hintTotal", hintTotal);
        row.put("studySeconds", studySeconds);
        row.put("hasData", processCount + resultCount > 0);
        return row;
    }

    private Map<String, Object> gradeOfLesson(LearnProgress progress,
                                              LearnSubmission sub,
                                              int hintCount,
                                              int activityTotal) {
        JsonNode content = parseContentJson(sub);
        boolean hasProgress = progress != null
                && !"NOT_STARTED".equals(nullTo(progress.getStatus(), "NOT_STARTED"));
        boolean hasSub = content != null;
        boolean hasData = hasProgress || hasSub;

        int completionPart = 0;
        if (progress != null) {
            if ("COMPLETED".equals(progress.getStatus())) {
                completionPart = 100;
            } else if (progress.getProgressPercent() != null) {
                completionPart = clamp(progress.getProgressPercent(), 0, 100);
            } else if (hasProgress) {
                completionPart = 40;
            }
        } else if (hasSub) {
            completionPart = 50;
        }

        int inquiryPart = 0;
        int doneActs = completedActivityCount(content);
        int actTotal = activityTotal > 0 ? activityTotal : Math.max(doneActs, 0);
        if (actTotal > 0) {
            inquiryPart = Math.round(doneActs * 100f / actTotal);
        } else if (hasSub) {
            inquiryPart = 40;
        }

        int engagePart = 50;
        int seconds = 0;
        if (progress != null && progress.getStudySeconds() != null) {
            seconds = progress.getStudySeconds();
        } else if (sub != null && sub.getStudySeconds() != null) {
            seconds = sub.getStudySeconds();
        }
        if (seconds >= 1200) {
            engagePart = 100;
        } else if (seconds >= 600) {
            engagePart = 80;
        } else if (seconds >= 180) {
            engagePart = 60;
        } else if (seconds > 0) {
            engagePart = 40;
        } else if (!hasData) {
            engagePart = 0;
        }
        if (hintCount >= 6) {
            engagePart = Math.max(0, engagePart - 25);
        } else if (hintCount >= 3) {
            engagePart = Math.max(0, engagePart - 12);
        }

        int attitudePart = 50;
        boolean hasEval = hasEvaluation(content);
        if (hasEval) {
            attitudePart = evaluationAttitudeScore(content);
        } else if (!hasData) {
            attitudePart = 0;
        }

        int processScore = Math.round(completionPart * 0.4f + inquiryPart * 0.3f
                + engagePart * 0.15f + attitudePart * 0.15f);

        Integer quizPercent = null;
        Integer resultScore = null;
        if (hasQuiz(content)) {
            quizPercent = quizPercentOf(content);
            resultScore = quizPercent;
        }

        Integer composite;
        if (resultScore != null) {
            composite = Math.round(processScore * 0.6f + resultScore * 0.4f);
        } else if (hasData) {
            composite = processScore;
        } else {
            composite = null;
        }
        String levelCode = levelCodeOf(composite, hasData);

        Map<String, Object> g = new LinkedHashMap<String, Object>();
        g.put("hasData", hasData);
        g.put("processScore", hasData ? processScore : null);
        g.put("resultScore", resultScore);
        g.put("compositeScore", composite);
        g.put("quizPercent", quizPercent);
        g.put("levelCode", levelCode);
        g.put("levelLabel", levelLabelOf(levelCode));
        g.put("parts", partsMap(completionPart, inquiryPart, engagePart, attitudePart, hasEval));
        return g;
    }

    private Map<String, Object> buildLessonCard(LessonRef lesson,
                                                LearnProgress progress,
                                                LearnSubmission sub,
                                                int hints) {
        JsonNode content = parseContentJson(sub);
        Map<String, Object> grade = gradeOfLesson(progress, sub, hints, estimateActivityCount(sub));
        Map<String, Object> card = new LinkedHashMap<String, Object>();
        card.put("lessonId", lesson.lessonId);
        card.put("lessonTitle", lesson.title);
        card.put("unitName", lesson.unitName);
        card.put("status", progress == null ? "NOT_STARTED" : progress.getStatus());
        card.put("progressPercent", progress == null ? 0 : progress.getProgressPercent());
        card.put("studySeconds", progress == null ? 0
                : (progress.getStudySeconds() == null ? 0 : progress.getStudySeconds()));
        card.put("lastVisitAt", progress == null ? null : progress.getLastVisitAt());
        card.put("completedAt", progress == null ? null : progress.getCompletedAt());
        card.put("inquiryDone", completedActivityCount(content));
        card.put("quizSubmitted", hasQuiz(content));
        card.put("quizPercent", grade.get("quizPercent"));
        card.put("evaluationSubmitted", hasEvaluation(content));
        card.put("hintCount", hints);
        card.put("processScore", grade.get("processScore"));
        card.put("resultScore", grade.get("resultScore"));
        card.put("compositeScore", grade.get("compositeScore"));
        card.put("levelCode", grade.get("levelCode"));
        card.put("levelLabel", grade.get("levelLabel"));
        card.put("hasData", grade.get("hasData"));
        return card;
    }

    // ---------- 单课明细块 ----------

    private Map<String, Object> progressMap(LearnProgress progress) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        if (progress == null) {
            m.put("status", "NOT_STARTED");
            m.put("progressPercent", 0);
            m.put("studySeconds", 0);
            return m;
        }
        m.put("status", progress.getStatus());
        m.put("progressPercent", progress.getProgressPercent());
        m.put("studySeconds", progress.getStudySeconds());
        m.put("lastVisitAt", progress.getLastVisitAt());
        m.put("completedAt", progress.getCompletedAt());
        return m;
    }

    private List<Map<String, Object>> buildActivityStatus(List<Map<String, Object>> defs,
                                                          JsonNode content,
                                                          List<LearnSubmissionLog> logs) {
        Set<Integer> doneIdx = doneIndexes(content);
        Map<Integer, LocalDateTime> firstAt = new HashMap<Integer, LocalDateTime>();
        for (LearnSubmissionLog log : logs) {
            if (log.getActivityIndex() == null) {
                continue;
            }
            LocalDateTime at = log.getCreatedAt();
            LocalDateTime old = firstAt.get(log.getActivityIndex());
            if (old == null || (at != null && at.isBefore(old))) {
                firstAt.put(log.getActivityIndex(), at);
            }
        }
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (defs.isEmpty() && !doneIdx.isEmpty()) {
            for (Integer idx : doneIdx) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("index", idx);
                row.put("title", "探究" + idx);
                row.put("type", "INTERACTIVE");
                row.put("done", true);
                row.put("firstAt", firstAt.get(idx));
                list.add(row);
            }
            return list;
        }
        for (Map<String, Object> def : defs) {
            int index = (Integer) def.get("index");
            String type = String.valueOf(def.get("type"));
            boolean done;
            if ("QUIZ".equals(type)) {
                done = hasQuiz(content);
            } else if ("EVALUATION".equals(type)) {
                done = hasEvaluation(content);
            } else {
                done = doneIdx.contains(index) || maxActivityIndex(content) >= index;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("index", index);
            row.put("title", def.get("title"));
            row.put("type", type);
            row.put("done", done);
            row.put("firstAt", firstAt.get(index));
            list.add(row);
        }
        return list;
    }

    private Map<String, Object> buildQuizDetail(JsonNode content, LearnSubmission sub) {
        Map<String, Object> quiz = new LinkedHashMap<String, Object>();
        boolean submitted = hasQuiz(content);
        quiz.put("submitted", submitted);
        if (!submitted) {
            quiz.put("dimensions", emptyQuizDimensions());
            return quiz;
        }
        int[] points = parseQuizDimensionPoints(content);
        List<Map<String, Object>> dims = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < QUIZ_DIMENSIONS.length; i++) {
            Map<String, Object> d = new LinkedHashMap<String, Object>();
            d.put("name", QUIZ_DIMENSIONS[i]);
            d.put("score", points[i]);
            d.put("maxScore", POINTS_PER_QUIZ_DIMENSION);
            d.put("percent", Math.round(points[i] * 100f / POINTS_PER_QUIZ_DIMENSION));
            dims.add(d);
        }
        quiz.put("dimensions", dims);
        quiz.put("percent", quizPercentOf(content));
        quiz.put("correctCount", content.path("correctCount").asInt(-1));
        quiz.put("totalQuestions", content.path("totalQuestions").asInt(-1));
        quiz.put("pointScore", points[0] + points[1] + points[2]);
        quiz.put("maxPointScore", QUIZ_TOTAL_POINTS);
        if (sub != null) {
            quiz.put("submittedAt", sub.getUpdatedAt() != null ? sub.getUpdatedAt() : sub.getSubmittedAt());
        }
        return quiz;
    }

    private Map<String, Object> buildEvaluationDetail(JsonNode content, LearnSubmission sub) {
        Map<String, Object> eval = new LinkedHashMap<String, Object>();
        boolean submitted = hasEvaluation(content);
        eval.put("submitted", submitted);
        List<Map<String, Object>> answers = new ArrayList<Map<String, Object>>();
        if (submitted) {
            JsonNode arr = content.path("evaluationAnswers");
            if (!arr.isArray() || arr.size() == 0) {
                arr = content.path("answers");
            }
            if (arr.isArray()) {
                for (JsonNode ans : arr) {
                    String aspect = normalizeEvalAspect(ans.path("aspect").asText(""));
                    if (!StringUtils.hasText(aspect)) {
                        continue;
                    }
                    Map<String, Object> row = new LinkedHashMap<String, Object>();
                    row.put("aspect", aspect);
                    row.put("selected", ans.path("selected").asInt(-1));
                    row.put("label", ans.path("label").asText(""));
                    row.put("concern", isEvalConcern(aspect, ans.path("selected").asInt(-1),
                            ans.path("label").asText("")));
                    answers.add(row);
                }
            }
        }
        eval.put("answers", answers);
        eval.put("attitudeScore", submitted ? evaluationAttitudeScore(content) : null);
        if (sub != null && submitted) {
            eval.put("submittedAt", sub.getUpdatedAt() != null ? sub.getUpdatedAt() : sub.getSubmittedAt());
        }
        return eval;
    }

    private Map<String, Object> buildHintSummary(List<LearnHintLog> hints) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("total", hints.size());
        Map<Integer, Integer> byAct = new TreeMap<Integer, Integer>();
        for (LearnHintLog h : hints) {
            int idx = h.getActivityIndex() == null ? -1 : h.getActivityIndex();
            byAct.put(idx, byAct.getOrDefault(idx, 0) + 1);
        }
        List<Map<String, Object>> breakdown = new ArrayList<Map<String, Object>>();
        for (Map.Entry<Integer, Integer> e : byAct.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("activityIndex", e.getKey());
            row.put("count", e.getValue());
            breakdown.add(row);
        }
        m.put("byActivity", breakdown);
        return m;
    }

    private List<Map<String, Object>> buildTrajectory(List<LearnSubmissionLog> logs) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        // 日志已按时间倒序；展示时改为正序更易读
        List<LearnSubmissionLog> ordered = new ArrayList<LearnSubmissionLog>(logs);
        Collections.reverse(ordered);
        for (LearnSubmissionLog log : ordered) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("activityKey", log.getActivityKey());
            row.put("activityIndex", log.getActivityIndex());
            row.put("score", log.getScore());
            row.put("studySeconds", log.getStudySeconds());
            row.put("createdAt", log.getCreatedAt());
            row.put("label", trajectoryLabel(log));
            list.add(row);
        }
        return list;
    }

    private String trajectoryLabel(LearnSubmissionLog log) {
        String key = log.getActivityKey() == null ? "" : log.getActivityKey();
        if (key.startsWith("quiz") || Integer.valueOf(99).equals(log.getActivityIndex())) {
            return "提交小测";
        }
        if (key.startsWith("evaluation") || Integer.valueOf(98).equals(log.getActivityIndex())) {
            return "提交学习评价";
        }
        if (key.startsWith("reading")) {
            return "完成阅读";
        }
        if (log.getActivityIndex() != null && log.getActivityIndex() > 0) {
            return "完成探究" + log.getActivityIndex();
        }
        if (StringUtils.hasText(key)) {
            return key;
        }
        return "学习提交";
    }

    private Map<String, Object> buildBaseline(SysUser student) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        pretestG6Repository.findByStudentId(student.getId()).ifPresent(p -> {
            m.put("g6Total", p.getTotalScore());
            m.put("g6Level", p.getLevelCode());
            m.put("g6At", p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getSubmittedAt());
        });
        pretestG4Repository.findByStudentId(student.getId()).ifPresent(p -> {
            m.put("g4Total", p.getTotalScore());
            m.put("g4At", p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getSubmittedAt());
        });
        if (StringUtils.hasText(student.getTierOverride())) {
            m.put("tierOverride", student.getTierOverride());
        }
        return m;
    }

    // ---------- 索引与课程列表 ----------

    private List<LessonRef> listLessonsOfGrade(Long gradeId) {
        List<LessonRef> list = new ArrayList<LessonRef>();
        for (CourseUnit unit : unitRepository.findByGradeIdOrderBySortOrderAsc(gradeId)) {
            for (CourseLesson lesson : lessonRepository.findByUnitIdOrderBySortOrderAsc(unit.getId())) {
                list.add(new LessonRef(lesson.getId(), lesson.getTitle(), unit.getName(),
                        unit.getSortOrder(), lesson.getSortOrder()));
            }
        }
        return list;
    }

    private Map<Long, LearnProgress> indexProgress(List<SysUser> students, List<LessonRef> lessons) {
        Map<Long, LearnProgress> map = new HashMap<Long, LearnProgress>();
        Set<Long> lessonIds = lessons.stream().map(l -> l.lessonId).collect(Collectors.toSet());
        for (SysUser student : students) {
            for (LearnProgress p : progressRepository.findByStudentId(student.getId())) {
                if (lessonIds.contains(p.getLessonId())) {
                    map.put(key(student.getId(), p.getLessonId()), p);
                }
            }
        }
        return map;
    }

    private Map<Long, LearnSubmission> indexSubmissions(List<SysUser> students, List<LessonRef> lessons) {
        Map<Long, Long> lessonTask = new HashMap<Long, Long>();
        Map<Long, Long> taskLesson = new HashMap<Long, Long>();
        for (LessonRef lesson : lessons) {
            CourseTask task = findWorkspaceTask(lesson.lessonId);
            if (task != null) {
                lessonTask.put(lesson.lessonId, task.getId());
                taskLesson.put(task.getId(), lesson.lessonId);
            }
        }
        Map<Long, LearnSubmission> map = new HashMap<Long, LearnSubmission>();
        if (taskLesson.isEmpty() || students.isEmpty()) {
            return map;
        }
        List<Long> taskIds = new ArrayList<Long>(taskLesson.keySet());
        Set<Long> studentIds = students.stream().map(SysUser::getId).collect(Collectors.toSet());
        for (LearnSubmission sub : submissionRepository.findByTaskIdIn(taskIds)) {
            if (!studentIds.contains(sub.getStudentId())) {
                continue;
            }
            Long lessonId = taskLesson.get(sub.getTaskId());
            if (lessonId != null) {
                map.put(key(sub.getStudentId(), lessonId), sub);
            }
        }
        return map;
    }

    private Map<Long, Integer> indexHints(List<SysUser> students, List<LessonRef> lessons) {
        Map<Long, Integer> map = new HashMap<Long, Integer>();
        Set<Long> lessonIds = lessons.stream().map(l -> l.lessonId).collect(Collectors.toSet());
        for (SysUser student : students) {
            for (LearnHintLog h : hintLogRepository.findByStudentId(student.getId())) {
                if (!lessonIds.contains(h.getLessonId())) {
                    continue;
                }
                Long k = key(student.getId(), h.getLessonId());
                map.put(k, map.getOrDefault(k, 0) + 1);
            }
        }
        return map;
    }

    private CourseTask findWorkspaceTask(Long lessonId) {
        List<CourseTask> tasks = taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
        CourseTask fallback = null;
        for (CourseTask task : tasks) {
            if (!"EXTERNAL".equals(task.getTaskType())) {
                continue;
            }
            if (fallback == null) {
                fallback = task;
            }
            if (!StringUtils.hasText(task.getConfigJson())) {
                continue;
            }
            try {
                JsonNode cfg = objectMapper.readTree(task.getConfigJson());
                if ("student_workspace".equals(cfg.path("layout").asText())) {
                    return task;
                }
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private SysUser requireClassStudent(Long classId, Long studentId) {
        SysUser student = userRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("学生不存在"));
        if (!"STUDENT".equals(student.getRole()) || student.getStatus() == null || student.getStatus() != 1) {
            throw new BusinessException("不是有效的学生账号");
        }
        if (student.getClassId() == null || !student.getClassId().equals(classId)) {
            // 兼容班级对照表：同班名单以 class_id 为准；不符则拒绝
            throw new BusinessException("该学生不在本班");
        }
        return student;
    }

    // ---------- JSON / 评分辅助 ----------

    private List<Map<String, Object>> buildActivityDefinitions(JsonNode workspaceConfig) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (workspaceConfig == null) {
            return list;
        }
        JsonNode activities = workspaceConfig.path("activities");
        if (!activities.isArray()) {
            return list;
        }
        int i = 0;
        for (JsonNode act : activities) {
            i++;
            int index = act.path("index").asInt(i);
            String type = act.path("type").asText("INTERACTIVE");
            if ("quiz".equalsIgnoreCase(type)) {
                type = "QUIZ";
            } else if ("evaluation".equalsIgnoreCase(type)) {
                type = "EVALUATION";
            } else {
                type = "INTERACTIVE";
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("index", index);
            row.put("title", act.path("title").asText("探究" + index));
            row.put("type", type);
            list.add(row);
        }
        JsonNode quiz = workspaceConfig.path("quiz");
        if (quiz.isObject() && quiz.size() > 0) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("index", 99);
            row.put("title", "课堂小测");
            row.put("type", "QUIZ");
            list.add(row);
        }
        JsonNode evaluation = workspaceConfig.path("evaluationForm");
        if (!evaluation.isObject()) {
            evaluation = workspaceConfig.path("evaluation");
        }
        if (evaluation.isObject() && evaluation.size() > 0) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("index", 98);
            row.put("title", "学习评价");
            row.put("type", "EVALUATION");
            list.add(row);
        }
        return list;
    }

    private int estimateActivityCount(LearnSubmission sub) {
        if (sub == null) {
            return 0;
        }
        // 无配置时用已完成探究数作分母下限，避免 100% 虚高
        int done = completedActivityCount(parseContentJson(sub));
        return Math.max(done, 3);
    }

    private int completedActivityCount(JsonNode content) {
        if (content == null) {
            return 0;
        }
        JsonNode arr = content.path("completedActivities");
        if (arr.isArray() && arr.size() > 0) {
            return arr.size();
        }
        return Math.max(0, maxActivityIndex(content));
    }

    private Set<Integer> doneIndexes(JsonNode content) {
        Set<Integer> set = new HashSet<Integer>();
        if (content == null) {
            return set;
        }
        JsonNode arr = content.path("completedActivities");
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                if (n.isInt() || n.isLong()) {
                    set.add(n.asInt());
                } else if (n.isTextual()) {
                    try {
                        set.add(Integer.parseInt(n.asText().replaceAll("[^0-9]", "")));
                    } catch (Exception ignored) {
                    }
                } else if (n.has("index")) {
                    set.add(n.path("index").asInt());
                }
            }
        }
        return set;
    }

    private int maxActivityIndex(JsonNode content) {
        if (content == null) {
            return 0;
        }
        if (content.has("activityIndex")) {
            return content.path("activityIndex").asInt(0);
        }
        if (content.has("stepCompleted")) {
            return content.path("stepCompleted").asInt(0);
        }
        int max = 0;
        for (Integer idx : doneIndexes(content)) {
            max = Math.max(max, idx);
        }
        return max;
    }

    private boolean hasQuiz(JsonNode content) {
        if (content == null) {
            return false;
        }
        if (content.has("dimensionScores") || content.has("quizScore")
                || content.path("quizSubmitted").asBoolean(false)) {
            return true;
        }
        if ("quiz".equals(content.path("type").asText())) {
            return true;
        }
        JsonNode answers = content.path("answers");
        if (answers.isArray()) {
            for (JsonNode ans : answers) {
                if (ans.has("dimension") || ans.has("isCorrect")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasEvaluation(JsonNode content) {
        if (content == null) {
            return false;
        }
        if (content.path("evaluationSubmitted").asBoolean(false)) {
            return true;
        }
        if (content.has("evaluationAnswers") && content.get("evaluationAnswers").size() > 0) {
            return true;
        }
        return "evaluation".equals(content.path("type").asText())
                && content.has("answers")
                && content.get("answers").size() > 0;
    }

    private int quizPercentOf(JsonNode node) {
        int totalQ = node.path("totalQuestions").asInt(0);
        int correct = node.path("correctCount").asInt(-1);
        if (totalQ > 0 && correct >= 0) {
            return Math.min(100, Math.round(correct * 100f / totalQ));
        }
        if (node.has("quizScore")) {
            int raw = node.get("quizScore").asInt(0);
            if (raw > QUIZ_TOTAL_POINTS) {
                return Math.min(100, raw);
            }
            return Math.round(raw * 100f / QUIZ_TOTAL_POINTS);
        }
        int[] dims = parseQuizDimensionPoints(node);
        int sum = dims[0] + dims[1] + dims[2];
        return Math.round(sum * 100f / QUIZ_TOTAL_POINTS);
    }

    private int[] parseQuizDimensionPoints(JsonNode node) {
        int[] points = new int[]{0, 0, 0};
        JsonNode dimScores = node.path("dimensionScores");
        if (dimScores.isObject() && dimScores.size() > 0) {
            for (int i = 0; i < QUIZ_DIMENSIONS.length; i++) {
                JsonNode d = dimScores.path(QUIZ_DIMENSIONS[i]);
                points[i] = Math.min(d.path("correct").asInt(0), POINTS_PER_QUIZ_DIMENSION);
            }
            return points;
        }
        JsonNode answers = node.path("answers");
        if (answers.isArray()) {
            Map<String, int[]> tmp = new HashMap<String, int[]>();
            for (JsonNode ans : answers) {
                String dim = ans.path("dimension").asText("");
                if (dim.isEmpty()) {
                    continue;
                }
                tmp.computeIfAbsent(dim, k -> new int[]{0, 0});
                tmp.get(dim)[1]++;
                if (ans.path("isCorrect").asBoolean(false)) {
                    tmp.get(dim)[0]++;
                }
            }
            for (int i = 0; i < QUIZ_DIMENSIONS.length; i++) {
                int[] t = tmp.get(QUIZ_DIMENSIONS[i]);
                points[i] = t != null ? Math.min(t[0], POINTS_PER_QUIZ_DIMENSION) : 0;
            }
        }
        return points;
    }

    private int evaluationAttitudeScore(JsonNode content) {
        JsonNode arr = content.path("evaluationAnswers");
        if (!arr.isArray() || arr.size() == 0) {
            arr = content.path("answers");
        }
        if (!arr.isArray() || arr.size() == 0) {
            return 50;
        }
        int score = 0;
        int n = 0;
        for (JsonNode ans : arr) {
            String aspect = normalizeEvalAspect(ans.path("aspect").asText(""));
            int selected = ans.path("selected").asInt(-1);
            String label = ans.path("label").asText("");
            if (!StringUtils.hasText(aspect) || selected < 0) {
                continue;
            }
            n++;
            // selected 越小通常越积极（兴趣高/难度适中偏易/信心高）
            if (isEvalConcern(aspect, selected, label)) {
                score += 35;
            } else if (selected <= 1) {
                score += 95;
            } else if (selected == 2) {
                score += 70;
            } else {
                score += 45;
            }
        }
        return n == 0 ? 50 : Math.round(score * 1f / n);
    }

    private boolean isEvalConcern(String aspect, int selected, String label) {
        String text = label == null ? "" : label;
        if ("学习兴趣".equals(aspect) || "学习信心".equals(aspect)) {
            return selected >= 3 || text.contains("不太") || text.contains("没有") || text.contains("较低");
        }
        if ("学习难度".equals(aspect)) {
            return selected >= 3 || text.contains("很难") || text.contains("较难") || text.contains("太难");
        }
        return selected >= 3;
    }

    private String normalizeEvalAspect(String aspect) {
        if (!StringUtils.hasText(aspect)) {
            return "";
        }
        String a = aspect.trim();
        for (String known : EVAL_ASPECTS) {
            if (known.equals(a) || a.contains(known.substring(2))) {
                return known;
            }
        }
        if (a.contains("兴趣")) {
            return "学习兴趣";
        }
        if (a.contains("难度") || a.contains("难易")) {
            return "学习难度";
        }
        if (a.contains("信心") || a.contains("自信")) {
            return "学习信心";
        }
        return a;
    }

    private JsonNode parseContentJson(LearnSubmission sub) {
        if (sub == null) {
            return null;
        }
        return parseJson(sub.getContentJson());
    }

    private JsonNode parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> studentBrief(SysUser student) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("studentId", student.getId());
        row.put("realName", student.getRealName());
        row.put("username", student.getUsername());
        return row;
    }

    private Map<String, Object> levelCounts(int excellent, int good, int pass, int weak, int noData) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("excellent", excellent);
        m.put("good", good);
        m.put("pass", pass);
        m.put("weak", weak);
        m.put("noData", noData);
        return m;
    }

    private Map<String, Object> partsMap(int completion, int inquiry, int engage, int attitude, boolean hasEval) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("completion", completion);
        m.put("inquiry", inquiry);
        m.put("engagement", engage);
        m.put("attitude", attitude);
        m.put("hasEvaluation", hasEval);
        return m;
    }

    private List<Map<String, Object>> emptyQuizDimensions() {
        List<Map<String, Object>> dims = new ArrayList<Map<String, Object>>();
        for (String name : QUIZ_DIMENSIONS) {
            Map<String, Object> d = new LinkedHashMap<String, Object>();
            d.put("name", name);
            d.put("score", 0);
            d.put("maxScore", POINTS_PER_QUIZ_DIMENSION);
            d.put("percent", 0);
            dims.add(d);
        }
        return dims;
    }

    private String levelCodeOf(Integer score, boolean hasData) {
        if (!hasData || score == null) {
            return "NONE";
        }
        if (score >= 85) {
            return "EXCELLENT";
        }
        if (score >= 70) {
            return "GOOD";
        }
        if (score >= 60) {
            return "PASS";
        }
        return "WEAK";
    }

    private String levelLabelOf(String code) {
        if ("EXCELLENT".equals(code)) {
            return "优秀";
        }
        if ("GOOD".equals(code)) {
            return "良好";
        }
        if ("PASS".equals(code)) {
            return "合格";
        }
        if ("WEAK".equals(code)) {
            return "待提升";
        }
        return "暂无数据";
    }

    private Map<String, Object> rubricText() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("process", "过程分：完成度40% + 探究完成30% + 投入（时长/提示）15% + 自评态度15%");
        m.put("result", "结果分：课堂小测百分制；未交小测时综合分仅看过程分");
        m.put("composite", "综合分 = 过程分×60% + 结果分×40%");
        m.put("levels", "优秀≥85，良好≥70，合格≥60，待提升低于60");
        return m;
    }

    private Long key(Long studentId, Long lessonId) {
        return studentId * 1_000_000_000L + lessonId;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private String nullTo(String v, String d) {
        return v == null ? d : v;
    }

    private static final class LessonRef {
        private final Long lessonId;
        private final String title;
        private final String unitName;
        private final Integer unitSort;
        private final Integer lessonSort;

        private LessonRef(Long lessonId, String title, String unitName, Integer unitSort, Integer lessonSort) {
            this.lessonId = lessonId;
            this.title = title;
            this.unitName = unitName;
            this.unitSort = unitSort;
            this.lessonSort = lessonSort;
        }
    }
}
