package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.dto.ParkReviewRequest;
import com.itech.learnspace.entity.*;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ParkService {

    private final LearnParkAccessRepository parkRepository;
    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final CourseLessonRepository lessonRepository;
    private final LearnProgressRepository progressRepository;
    private final CourseTaskRepository taskRepository;
    private final LearnSubmissionRepository submissionRepository;
    private final LearnSubmissionLogRepository submissionLogRepository;
    private final DashboardService dashboardService;
    private final SseService sseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParkService(LearnParkAccessRepository parkRepository,
                       SysUserRepository userRepository,
                       SysClassRepository classRepository,
                       CourseLessonRepository lessonRepository,
                       LearnProgressRepository progressRepository,
                       CourseTaskRepository taskRepository,
                       LearnSubmissionRepository submissionRepository,
                       LearnSubmissionLogRepository submissionLogRepository,
                       DashboardService dashboardService,
                       SseService sseService) {
        this.parkRepository = parkRepository;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.taskRepository = taskRepository;
        this.submissionRepository = submissionRepository;
        this.submissionLogRepository = submissionLogRepository;
        this.dashboardService = dashboardService;
        this.sseService = sseService;
    }

    public Map<String, Object> getStatus(Long studentId) {
        SysUser student = userRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (student.getClassId() == null) {
            return lockedStatus(null, null, false, "未加入班级，暂无法使用游学乐园");
        }

        SysClass clazz = classRepository.findById(student.getClassId()).orElse(null);
        if (clazz == null) {
            return lockedStatus(null, null, false, "班级信息不存在");
        }

        Long lessonId = clazz.getCurrentLessonId();
        String lessonTitle = null;
        if (lessonId != null) {
            lessonTitle = lessonRepository.findById(lessonId).map(CourseLesson::getTitle).orElse(null);
        }

        boolean taskCompleted = lessonId != null && isLessonCompleted(studentId, lessonId);
        LearnParkAccess access = parkRepository.findByStudentId(studentId).orElse(null);
        String rawStatus = access != null ? access.getStatus() : "NONE";
        boolean sameLesson = access != null && Objects.equals(access.getLessonId(), lessonId) && lessonId != null;

        // 授权绑定当前课时：课时变更后需重新完成并申请
        boolean approvedForCurrent = "APPROVED".equals(rawStatus) && sameLesson;
        boolean pendingForCurrent = "PENDING".equals(rawStatus) && sameLesson;
        String accessStatus = approvedForCurrent ? "APPROVED"
                : pendingForCurrent ? "PENDING"
                : (!sameLesson && access != null ? "NONE" : rawStatus);

        Map<String, Object> result = new HashMap<>();
        result.put("currentLessonId", lessonId);
        result.put("currentLessonTitle", lessonTitle);
        result.put("taskCompleted", taskCompleted);
        result.put("accessStatus", accessStatus);
        result.put("unlocked", approvedForCurrent);
        result.put("reviewNote", access != null && sameLesson ? access.getReviewNote() : null);
        result.put("appliedAt", access != null && sameLesson ? access.getAppliedAt() : null);

        if (lessonId == null) {
            result.put("canApply", false);
            result.put("message", "教师尚未设置当前课时，请先完成课堂学习任务");
            return result;
        }
        if (!taskCompleted) {
            result.put("canApply", false);
            result.put("message", "请先完成当前课程「" + (lessonTitle != null ? lessonTitle : "") + "」的全部任务后再申请");
            return result;
        }
        if (approvedForCurrent) {
            result.put("canApply", false);
            result.put("message", "教师已授权，尽情畅游游学乐园吧");
            return result;
        }
        if (pendingForCurrent) {
            result.put("canApply", false);
            result.put("message", "申请已提交，等待教师授权开启");
            return result;
        }

        result.put("canApply", true);
        if ("REJECTED".equals(accessStatus) || "REVOKED".equals(accessStatus)) {
            result.put("message", "当前课时任务已完成，可重新申请开启游学乐园");
        } else {
            result.put("message", "当前课时任务已完成，可向教师申请开启游学乐园");
        }
        return result;
    }

    @Transactional
    public Map<String, Object> apply(Long studentId) {
        Map<String, Object> status = getStatus(studentId);
        if (!Boolean.TRUE.equals(status.get("canApply"))) {
            throw new BusinessException(String.valueOf(status.get("message")));
        }

        SysUser student = userRepository.findById(studentId).orElseThrow(() -> new BusinessException("用户不存在"));
        Long lessonId = (Long) status.get("currentLessonId");
        if (lessonId == null && status.get("currentLessonId") != null) {
            lessonId = Long.valueOf(String.valueOf(status.get("currentLessonId")));
        }

        LearnParkAccess access = parkRepository.findByStudentId(studentId).orElse(new LearnParkAccess());
        access.setStudentId(studentId);
        access.setClassId(student.getClassId());
        access.setLessonId(lessonId);
        access.setStatus("PENDING");
        access.setAppliedAt(LocalDateTime.now());
        access.setReviewedAt(null);
        access.setReviewerId(null);
        access.setReviewNote(null);
        parkRepository.save(access);

        Map<String, Object> out = getStatus(studentId);
        out.put("applied", true);
        return out;
    }

    public List<Map<String, Object>> listApplications(Long classId, Long teacherId) {
        dashboardService.checkTeacherOwnsClass(teacherId, classId);
        List<LearnParkAccess> list = parkRepository.findByClassIdOrderByAppliedAtDesc(classId);
        Map<Long, SysUser> students = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (LearnParkAccess a : list) {
            SysUser stu = students.get(a.getStudentId());
            if (stu == null) {
                stu = userRepository.findById(a.getStudentId()).orElse(null);
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", a.getId());
            row.put("studentId", a.getStudentId());
            row.put("realName", stu != null ? stu.getRealName() : ("学生" + a.getStudentId()));
            row.put("username", stu != null ? stu.getUsername() : "");
            row.put("lessonId", a.getLessonId());
            if (a.getLessonId() != null) {
                lessonRepository.findById(a.getLessonId()).ifPresent(l -> row.put("lessonTitle", l.getTitle()));
            }
            row.put("status", a.getStatus());
            row.put("appliedAt", a.getAppliedAt());
            row.put("reviewedAt", a.getReviewedAt());
            row.put("reviewNote", a.getReviewNote());
            boolean completed = a.getLessonId() != null && isLessonCompleted(a.getStudentId(), a.getLessonId());
            row.put("taskCompleted", completed);
            rows.add(row);
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> review(ParkReviewRequest req, Long teacherId) {
        if (req.getStudentId() == null || !StringUtils.hasText(req.getAction())) {
            throw new BusinessException("参数不完整");
        }
        String action = req.getAction().trim().toLowerCase(Locale.ROOT);
        if (!Arrays.asList("approve", "reject", "revoke").contains(action)) {
            throw new BusinessException("不支持的操作");
        }

        SysUser student = userRepository.findById(req.getStudentId())
                .orElseThrow(() -> new BusinessException("学生不存在"));
        if (student.getClassId() == null) {
            throw new BusinessException("学生未加入班级");
        }
        dashboardService.checkTeacherOwnsClass(teacherId, student.getClassId());

        LearnParkAccess access = parkRepository.findByStudentId(req.getStudentId())
                .orElseThrow(() -> new BusinessException("该学生尚未申请游学乐园"));

        if ("approve".equals(action)) {
            if (!isLessonCompleted(student.getId(), access.getLessonId())) {
                throw new BusinessException("学生尚未完成对应课时任务，暂不可授权");
            }
            access.setStatus("APPROVED");
        } else if ("reject".equals(action)) {
            access.setStatus("REJECTED");
        } else {
            access.setStatus("REVOKED");
        }
        access.setReviewedAt(LocalDateTime.now());
        access.setReviewerId(teacherId);
        access.setReviewNote(req.getNote());
        parkRepository.save(access);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "park_" + action);
        payload.put("status", access.getStatus());
        payload.put("message", parkNotifyMessage(action));
        payload.put("lessonId", access.getLessonId());
        sseService.pushToStudent(student.getId(), "park_" + action, payload);

        Map<String, Object> out = new HashMap<>();
        out.put("studentId", student.getId());
        out.put("status", access.getStatus());
        out.put("reviewedAt", access.getReviewedAt());
        return out;
    }

    public long countPending(Long classId, Long teacherId) {
        dashboardService.checkTeacherOwnsClass(teacherId, classId);
        return parkRepository.countByClassIdAndStatus(classId, "PENDING");
    }

    private Map<String, Object> lockedStatus(Long lessonId, String lessonTitle, boolean completed, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("currentLessonId", lessonId);
        result.put("currentLessonTitle", lessonTitle);
        result.put("taskCompleted", completed);
        result.put("accessStatus", "NONE");
        result.put("unlocked", false);
        result.put("canApply", false);
        result.put("message", message);
        return result;
    }

    private String parkNotifyMessage(String action) {
        if ("approve".equals(action)) {
            return "教师已开启游学乐园，可以去玩啦";
        }
        if ("reject".equals(action)) {
            return "游学乐园申请未通过，请继续完成课堂任务";
        }
        return "教师已关闭游学乐园访问权限";
    }

    /**
     * 判断当前课时任务是否完成：
     * 1) learn_progress 为 COMPLETED；或
     * 2) 学生工作区活动（阅读/探究/评价/小测）均已提交；或
     * 3) 普通任务全部提交。
     */
    boolean isLessonCompleted(Long studentId, Long lessonId) {
        if (studentId == null || lessonId == null) {
            return false;
        }
        Optional<LearnProgress> progress = progressRepository.findByStudentIdAndLessonId(studentId, lessonId);
        if (progress.isPresent() && "COMPLETED".equals(progress.get().getStatus())) {
            return true;
        }

        List<CourseTask> tasks = taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
        if (tasks.isEmpty()) {
            return false;
        }

        // 学生工作区：检查活动提交
        for (CourseTask task : tasks) {
            if (!"EXTERNAL".equals(task.getTaskType())) {
                continue;
            }
            try {
                JsonNode cfg = objectMapper.readTree(task.getConfigJson() != null ? task.getConfigJson() : "{}");
                if (!"student_workspace".equals(cfg.path("layout").asText())) {
                    continue;
                }
                Set<Integer> required = new HashSet<>();
                if (cfg.has("activities")) {
                    for (JsonNode act : cfg.get("activities")) {
                        int idx = act.path("index").asInt(0);
                        if (idx > 0) {
                            required.add(idx);
                        }
                    }
                }
                // 无配置活动时：阅读教材 + 至少一次有效提交视为完成
                if (required.isEmpty()) {
                    required.add(0);
                }

                Set<Integer> done = collectCompletedActivities(studentId, lessonId, tasks);
                return !required.isEmpty() && done.containsAll(required);
            } catch (Exception ignored) {
            }
        }

        List<Long> taskIds = tasks.stream().map(CourseTask::getId).collect(Collectors.toList());
        long submitted = submissionRepository.countByTaskIdInAndStudentIdIn(
                taskIds, Collections.singletonList(studentId));
        return submitted >= tasks.size();
    }

    private Set<Integer> collectCompletedActivities(Long studentId, Long lessonId, List<CourseTask> tasks) {
        Set<Integer> completed = new HashSet<>();
        List<Long> taskIds = tasks.stream().map(CourseTask::getId).collect(Collectors.toList());
        submissionRepository.findByTaskIdIn(taskIds).stream()
                .filter(s -> studentId.equals(s.getStudentId()))
                .findFirst()
                .ifPresent(sub -> {
                    try {
                        JsonNode node = objectMapper.readTree(sub.getContentJson());
                        if (node.has("completedActivities")) {
                            for (JsonNode n : node.get("completedActivities")) {
                                completed.add(n.asInt());
                            }
                        }
                        int idx = node.path("activityIndex").asInt(0);
                        if (idx > 0 && idx < 10) {
                            completed.add(idx);
                        }
                        if (node.has("quizScore") || "quiz".equals(node.path("type").asText())) {
                            completed.add(99);
                        }
                        if ("reading".equals(node.path("type").asText())) {
                            completed.add(0);
                        }
                        if ("evaluation".equals(node.path("type").asText()) || node.has("evaluationAnswers")) {
                            completed.add(98);
                        }
                    } catch (Exception ignored) {
                    }
                });

        for (LearnSubmissionLog log : submissionLogRepository.findByStudentIdAndLessonIdOrderByCreatedAtDesc(studentId, lessonId)) {
            if (log.getActivityIndex() != null) {
                completed.add(log.getActivityIndex());
            }
            if ("reading".equals(log.getActivityKey())) {
                completed.add(0);
            }
            if ("evaluation".equals(log.getActivityKey())) {
                completed.add(98);
            }
            if ("quiz".equals(log.getActivityKey())) {
                completed.add(99);
            }
            if (log.getActivityKey() != null && log.getActivityKey().startsWith("act-")) {
                try {
                    completed.add(Integer.parseInt(log.getActivityKey().substring(4)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return completed;
    }
}
