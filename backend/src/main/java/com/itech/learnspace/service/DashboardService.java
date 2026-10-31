package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.entity.*;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final LearnSubmissionLogRepository submissionLogRepository;
    private final LearnPointsRepository pointsRepository;
    private final LearnProgressRepository progressRepository;
    private final LearnHintLogRepository hintLogRepository;
    private final LearnVisitLogRepository visitLogRepository;
    private final LearnParkAccessRepository parkAccessRepository;
    private final ScaffoldService scaffoldService;
    private final SseService sseService;
    private final TeacherClassService teacherClassService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final long LESSON_DASH_CACHE_MS = 4000L;
    private final Map<String, Map<String, Object>> lessonDashCache = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, Long> lessonDashCachedAt = new ConcurrentHashMap<String, Long>();

    public DashboardService(SysUserRepository userRepository, SysClassRepository classRepository,
                            CourseGradeRepository gradeRepository, CourseUnitRepository unitRepository,
                            CourseLessonRepository lessonRepository, CourseTaskRepository taskRepository,
                            LearnSubmissionRepository submissionRepository,
                            LearnSubmissionLogRepository submissionLogRepository,
                            LearnPointsRepository pointsRepository,
                            LearnProgressRepository progressRepository,
                            LearnHintLogRepository hintLogRepository,
                            LearnVisitLogRepository visitLogRepository,
                            LearnParkAccessRepository parkAccessRepository,
                            @Lazy ScaffoldService scaffoldService,
                            @Lazy SseService sseService,
                            TeacherClassService teacherClassService) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.gradeRepository = gradeRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
        this.taskRepository = taskRepository;
        this.submissionRepository = submissionRepository;
        this.submissionLogRepository = submissionLogRepository;
        this.pointsRepository = pointsRepository;
        this.progressRepository = progressRepository;
        this.hintLogRepository = hintLogRepository;
        this.visitLogRepository = visitLogRepository;
        this.parkAccessRepository = parkAccessRepository;
        this.scaffoldService = scaffoldService;
        this.sseService = sseService;
        this.teacherClassService = teacherClassService;
    }

    /** 已匹配任课班级的教师只返回这些班；还没匹配过的教师仍看全部。 */
    public List<SysClass> getTeacherClasses(Long teacherId) {
        return teacherClassService.visibleClasses(teacherId);
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

    /** 班级积分排行（含 0 分学生，按累计获得积分排序） */
    public List<Map<String, Object>> getClassRanking(Long classId) {
        return getClassRanking(classId, null);
    }

    public List<Map<String, Object>> getClassRanking(Long classId, Long currentStudentId) {
        List<SysUser> students = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1);
        if (students.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = students.stream().map(SysUser::getId).collect(Collectors.toList());
        Map<Long, Integer> pointsMap = new HashMap<Long, Integer>();
        for (Object[] row : pointsRepository.rankByClassStudents(ids)) {
            Number pts = (Number) row[1];
            pointsMap.put((Long) row[0], pts == null ? 0 : pts.intValue());
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (SysUser s : students) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("studentId", s.getId());
            item.put("realName", s.getRealName());
            item.put("points", pointsMap.getOrDefault(s.getId(), 0));
            item.put("isMe", currentStudentId != null && currentStudentId.equals(s.getId()));
            result.add(item);
        }
        result.sort((a, b) -> {
            int pb = ((Number) b.get("points")).intValue();
            int pa = ((Number) a.get("points")).intValue();
            if (pb != pa) {
                return pb - pa;
            }
            return String.valueOf(a.get("realName")).compareTo(String.valueOf(b.get("realName")));
        });
        int rank = 1;
        for (Map<String, Object> item : result) {
            item.put("rank", rank++);
        }
        return result;
    }

    /** 学生班级学习大屏：本班排行 + 本课各环节完成人数 */
    public Map<String, Object> buildStudentClassScreen(Long classId, Long lessonId, Long studentId) {
        Map<String, Object> dash = buildLessonActivityDashboard(lessonId, classId);
        List<Map<String, Object>> ranking = getClassRanking(classId, studentId);
        List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        Object rawActs = dash.get("activities");
        if (rawActs instanceof List) {
            for (Object o : (List<?>) rawActs) {
                if (!(o instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> src = (Map<String, Object>) o;
                Map<String, Object> act = new HashMap<String, Object>();
                act.put("index", src.get("index"));
                act.put("title", src.get("title"));
                act.put("type", src.get("type"));
                act.put("submittedCount", src.get("submittedCount"));
                act.put("totalStudents", dash.get("totalStudents"));
                act.put("submitRate", src.get("submitRate"));
                activities.add(act);
            }
        }
        Integer myRank = null;
        Integer myPoints = 0;
        for (Map<String, Object> row : ranking) {
            if (Boolean.TRUE.equals(row.get("isMe"))) {
                myRank = (Integer) row.get("rank");
                myPoints = ((Number) row.get("points")).intValue();
                break;
            }
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("classId", classId);
        result.put("className", dash.get("className"));
        result.put("lessonId", lessonId);
        result.put("lessonTitle", dash.get("lessonTitle"));
        result.put("totalStudents", dash.get("totalStudents"));
        result.put("ranking", ranking);
        result.put("activities", activities);
        result.put("myRank", myRank);
        result.put("myPoints", myPoints);
        result.put("updateTime", dash.get("updateTime"));
        return result;
    }

    /** 教师查看学生端积分榜。没有班级时给出空榜，有多个班时先看第一个班。 */
    public Map<String, Object> buildTeacherClassScreen(Long teacherId, Long lessonId) {
        List<SysClass> classes = classRepository.findByTeacherId(teacherId);
        if (classes == null || classes.isEmpty()) {
            Map<String, Object> empty = new HashMap<String, Object>();
            empty.put("className", "还没有绑定班级");
            empty.put("lessonTitle", "");
            empty.put("totalStudents", 0);
            empty.put("ranking", new ArrayList<Map<String, Object>>());
            empty.put("activities", new ArrayList<Map<String, Object>>());
            empty.put("myRank", null);
            empty.put("myPoints", 0);
            return empty;
        }
        return buildStudentClassScreen(classes.get(0).getId(), lessonId, teacherId);
    }

    public void checkTeacherOwnsClass(Long teacherId, Long classId) {
        teacherClassService.assertCanView(teacherId, classId);
    }

    /**
     * 清除本班本课的学生上课记录：提交、进度、访问、本课积分、支架提示、游学乐园申请。
     * 不清除前测和教师指定的课堂档位。
     */
    @Transactional
    public Map<String, Object> clearLessonClassRecords(Long lessonId, Long classId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));

        List<Long> studentIds = userRepository.findStudentIdsInClass(classId).stream()
                .map(Number::longValue)
                .distinct()
                .collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            studentIds = userRepository.findByClassId(classId).stream()
                    .filter(s -> s != null && "STUDENT".equals(s.getRole()))
                    .map(SysUser::getId)
                    .collect(Collectors.toList());
        }

        List<CourseTask> tasks = taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
        List<Long> taskIds = tasks.stream().map(CourseTask::getId).collect(Collectors.toList());

        int submissions = 0;
        int progress = 0;
        int visits = 0;
        int hints = 0;
        int points = 0;
        int park = 0;
        if (!studentIds.isEmpty()) {
            if (!taskIds.isEmpty()) {
                submissions = (int) submissionRepository.deleteByTaskIdInAndStudentIdIn(taskIds, studentIds);
                points += (int) pointsRepository.deleteByStudentIdInAndSourceTypeAndSourceIdIn(
                        studentIds, "TASK", taskIds);
            }
            List<Long> activitySourceIds = new ArrayList<Long>();
            for (int slot = 1; slot <= 20; slot++) {
                activitySourceIds.add(lessonId * 100L + slot);
            }
            points += (int) pointsRepository.deleteByStudentIdInAndSourceTypeAndSourceIdIn(
                    studentIds, "ACTIVITY", activitySourceIds);
            points += (int) pointsRepository.deleteByStudentIdInAndSourceTypeAndSourceIdIn(
                    studentIds, "BONUS", activitySourceIds);
            progress = (int) progressRepository.deleteByLessonIdAndStudentIdIn(lessonId, studentIds);
            visits = (int) visitLogRepository.deleteByLessonIdAndStudentIdIn(lessonId, studentIds);
            hints = (int) hintLogRepository.deleteByLessonIdAndStudentIdIn(lessonId, studentIds);
        }
        park = (int) parkAccessRepository.deleteByClassIdAndLessonId(classId, lessonId);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("lessonId", lessonId);
        result.put("lessonTitle", lesson.getTitle());
        result.put("classId", classId);
        result.put("className", cls.getName());
        result.put("studentCount", studentIds.size());
        result.put("deletedSubmissions", submissions);
        result.put("deletedProgress", progress);
        result.put("deletedVisits", visits);
        result.put("deletedHints", hints);
        result.put("deletedPoints", points);
        result.put("deletedPark", park);

        lessonDashCache.remove(lessonId + ":" + classId);
        lessonDashCachedAt.remove(lessonId + ":" + classId);
        try {
            sseService.broadcast(classId, buildLessonActivityDashboard(lessonId, classId));
        } catch (Exception ignored) {
        }
        return result;
    }

    /**
     * 课时活动维度看板（参考 keji520 全班学习情况看板）
     * 以课时 EXTERNAL 任务 config_json 中的 activities 为活动列表，按 activityIndex 统计提交
     */
    public Map<String, Object> buildLessonActivityDashboard(Long lessonId, Long classId) {
        String cacheKey = lessonId + ":" + classId;
        Long cachedAt = lessonDashCachedAt.get(cacheKey);
        Map<String, Object> cached = lessonDashCache.get(cacheKey);
        if (cached != null && cachedAt != null
                && System.currentTimeMillis() - cachedAt < LESSON_DASH_CACHE_MS) {
            return cached;
        }
        Map<String, Object> built = buildLessonActivityDashboardNow(lessonId, classId);
        lessonDashCache.put(cacheKey, built);
        lessonDashCachedAt.put(cacheKey, System.currentTimeMillis());
        return built;
    }

    private Map<String, Object> buildLessonActivityDashboardNow(Long lessonId, Long classId) {
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
        Map<Long, Set<Integer>> doneByStudent = collectDoneByStudent(lessonId, students, subByStudent);
        Map<Long, String> tierByStudent = scaffoldService.effectiveTiers(students);
        Map<String, Integer> hintCounts = scaffoldService.hintCountByStudentActivity(lessonId);

        List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> def : activityDefs) {
            int index = (Integer) def.get("index");
            List<Map<String, Object>> submittedStudents = new ArrayList<Map<String, Object>>();
            List<Map<String, Object>> unsubmittedStudents = new ArrayList<Map<String, Object>>();

            String actType = (String) def.get("type");
            for (SysUser student : students) {
                LearnSubmission sub = subByStudent.get(student.getId());
                boolean done = partDone(actType, index, doneByStudent.get(student.getId()), sub);
                Map<String, Object> row = new HashMap<String, Object>();
                row.put("studentId", student.getId());
                row.put("realName", student.getRealName());
                row.put("username", student.getUsername());
                if (done && sub != null) {
                    row.put("submittedAt", sub.getSubmittedAt());
                    if ("QUIZ".equals(actType)) {
                        int quizScore = quizTotalOf(sub);
                        row.put("score", quizScore >= 0 ? quizScore : sub.getScore());
                    } else {
                        row.put("score", sub.getScore());
                    }
                    if ("INTERACTIVE".equals(actType) && String.valueOf(def.get("title")).contains("流程")) {
                        attachFlowchart(row, sub, index);
                    }
                    if (isSurveyThemeActivity(def)) {
                        row.put("surveyTheme", surveyThemeOf(sub));
                    }
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
            final String doneType = actType;
            act.put("tierStats", scaffoldService.buildActivityTierSlice(
                    index, students, tierByStudent, subByStudent, hintCounts,
                    (student, sub) -> partDone(doneType, index, doneByStudent.get(student.getId()), sub)));
            if (isSurveyThemeActivity(def)) {
                List<Map<String, Object>> themeRoster = new ArrayList<Map<String, Object>>();
                for (SysUser student : students) {
                    LearnSubmission sub = subByStudent.get(student.getId());
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("studentId", student.getId());
                    item.put("realName", student.getRealName());
                    item.put("username", student.getUsername());
                    item.put("surveyTheme", surveyThemeOf(sub));
                    themeRoster.add(item);
                }
                act.put("surveyThemes", themeRoster);
            }
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
        result.put("quizSummary", buildQuizSummary(students, subByStudent));
        result.put("evaluationSummary", buildEvaluationSummary(students, subByStudent));
        result.put("tierRoster", buildTierRoster(activityDefs, students, subByStudent, tierByStudent));
        result.put("updateTime", new Date());
        return result;
    }

    private static final String[] QUIZ_DIMENSIONS = {"知识掌握", "能力提升", "素养变化"};
    private static final String[] EVAL_ASPECTS = {"学习兴趣", "学习难度", "学习信心"};
    private static final int POINTS_PER_QUIZ_DIMENSION = 2;
    private static final int QUIZ_TOTAL_POINTS = 6;
    /** 课堂小测低于 60 分视为低分，与学生端「建议回顾」一致 */
    private static final int QUIZ_LOW_PERCENT = 60;

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

    /** 最新一份原文被草稿盖掉时，仍认正式提交记录里的探究、小测和学习评价。 */
    private Map<Long, Set<Integer>> collectDoneByStudent(Long lessonId, List<SysUser> students,
                                                         Map<Long, LearnSubmission> subByStudent) {
        Map<Long, Set<Integer>> done = new HashMap<Long, Set<Integer>>();
        if (students == null || students.isEmpty()) {
            return done;
        }
        List<Long> studentIds = new ArrayList<Long>();
        for (SysUser student : students) {
            studentIds.add(student.getId());
            Set<Integer> set = new HashSet<Integer>();
            absorbDone(set, subByStudent.get(student.getId()));
            done.put(student.getId(), set);
        }
        for (LearnSubmissionLog log : submissionLogRepository.findByLessonIdAndStudentIdIn(lessonId, studentIds)) {
            if (log.getStudentId() == null) {
                continue;
            }
            Set<Integer> set = done.get(log.getStudentId());
            if (set == null) {
                continue;
            }
            Integer idx = log.getActivityIndex();
            if (idx != null && idx > 0 && idx < 10) {
                set.add(idx);
            }
            if (idx != null && idx == 99 || "quiz".equals(log.getActivityKey())) {
                set.add(99);
            }
            if (idx != null && idx == 98 || "evaluation".equals(log.getActivityKey())) {
                set.add(98);
            }
        }
        return done;
    }

    private void absorbDone(Set<Integer> done, LearnSubmission sub) {
        if (sub == null || !StringUtils.hasText(sub.getContentJson())) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(sub.getContentJson());
            if (node.has("completedActivities") && node.get("completedActivities").isArray()) {
                for (JsonNode item : node.get("completedActivities")) {
                    int idx = item.asInt(0);
                    if (idx > 0 && idx < 10) {
                        done.add(idx);
                    }
                }
            }
            if (node.path("stepCompleted").asBoolean(false)) {
                String type = node.path("type").asText("");
                int idx = node.path("activityIndex").asInt(0);
                if (idx > 0 && idx < 10 && !"quiz".equals(type) && !"evaluation".equals(type)) {
                    done.add(idx);
                }
            }
            if (hasQuizSubmission(sub)) {
                done.add(99);
            }
            if (hasEvaluationSubmission(sub)) {
                done.add(98);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean partDone(String actType, int index, Set<Integer> done, LearnSubmission sub) {
        Set<Integer> marks = done == null ? Collections.<Integer>emptySet() : done;
        if ("QUIZ".equals(actType)) {
            return marks.contains(99) || hasQuizSubmission(sub);
        }
        if ("EVALUATION".equals(actType)) {
            return marks.contains(98) || hasEvaluationSubmission(sub);
        }
        return marks.contains(index) || parseMaxActivityIndex(sub) >= index;
    }

    private int parseMaxActivityIndex(LearnSubmission sub) {
        if (sub == null || !StringUtils.hasText(sub.getContentJson())) {
            return 0;
        }
        try {
            JsonNode node = objectMapper.readTree(sub.getContentJson());
            String type = node.path("type").asText("");
            int max = 0;
            if (node.has("completedActivities") && node.get("completedActivities").isArray()) {
                for (JsonNode item : node.get("completedActivities")) {
                    max = Math.max(max, item.asInt(0));
                }
            }
            if (!"quiz".equals(type) && !"evaluation".equals(type) && node.has("activityIndex")) {
                int idx = node.get("activityIndex").asInt(0);
                if (idx > 0 && idx < 10) {
                    max = Math.max(max, idx);
                }
            }
            return max;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private Map<String, Object> buildQuizSummary(List<SysUser> students, Map<Long, LearnSubmission> subByStudent) {
        Map<String, int[]> dimAgg = new LinkedHashMap<String, int[]>();
        Map<String, List<Map<String, Object>>> dimWeakNames = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (String dim : QUIZ_DIMENSIONS) {
            dimAgg.put(dim, new int[]{0, 0});
            dimWeakNames.put(dim, new ArrayList<Map<String, Object>>());
        }
        List<Integer> totalScores = new ArrayList<Integer>();
        List<Integer> percents = new ArrayList<Integer>();
        List<Map<String, Object>> lowScoreStudents = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> unsubmittedStudents = new ArrayList<Map<String, Object>>();
        int participants = 0;

        for (SysUser student : students) {
            LearnSubmission sub = subByStudent.get(student.getId());
            JsonNode node = parseContentJson(sub);
            if (node == null || !hasQuizSubmission(sub)) {
                unsubmittedStudents.add(studentBrief(student));
                continue;
            }
            participants++;
            int[] perStudent = parseQuizDimensionPoints(node);
            int studentTotal = 0;
            for (String dim : QUIZ_DIMENSIONS) {
                int idx = dimIndex(dim);
                int[] agg = dimAgg.get(dim);
                agg[0] += perStudent[idx];
                agg[1]++;
                studentTotal += perStudent[idx];
                if (perStudent[idx] == 0) {
                    dimWeakNames.get(dim).add(studentBrief(student));
                }
            }
            totalScores.add(studentTotal);
            int percent = quizPercentOf(node, studentTotal);
            percents.add(percent);
            if (percent < QUIZ_LOW_PERCENT) {
                Map<String, Object> row = studentBrief(student);
                row.put("score", percent);
                row.put("pointScore", studentTotal);
                row.put("correctCount", node.path("correctCount").asInt(-1));
                row.put("totalQuestions", node.path("totalQuestions").asInt(-1));
                row.put("scoreLabel", percent + "分");
                lowScoreStudents.add(row);
            }
        }

        lowScoreStudents.sort((a, b) -> {
            int cmp = Integer.compare((Integer) a.get("score"), (Integer) b.get("score"));
            return cmp != 0 ? cmp : String.valueOf(a.get("realName")).compareTo(String.valueOf(b.get("realName")));
        });
        unsubmittedStudents.sort((a, b) -> String.valueOf(a.get("realName")).compareTo(String.valueOf(b.get("realName"))));

        List<Map<String, Object>> dimensions = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> weakDimensions = new ArrayList<Map<String, Object>>();
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
            if (participants > 0 && rate < QUIZ_LOW_PERCENT) {
                Map<String, Object> weak = new LinkedHashMap<String, Object>();
                weak.put("name", dim);
                weak.put("correctRate", rate);
                weak.put("students", dimWeakNames.get(dim));
                weakDimensions.add(weak);
            }
        }

        double avgTotal = 0;
        int maxTotal = 0;
        int minTotal = 0;
        double passRate = 0;
        double avgPercent = 0;
        if (!totalScores.isEmpty()) {
            int sum = 0;
            maxTotal = totalScores.get(0);
            minTotal = totalScores.get(0);
            int passCount = 0;
            int percentSum = 0;
            for (int i = 0; i < totalScores.size(); i++) {
                int s = totalScores.get(i);
                sum += s;
                maxTotal = Math.max(maxTotal, s);
                minTotal = Math.min(minTotal, s);
                int p = percents.get(i);
                percentSum += p;
                if (p >= QUIZ_LOW_PERCENT) {
                    passCount++;
                }
            }
            avgTotal = round1(sum * 1.0 / totalScores.size());
            avgPercent = round1(percentSum * 1.0 / percents.size());
            passRate = round1(passCount * 100.0 / totalScores.size());
        }

        int totalStudents = students.size();
        Map<String, Object> quiz = new HashMap<String, Object>();
        quiz.put("participantCount", participants);
        quiz.put("totalMaxScore", QUIZ_TOTAL_POINTS);
        quiz.put("avgTotalScore", avgTotal);
        quiz.put("avgPercent", avgPercent);
        quiz.put("maxScore", maxTotal);
        quiz.put("minScore", minTotal);
        quiz.put("passRate", passRate);
        quiz.put("lowThreshold", QUIZ_LOW_PERCENT);
        quiz.put("submitRate", totalStudents > 0 ? round1(participants * 100.0 / totalStudents) : 0);
        quiz.put("dimensions", dimensions);
        quiz.put("weakDimensions", weakDimensions);
        quiz.put("lowScoreStudents", lowScoreStudents);
        quiz.put("unsubmittedStudents", unsubmittedStudents);
        return quiz;
    }

    private Map<String, Object> buildEvaluationSummary(List<SysUser> students, Map<Long, LearnSubmission> subByStudent) {
        Map<String, Map<Integer, Integer>> aspectCounts = new LinkedHashMap<String, Map<Integer, Integer>>();
        Map<String, Integer> aspectParticipants = new LinkedHashMap<String, Integer>();
        Map<String, List<Map<String, Object>>> aspectConcerns = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (String aspect : EVAL_ASPECTS) {
            aspectCounts.put(aspect, new HashMap<Integer, Integer>());
            aspectParticipants.put(aspect, 0);
            aspectConcerns.put(aspect, new ArrayList<Map<String, Object>>());
        }
        List<Map<String, Object>> concernStudents = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> unsubmittedStudents = new ArrayList<Map<String, Object>>();
        int evalParticipants = 0;

        for (SysUser student : students) {
            LearnSubmission sub = subByStudent.get(student.getId());
            JsonNode node = parseContentJson(sub);
            if (node == null || !hasEvaluationSubmission(sub)) {
                unsubmittedStudents.add(studentBrief(student));
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
            List<String> reasons = new ArrayList<String>();
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
                String label = ans.path("label").asText("");
                if (isEvalConcern(aspect, selected, label)) {
                    String reason = evalConcernReason(aspect);
                    reasons.add(reason);
                    Map<String, Object> named = studentBrief(student);
                    named.put("option", label);
                    aspectConcerns.get(aspect).add(named);
                }
            }
            if (!reasons.isEmpty()) {
                Map<String, Object> row = studentBrief(student);
                row.put("reasons", reasons.stream().distinct().collect(Collectors.toList()));
                concernStudents.add(row);
            }
        }

        concernStudents.sort((a, b) -> String.valueOf(a.get("realName")).compareTo(String.valueOf(b.get("realName"))));
        unsubmittedStudents.sort((a, b) -> String.valueOf(a.get("realName")).compareTo(String.valueOf(b.get("realName"))));

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
            row.put("concernStudents", aspectConcerns.get(aspect));
            dimensions.add(row);
        }

        int totalStudents = students.size();
        Map<String, Object> eval = new HashMap<String, Object>();
        eval.put("participantCount", evalParticipants);
        eval.put("submitRate", totalStudents > 0 ? round1(evalParticipants * 100.0 / totalStudents) : 0);
        eval.put("dimensions", dimensions);
        eval.put("concernStudents", concernStudents);
        eval.put("unsubmittedStudents", unsubmittedStudents);
        return eval;
    }

    private Map<String, Object> studentBrief(SysUser student) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("studentId", student.getId());
        row.put("realName", student.getRealName());
        row.put("username", student.getUsername());
        return row;
    }

    /** 统一换成百分制，兼容 6 分制与按题数计分 */
    private int quizPercentOf(JsonNode node, int pointScore) {
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
        }
        JsonNode answers = node.path("answers");
        if (answers.isArray() && answers.size() > 0) {
            int ok = 0;
            int n = 0;
            for (JsonNode ans : answers) {
                if (ans.has("isCorrect") || ans.has("dimension")) {
                    n++;
                    if (ans.path("isCorrect").asBoolean(false)) {
                        ok++;
                    }
                }
            }
            if (n > 0) {
                return Math.round(ok * 100f / n);
            }
        }
        return Math.min(100, Math.round(pointScore * 100f / QUIZ_TOTAL_POINTS));
    }

    private boolean isEvalConcern(String aspect, int selected, String label) {
        String text = label == null ? "" : label;
        if (text.contains("不难") || text.contains("掌握较好")) {
            return false;
        }
        if ("学习兴趣".equals(aspect) || "学习信心".equals(aspect)) {
            return selected >= 2;
        }
        if ("学习难度".equals(aspect)) {
            return text.contains("不太") || text.contains("很难") || text.contains("比较难")
                    || text.contains("有点难") || text.contains("挑战");
        }
        return selected >= 2;
    }

    private String evalConcernReason(String aspect) {
        if ("学习兴趣".equals(aspect)) {
            return "兴趣偏低";
        }
        if ("学习信心".equals(aspect)) {
            return "信心不足";
        }
        if ("学习难度".equals(aspect)) {
            return "感觉较难";
        }
        return "需关注";
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
        if (node.has("dimensionScores") || node.has("quizScore") || node.path("quizSubmitted").asBoolean(false)) {
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
        if (node.path("evaluationSubmitted").asBoolean(false)) {
            return true;
        }
        if (node.has("evaluationAnswers")) {
            return node.get("evaluationAnswers").size() > 0;
        }
        if ("evaluation".equals(node.path("type").asText()) && node.has("answers")) {
            return node.get("answers").size() > 0;
        }
        return false;
    }

    private boolean isSurveyThemeActivity(Map<String, Object> def) {
        return def != null
                && Integer.valueOf(3).equals(def.get("index"))
                && "收回汇总".equals(String.valueOf(def.get("title")));
    }

    private String surveyThemeOf(LearnSubmission sub) {
        JsonNode node = parseContentJson(sub);
        if (node == null) {
            return "";
        }
        JsonNode fields = node.path("activityDrafts").path("3").path("fields");
        if (fields.isArray()) {
            for (JsonNode field : fields) {
                if ("themeInput".equals(field.path("id").asText(""))) {
                    String typed = field.path("value").asText("").trim();
                    if (StringUtils.hasText(typed)) {
                        return typed;
                    }
                }
            }
        }
        return node.path("surveyTheme").asText("").trim();
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

    /**
     * 按本课探究 / 流程图 / 小测给出第2课建议档。
     * 暂无提交：沿用当前档；探究完成≤1 或流程图&lt;50：C；三个探究完成且流程图≥80：A；其余：B。
     */
    private Map<String, Object> buildTierRoster(List<Map<String, Object>> activityDefs,
                                                List<SysUser> students,
                                                Map<Long, LearnSubmission> subByStudent,
                                                Map<Long, String> tierByStudent) {
        List<Integer> inquiryIdx = new ArrayList<Integer>();
        for (Map<String, Object> def : activityDefs) {
            if ("INTERACTIVE".equals(def.get("type"))) {
                inquiryIdx.add((Integer) def.get("index"));
            }
        }
        int inquiryTotal = inquiryIdx.size();
        int curA = 0, curB = 0, curC = 0;
        int sugA = 0, sugB = 0, sugC = 0;
        int withData = 0;
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (SysUser student : students) {
            LearnSubmission sub = subByStudent.get(student.getId());
            int inquiryDone = inquiryDoneCount(sub, inquiryIdx);
            int flowchartScore = flowchartScoreOf(sub);
            int quizScore = quizTotalOf(sub);
            boolean hasData = inquiryDone > 0 || flowchartScore >= 0 || quizScore >= 0;
            if (hasData) {
                withData++;
            }
            String current = tierByStudent.getOrDefault(student.getId(), "B");
            if (!"A".equals(current) && !"C".equals(current)) {
                current = "B";
            }
            String suggested = suggestLessonTier(inquiryDone, inquiryTotal, flowchartScore, quizScore, current);
            String override = student.getTierOverride();
            if (override != null) {
                override = override.trim().toUpperCase();
                if (!"A".equals(override) && !"B".equals(override) && !"C".equals(override)) {
                    override = null;
                }
            }
            if ("A".equals(current)) curA++;
            else if ("C".equals(current)) curC++;
            else curB++;
            if ("A".equals(suggested)) sugA++;
            else if ("C".equals(suggested)) sugC++;
            else sugB++;

            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("studentId", student.getId());
            row.put("realName", student.getRealName());
            row.put("username", student.getUsername());
            row.put("inquiryDone", inquiryDone);
            row.put("inquiryTotal", inquiryTotal);
            row.put("flowchartScore", flowchartScore >= 0 ? flowchartScore : null);
            row.put("quizScore", quizScore >= 0 ? quizScore : null);
            row.put("hasData", hasData);
            row.put("currentTier", current);
            row.put("suggestedTier", suggested);
            row.put("suggestReason", suggestReason(hasData, suggested, inquiryDone, flowchartScore));
            row.put("tierOverride", override);
            list.add(row);
        }
        list.sort((a, b) -> String.valueOf(a.get("realName")).compareTo(String.valueOf(b.get("realName"))));
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("students", list);
        out.put("inquiryTotal", inquiryTotal);
        out.put("withData", withData);
        out.put("countA", curA);
        out.put("countB", curB);
        out.put("countC", curC);
        out.put("suggestA", sugA);
        out.put("suggestB", sugB);
        out.put("suggestC", sugC);
        out.put("rule", "暂无提交沿用当前档；探究完成≤1或流程图<50建议C；三个探究完成且流程图≥80建议A；其余建议B。");
        return out;
    }

    private int inquiryDoneCount(LearnSubmission sub, List<Integer> inquiryIdx) {
        int max = parseMaxActivityIndex(sub);
        int n = 0;
        for (int idx : inquiryIdx) {
            if (max >= idx) {
                n++;
            }
        }
        return n;
    }

    private int flowchartScoreOf(LearnSubmission sub) {
        JsonNode node = parseContentJson(sub);
        if (node == null || !node.has("flowchart")) {
            return -1;
        }
        if (node.has("flowchartScore")) {
            return node.get("flowchartScore").asInt(0);
        }
        if (node.has("userScore")) {
            return node.get("userScore").asInt(0);
        }
        return 0;
    }

    private int quizTotalOf(LearnSubmission sub) {
        if (!hasQuizSubmission(sub)) {
            return -1;
        }
        JsonNode node = parseContentJson(sub);
        if (node == null) {
            return -1;
        }
        int[] dims = parseQuizDimensionPoints(node);
        int sum = dims[0] + dims[1] + dims[2];
        if (sum > 0) {
            return sum;
        }
        if (node.has("quizScore")) {
            return normalizeQuizTotal(node.get("quizScore").asInt(0));
        }
        return 0;
    }

    private String suggestLessonTier(int inquiryDone, int inquiryTotal, int flowchartScore, int quizScore, String current) {
        if (inquiryDone == 0 && flowchartScore < 0 && quizScore < 0) {
            return current;
        }
        if (inquiryDone <= 1 || (flowchartScore >= 0 && flowchartScore < 50)) {
            return "C";
        }
        if (inquiryTotal > 0 && inquiryDone >= inquiryTotal && flowchartScore >= 80) {
            return "A";
        }
        return "B";
    }

    private String suggestReason(boolean hasData, String suggested, int inquiryDone, int flowchartScore) {
        if (!hasData) {
            return "暂无本课提交，建议沿用当前档";
        }
        if ("C".equals(suggested)) {
            if (inquiryDone <= 1) {
                return "探究完成较少，第2课需要更厚支架";
            }
            if (flowchartScore >= 0 && flowchartScore < 50) {
                return "流程图未达标，第2课需要更厚支架";
            }
            return "本课表现较弱，第2课建议加厚支架";
        }
        if ("A".equals(suggested)) {
            return "探究与流程图完成较好，第2课可挑战";
        }
        return "本课表现中等，第2课用课堂基准支架";
    }

    private void attachFlowchart(Map<String, Object> row, LearnSubmission sub, int index) {
        if (sub == null) {
            return;
        }
        JsonNode node = parseContentJson(sub);
        if (node == null || !node.has("flowchart")) {
            return;
        }
        try {
            row.put("flowchart", objectMapper.convertValue(node.get("flowchart"), Object.class));
        } catch (Exception ignored) {
            return;
        }
        int fcScore = node.path("flowchartScore").asInt(node.path("userScore").asInt(0));
        row.put("flowchartScore", fcScore);
        row.put("score", fcScore);
        row.put("flowchartPassed", node.path("flowchartPassed").asBoolean(false));
        row.put("flowchartVersion", node.path("flowchartVersion").asInt(1));
        if (node.has("flowchartChecks")) {
            try {
                row.put("flowchartChecks", objectMapper.convertValue(node.get("flowchartChecks"), Object.class));
            } catch (Exception ignored) {
            }
        }
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
