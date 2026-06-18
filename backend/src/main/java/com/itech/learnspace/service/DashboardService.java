package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.entity.*;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final CourseGradeRepository gradeRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseTaskRepository taskRepository;
    private final LearnSubmissionRepository submissionRepository;
    private final LearnPointsRepository pointsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashboardService(SysUserRepository userRepository, SysClassRepository classRepository,
                            CourseGradeRepository gradeRepository, CourseUnitRepository unitRepository,
                            CourseLessonRepository lessonRepository, CourseTaskRepository taskRepository,
                            LearnSubmissionRepository submissionRepository,
                            LearnPointsRepository pointsRepository) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.gradeRepository = gradeRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
        this.taskRepository = taskRepository;
        this.submissionRepository = submissionRepository;
        this.pointsRepository = pointsRepository;
    }

    public List<SysClass> getTeacherClasses(Long teacherId) {
        return classRepository.findByTeacherId(teacherId);
    }

    /**
     * 构建班级学情看板数据（SSE 推送和 REST 查询共用）
     */
    public Map<String, Object> buildDashboardData(Long classId) {
        List<SysUser> students = userRepository.findByClassId(classId);
        List<Long> studentIds = students.stream().map(SysUser::getId).collect(Collectors.toList());

        // 获取所有任务
        List<CourseTask> allTasks = new ArrayList<CourseTask>();
        for (CourseGrade grade : gradeRepository.findAllByOrderBySortOrderAsc()) {
            for (CourseUnit unit : unitRepository.findByGradeIdOrderBySortOrderAsc(grade.getId())) {
                for (CourseLesson lesson : lessonRepository.findByUnitIdOrderBySortOrderAsc(unit.getId())) {
                    allTasks.addAll(taskRepository.findByLessonIdOrderBySortOrderAsc(lesson.getId()));
                }
            }
        }

        List<Long> taskIds = allTasks.stream().map(CourseTask::getId).collect(Collectors.toList());
        List<LearnSubmission> submissions = taskIds.isEmpty() ? Collections.emptyList()
                : submissionRepository.findByTaskIdIn(taskIds).stream()
                .filter(s -> studentIds.contains(s.getStudentId()))
                .collect(Collectors.toList());

        Set<String> submittedPairs = new HashSet<String>();
        for (LearnSubmission s : submissions) {
            submittedPairs.add(s.getStudentId() + "_" + s.getTaskId());
        }

        int totalTasks = allTasks.size();
        int totalSlots = students.size() * totalTasks;
        int submittedCount = submittedPairs.size();
        double completionRate = totalSlots > 0 ? (submittedCount * 100.0 / totalSlots) : 0;

        // 学生提交明细
        List<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
        for (SysUser student : students) {
            int studentSubmitted = 0;
            for (CourseTask task : allTasks) {
                if (submittedPairs.contains(student.getId() + "_" + task.getId())) {
                    studentSubmitted++;
                }
            }
            Map<String, Object> detail = new HashMap<String, Object>();
            detail.put("studentId", student.getId());
            detail.put("realName", student.getRealName());
            detail.put("submittedCount", studentSubmitted);
            detail.put("totalTasks", totalTasks);
            detail.put("completed", studentSubmitted >= totalTasks && totalTasks > 0);
            details.add(detail);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("classId", classId);
        data.put("totalStudents", students.size());
        data.put("totalTasks", totalTasks);
        data.put("submittedCount", submittedCount);
        data.put("unsubmittedCount", totalSlots - submittedCount);
        data.put("completionRate", Math.round(completionRate * 10) / 10.0);
        data.put("studentDetails", details);
        data.put("updateTime", new Date());
        return data;
    }

    /**
     * 全班完成矩阵：每个学生 × 每个任务的完成状态
     */
    public Map<String, Object> buildMatrix(Long classId) {
        List<SysUser> students = userRepository.findByClassId(classId);

        List<Map<String, Object>> taskList = new ArrayList<Map<String, Object>>();
        List<CourseTask> allTasks = new ArrayList<CourseTask>();
        for (CourseGrade grade : gradeRepository.findAllByOrderBySortOrderAsc()) {
            for (CourseUnit unit : unitRepository.findByGradeIdOrderBySortOrderAsc(grade.getId())) {
                for (CourseLesson lesson : lessonRepository.findByUnitIdOrderBySortOrderAsc(unit.getId())) {
                    for (CourseTask task : taskRepository.findByLessonIdOrderBySortOrderAsc(lesson.getId())) {
                        allTasks.add(task);
                        Map<String, Object> t = new HashMap<String, Object>();
                        t.put("taskId", task.getId());
                        t.put("title", task.getTitle());
                        t.put("lessonTitle", lesson.getTitle());
                        taskList.add(t);
                    }
                }
            }
        }

        List<Long> taskIds = allTasks.stream().map(CourseTask::getId).collect(Collectors.toList());
        List<LearnSubmission> submissions = taskIds.isEmpty() ? Collections.emptyList()
                : submissionRepository.findByTaskIdIn(taskIds);

        Map<String, LearnSubmission> subMap = new HashMap<String, LearnSubmission>();
        for (LearnSubmission s : submissions) {
            subMap.put(s.getStudentId() + "_" + s.getTaskId(), s);
        }

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (SysUser student : students) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("studentId", student.getId());
            row.put("realName", student.getRealName());

            List<Map<String, Object>> cells = new ArrayList<Map<String, Object>>();
            for (CourseTask task : allTasks) {
                LearnSubmission sub = subMap.get(student.getId() + "_" + task.getId());
                Map<String, Object> cell = new HashMap<String, Object>();
                cell.put("taskId", task.getId());
                cell.put("done", sub != null);
                cell.put("score", sub != null ? sub.getScore() : null);
                cells.add(cell);
            }
            row.put("cells", cells);
            rows.add(row);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("tasks", taskList);
        result.put("rows", rows);
        return result;
    }

    /** 班级积分排行 */
    public List<Map<String, Object>> getClassRanking(Long classId) {
        List<SysUser> students = userRepository.findByClassId(classId);
        List<Long> ids = students.stream().map(SysUser::getId).collect(Collectors.toList());
        if (ids.isEmpty()) return Collections.emptyList();

        Map<Long, String> nameMap = students.stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));

        List<Object[]> ranks = pointsRepository.rankByClassStudents(ids);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        int rank = 1;
        for (Object[] row : ranks) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("rank", rank++);
            item.put("studentId", row[0]);
            item.put("realName", nameMap.get(row[0]));
            item.put("points", row[1]);
            result.add(item);
        }
        return result;
    }

    public void checkTeacherOwnsClass(Long teacherId, Long classId) {
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        if (!cls.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "无权查看该班级");
        }
    }

    /**
     * 课时活动维度看板（参考 keji520 全班学习情况看板）
     * 以课时 EXTERNAL 任务 config_json 中的 activities 为活动列表，按 activityIndex 统计提交
     */
    public Map<String, Object> buildLessonActivityDashboard(Long lessonId, Long classId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        CourseUnit unit = unitRepository.findById(lesson.getUnitId())
                .orElseThrow(() -> new BusinessException("单元不存在"));
        CourseGrade grade = gradeRepository.findById(unit.getGradeId())
                .orElseThrow(() -> new BusinessException("年级不存在"));

        List<SysUser> students = userRepository.findByClassId(classId);
        List<CourseTask> tasks = taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId);

        CourseTask workspaceTask = null;
        JsonNode workspaceConfig = null;
        for (CourseTask task : tasks) {
            if (!"EXTERNAL".equals(task.getTaskType()) || !StringUtils.hasText(task.getConfigJson())) {
                continue;
            }
            try {
                JsonNode cfg = objectMapper.readTree(task.getConfigJson());
                if ("student_workspace".equals(cfg.path("layout").asText())) {
                    workspaceTask = task;
                    workspaceConfig = cfg;
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        List<Map<String, Object>> activityDefs = buildActivityDefinitions(workspaceConfig);
        Map<Long, LearnSubmission> subByStudent = loadClassSubmissions(students, workspaceTask);

        List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> def : activityDefs) {
            int index = (Integer) def.get("index");
            boolean unlocked = def.get("unlocked") == null || Boolean.TRUE.equals(def.get("unlocked"));
            List<Map<String, Object>> submittedStudents = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> unsubmittedStudents = new ArrayList<Map<String, Object>>();

            String actType = (String) def.get("type");
            for (SysUser student : students) {
                LearnSubmission sub = subByStudent.get(student.getId());
                int maxIndex = parseMaxActivityIndex(sub);
                boolean done;
                if ("QUIZ".equals(actType)) {
                    done = unlocked && hasQuizSubmission(sub);
                } else if ("EVALUATION".equals(actType)) {
                    done = unlocked && hasEvaluationSubmission(sub);
                } else {
                    done = unlocked && maxIndex >= index;
                }
                Map<String, Object> row = new HashMap<String, Object>();
                row.put("studentId", student.getId());
                row.put("realName", student.getRealName());
                row.put("username", student.getUsername());
                if (done && sub != null) {
                    row.put("submittedAt", sub.getSubmittedAt());
                    row.put("score", sub.getScore());
                    submittedStudents.add(row);
                } else {
                    unsubmittedStudents.add(row);
                }
            }

            int submittedCount = submittedStudents.size();
            int total = students.size();
            double rate = total > 0 ? Math.round(submittedCount * 1000.0 / total) / 10.0 : 0;

            Map<String, Object> act = new HashMap<String, Object>(def);
            act.put("submittedCount", submittedCount);
            act.put("unsubmittedCount", total - submittedCount);
            act.put("submitRate", rate);
            act.put("submittedStudents", submittedStudents);
            act.put("unsubmittedStudents", unsubmittedStudents);
            activities.add(act);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("lessonId", lessonId);
        result.put("lessonTitle", lesson.getTitle());
        result.put("gradeName", grade.getName());
        result.put("unitName", unit.getName());
        result.put("classId", classId);
        result.put("className", cls.getName());
        result.put("schoolName", "杭州市滨江实验小学");
        result.put("totalStudents", students.size());
        result.put("activityCount", activities.size());
        result.put("activities", activities);
        Collection<LearnSubmission> classSubs = subByStudent.values();
        result.put("quizSummary", buildQuizSummary(classSubs, students.size()));
        result.put("evaluationSummary", buildEvaluationSummary(classSubs, students.size()));
        result.put("updateTime", new Date());
        return result;
    }

    private static final String[] QUIZ_DIMENSIONS = {"知识掌握", "能力提升", "素养变化"};
    private static final String[] EVAL_ASPECTS = {"学习兴趣", "学习难度", "学习信心"};
    private static final int POINTS_PER_QUIZ_DIMENSION = 2;
    private static final int QUIZ_TOTAL_POINTS = 6;

    private List<Map<String, Object>> buildActivityDefinitions(JsonNode workspaceConfig) {
        List<Map<String, Object>> defs = new ArrayList<Map<String, Object>>();
        if (workspaceConfig != null && workspaceConfig.has("activities")) {
            for (JsonNode act : workspaceConfig.get("activities")) {
                Map<String, Object> def = new HashMap<String, Object>();
                def.put("index", act.path("index").asInt());
                def.put("title", act.path("title").asText("活动"));
                def.put("unlocked", act.path("unlocked").asBoolean(true));
                def.put("type", "INTERACTIVE");
                defs.add(def);
            }
        }
        if (defs.isEmpty()) {
            Map<String, Object> fallback = new HashMap<String, Object>();
            fallback.put("index", 1);
            fallback.put("title", "探究活动");
            fallback.put("unlocked", true);
            fallback.put("type", "INTERACTIVE");
            defs.add(fallback);
        }
        defs.sort(Comparator.comparingInt(d -> (Integer) d.get("index")));

        Map<String, Object> evaluation = new HashMap<String, Object>();
        evaluation.put("index", defs.size() + 1);
        evaluation.put("title", "学习评价");
        evaluation.put("unlocked", false);
        evaluation.put("type", "EVALUATION");
        defs.add(evaluation);

        Map<String, Object> quiz = new HashMap<String, Object>();
        quiz.put("index", defs.size() + 1);
        quiz.put("title", "课堂小测");
        quiz.put("unlocked", false);
        quiz.put("type", "QUIZ");
        defs.add(quiz);
        return defs;
    }

    private Map<Long, LearnSubmission> loadClassSubmissions(List<SysUser> students, CourseTask workspaceTask) {
        Map<Long, LearnSubmission> subByStudent = new HashMap<Long, LearnSubmission>();
        if (workspaceTask == null || students.isEmpty()) {
            return subByStudent;
        }
        Set<Long> studentIds = students.stream().map(SysUser::getId).collect(Collectors.toSet());
        for (LearnSubmission s : submissionRepository.findByTaskIdIn(
                Collections.singletonList(workspaceTask.getId()))) {
            if (studentIds.contains(s.getStudentId())) {
                subByStudent.put(s.getStudentId(), s);
            }
        }
        return subByStudent;
    }

    private int parseMaxActivityIndex(LearnSubmission sub) {
        if (sub == null || !StringUtils.hasText(sub.getContentJson())) {
            return 0;
        }
        try {
            JsonNode node = objectMapper.readTree(sub.getContentJson());
            if (node.has("activityIndex") && !"quiz".equals(node.path("type").asText())) {
                int idx = node.get("activityIndex").asInt(0);
                if (idx > 0 && idx < 10) {
                    return idx;
                }
            }
            if (node.has("completedActivities") && node.get("completedActivities").isArray()) {
                int max = 0;
                for (JsonNode item : node.get("completedActivities")) {
                    max = Math.max(max, item.asInt(0));
                }
                return max;
            }
            return 1;
        } catch (Exception ignored) {
            return 1;
        }
    }

    private Map<String, Object> buildQuizSummary(Collection<LearnSubmission> submissions, int totalStudents) {
        Map<String, int[]> dimAgg = new LinkedHashMap<String, int[]>();
        for (String dim : QUIZ_DIMENSIONS) {
            dimAgg.put(dim, new int[]{0, 0});
        }
        List<Integer> totalScores = new ArrayList<Integer>();
        int participants = 0;

        for (LearnSubmission sub : submissions) {
            JsonNode node = parseContentJson(sub);
            if (node == null || !hasQuizSubmission(sub)) {
                continue;
            }
            participants++;
            int[] perStudent = parseQuizDimensionPoints(node);
            int studentTotal = 0;
            for (String dim : QUIZ_DIMENSIONS) {
                int[] agg = dimAgg.get(dim);
                agg[0] += perStudent[dimIndex(dim)];
                agg[1]++;
                studentTotal += perStudent[dimIndex(dim)];
            }
            totalScores.add(studentTotal);
        }

        List<Map<String, Object>> dimensions = new ArrayList<Map<String, Object>>();
        for (String dim : QUIZ_DIMENSIONS) {
            int[] agg = dimAgg.get(dim);
            double avg = participants > 0 ? round1(agg[0] * 1.0 / participants) : 0;
            double rate = participants > 0
                    ? round1(agg[0] * 100.0 / (participants * POINTS_PER_QUIZ_DIMENSION)) : 0;
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("name", dim);
            row.put("maxScore", POINTS_PER_QUIZ_DIMENSION);
            row.put("avgScore", avg);
            row.put("correctRate", rate);
            dimensions.add(row);
        }

        double avgTotal = 0;
        int maxTotal = 0;
        int minTotal = 0;
        double passRate = 0;
        if (!totalScores.isEmpty()) {
            int sum = 0;
            maxTotal = totalScores.get(0);
            minTotal = totalScores.get(0);
            int passCount = 0;
            for (int s : totalScores) {
                sum += s;
                maxTotal = Math.max(maxTotal, s);
                minTotal = Math.min(minTotal, s);
                if (s >= 4) {
                    passCount++;
                }
            }
            avgTotal = round1(sum * 1.0 / totalScores.size());
            passRate = round1(passCount * 100.0 / totalScores.size());
        }

        Map<String, Object> quiz = new HashMap<String, Object>();
        quiz.put("participantCount", participants);
        quiz.put("totalMaxScore", QUIZ_TOTAL_POINTS);
        quiz.put("avgTotalScore", avgTotal);
        quiz.put("maxScore", maxTotal);
        quiz.put("minScore", minTotal);
        quiz.put("passRate", passRate);
        quiz.put("submitRate", totalStudents > 0 ? round1(participants * 100.0 / totalStudents) : 0);
        quiz.put("dimensions", dimensions);
        return quiz;
    }

    private Map<String, Object> buildEvaluationSummary(Collection<LearnSubmission> submissions, int totalStudents) {
        Map<String, Map<Integer, Integer>> aspectCounts = new LinkedHashMap<String, Map<Integer, Integer>>();
        Map<String, Integer> aspectParticipants = new LinkedHashMap<String, Integer>();
        for (String aspect : EVAL_ASPECTS) {
            aspectCounts.put(aspect, new HashMap<Integer, Integer>());
            aspectParticipants.put(aspect, 0);
        }
        int evalParticipants = 0;

        for (LearnSubmission sub : submissions) {
            JsonNode node = parseContentJson(sub);
            if (node == null || !hasEvaluationSubmission(sub)) {
                continue;
            }
            evalParticipants++;
            JsonNode answers = node.path("evaluationAnswers");
            if (!answers.isArray() || answers.size() == 0) {
                answers = node.path("answers");
            }
            if (!answers.isArray()) {
                continue;
            }
            for (JsonNode ans : answers) {
                String aspect = normalizeEvalAspect(ans.path("aspect").asText(""));
                if (!aspectCounts.containsKey(aspect)) {
                    continue;
                }
                int selected = ans.path("selected").asInt(-1);
                if (selected < 0) {
                    continue;
                }
                Map<Integer, Integer> counts = aspectCounts.get(aspect);
                counts.put(selected, counts.getOrDefault(selected, 0) + 1);
                aspectParticipants.put(aspect, aspectParticipants.get(aspect) + 1);
            }
        }

        List<Map<String, Object>> dimensions = new ArrayList<Map<String, Object>>();
        for (String aspect : EVAL_ASPECTS) {
            Map<Integer, Integer> counts = aspectCounts.get(aspect);
            int answered = aspectParticipants.get(aspect);
            int positive = counts.getOrDefault(0, 0) + counts.getOrDefault(1, 0);
            double positiveRate = answered > 0 ? round1(positive * 100.0 / answered) : 0;
            double avgIndex = 0;
            if (answered > 0) {
                int sumIdx = 0;
                for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
                    sumIdx += e.getKey() * e.getValue();
                }
                avgIndex = round1(sumIdx * 1.0 / answered);
            }
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("name", aspect);
            row.put("participantCount", answered);
            row.put("positiveRate", positiveRate);
            row.put("avgIndex", avgIndex);
            row.put("distribution", buildDistribution(counts, answered));
            dimensions.add(row);
        }

        Map<String, Object> eval = new HashMap<String, Object>();
        eval.put("participantCount", evalParticipants);
        eval.put("submitRate", totalStudents > 0 ? round1(evalParticipants * 100.0 / totalStudents) : 0);
        eval.put("dimensions", dimensions);
        return eval;
    }

    private String normalizeEvalAspect(String aspect) {
        if ("难度感知".equals(aspect) || "学习难度".equals(aspect)) {
            return "学习难度";
        }
        return aspect;
    }

    private List<Map<String, Object>> buildDistribution(Map<Integer, Integer> counts, int total) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (total <= 0) {
            return list;
        }
        List<Integer> keys = new ArrayList<Integer>(counts.keySet());
        Collections.sort(keys);
        for (Integer key : keys) {
            int count = counts.get(key);
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("optionIndex", key);
            item.put("count", count);
            item.put("rate", round1(count * 100.0 / total));
            list.add(item);
        }
        return list;
    }

    private int[] parseQuizDimensionPoints(JsonNode node) {
        int[] points = new int[]{0, 0, 0};
        JsonNode dimScores = node.path("dimensionScores");
        if (dimScores.isObject() && dimScores.size() > 0) {
            for (int i = 0; i < QUIZ_DIMENSIONS.length; i++) {
                JsonNode d = dimScores.path(QUIZ_DIMENSIONS[i]);
                int correct = Math.min(d.path("correct").asInt(0), POINTS_PER_QUIZ_DIMENSION);
                points[i] = correct;
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
            return points;
        }
        if (node.has("correctCount")) {
            int correct = node.get("correctCount").asInt(0);
            int perDim = correct / QUIZ_DIMENSIONS.length;
            int rem = correct % QUIZ_DIMENSIONS.length;
            for (int i = 0; i < QUIZ_DIMENSIONS.length; i++) {
                points[i] = Math.min(perDim + (i < rem ? 1 : 0), POINTS_PER_QUIZ_DIMENSION);
            }
            return points;
        }
        if (node.has("quizScore")) {
            int total = normalizeQuizTotal(node.get("quizScore").asInt(0));
            int perDim = total / QUIZ_DIMENSIONS.length;
            int rem = total % QUIZ_DIMENSIONS.length;
            for (int i = 0; i < QUIZ_DIMENSIONS.length; i++) {
                points[i] = Math.min(perDim + (i < rem ? 1 : 0), POINTS_PER_QUIZ_DIMENSION);
            }
        }
        return points;
    }

    private int normalizeQuizTotal(int quizScore) {
        if (quizScore <= QUIZ_TOTAL_POINTS) {
            return quizScore;
        }
        return Math.min(QUIZ_TOTAL_POINTS, Math.round(quizScore * QUIZ_TOTAL_POINTS / 100f));
    }

    private int dimIndex(String dim) {
        for (int i = 0; i < QUIZ_DIMENSIONS.length; i++) {
            if (QUIZ_DIMENSIONS[i].equals(dim)) {
                return i;
            }
        }
        return 0;
    }

    private boolean hasQuizSubmission(LearnSubmission sub) {
        JsonNode node = parseContentJson(sub);
        if (node == null) {
            return false;
        }
        if (node.has("dimensionScores") || node.has("quizScore")) {
            return true;
        }
        if ("quiz".equals(node.path("type").asText())) {
            return true;
        }
        JsonNode answers = node.path("answers");
        if (answers.isArray()) {
            for (JsonNode ans : answers) {
                if (ans.has("dimension") || ans.has("isCorrect")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasEvaluationSubmission(LearnSubmission sub) {
        JsonNode node = parseContentJson(sub);
        if (node == null) {
            return false;
        }
        if (node.has("evaluationAnswers")) {
            return node.get("evaluationAnswers").size() > 0;
        }
        if ("evaluation".equals(node.path("type").asText()) && node.has("answers")) {
            return node.get("answers").size() > 0;
        }
        return false;
    }

    private JsonNode parseContentJson(LearnSubmission sub) {
        if (sub == null || !StringUtils.hasText(sub.getContentJson())) {
            return null;
        }
        try {
            return objectMapper.readTree(sub.getContentJson());
        } catch (Exception ignored) {
            return null;
        }
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
