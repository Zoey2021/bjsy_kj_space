package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.dto.SubmitRequest;
import com.itech.learnspace.dto.VisitRequest;
import com.itech.learnspace.entity.*;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LearnService {

    private final LearnSubmissionRepository submissionRepository;
    private final LearnProgressRepository progressRepository;
    private final LearnVisitLogRepository visitLogRepository;
    private final LearnPointsRepository pointsRepository;
    private final CourseTaskRepository taskRepository;
    private final CourseLessonRepository lessonRepository;
    private final SysUserRepository userRepository;
    private final SysConfigRepository configRepository;
    private final LearnSubmissionLogRepository submissionLogRepository;
    private final DashboardService dashboardService;
    private final SseService sseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LearnService(LearnSubmissionRepository submissionRepository,
                        LearnProgressRepository progressRepository,
                        LearnVisitLogRepository visitLogRepository,
                        LearnPointsRepository pointsRepository,
                        CourseTaskRepository taskRepository,
                        CourseLessonRepository lessonRepository,
                        SysUserRepository userRepository,
                        SysConfigRepository configRepository,
                        LearnSubmissionLogRepository submissionLogRepository,
                        DashboardService dashboardService,
                        SseService sseService) {
        this.submissionRepository = submissionRepository;
        this.progressRepository = progressRepository;
        this.visitLogRepository = visitLogRepository;
        this.pointsRepository = pointsRepository;
        this.taskRepository = taskRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.configRepository = configRepository;
        this.submissionLogRepository = submissionLogRepository;
        this.dashboardService = dashboardService;
        this.sseService = sseService;
    }

    /**
     * 学生提交任务（核心接口）
     * 流程：保存提交 → 更新进度 → 加积分 → SSE推送看板更新
     */
    @Transactional
    public LearnSubmission submit(SubmitRequest req, Long studentId) {
        CourseTask task = taskRepository.findById(req.getTaskId())
                .orElseThrow(() -> new BusinessException("任务不存在"));

        LearnSubmission submission = submissionRepository
                .findByTaskIdAndStudentId(req.getTaskId(), studentId)
                .orElse(new LearnSubmission());
        boolean isFirstSubmit = submission.getId() == null;

        submission.setTaskId(req.getTaskId());
        submission.setStudentId(studentId);
        submission.setContentJson(req.getContentJson());
        submission.setStudySeconds(req.getStudySeconds() != null ? req.getStudySeconds() : 0);
        submission.setScore(resolveSubmissionScore(task, req.getContentJson()));
        submission.setStatus("SUBMITTED");
        submissionRepository.save(submission);

        appendSubmissionLog(studentId, task, req.getContentJson(), submission.getScore(),
                req.getStudySeconds() != null ? req.getStudySeconds() : 0);

        // 更新课时学习进度
        updateLessonProgress(studentId, task.getLessonId());

        // 每完成一个活动 +2 积分（同一活动仅奖励一次）
        awardActivityPoints(studentId, task.getLessonId(), req.getContentJson());

        // 非工作台任务：首次提交仍按任务奖励（兼容旧逻辑）
        if (isFirstSubmit && !isWorkspaceSubmission(req.getContentJson())) {
            addPoints(studentId, task.getId(), task.getTitle(), "TASK", task.getId(), pointsPerTask());
        }

        // 【关键】通过 SSE 实时通知教师看板刷新
        SysUser student = userRepository.findById(studentId).orElse(null);
        if (student != null && student.getClassId() != null) {
            sseService.broadcast(student.getClassId(),
                    dashboardService.buildDashboardData(student.getClassId()));
        }

        return submission;
    }

    /** 记录学生访问课时 */
    @Transactional
    public void recordVisit(VisitRequest req, Long studentId) {
        LearnVisitLog log = new LearnVisitLog();
        log.setStudentId(studentId);
        log.setLessonId(req.getLessonId());
        log.setPageUrl(req.getPageUrl());
        log.setDurationSec(req.getDurationSec() != null ? req.getDurationSec() : 0);
        visitLogRepository.save(log);

        upsertProgress(studentId, req.getLessonId(), req.getDurationSec());
    }

    /** 避免并发访问记录导致 uk_student_lesson 唯一键冲突 */
    private void upsertProgress(Long studentId, Long lessonId, Integer durationSec) {
        LearnProgress progress = progressRepository
                .findByStudentIdAndLessonId(studentId, lessonId)
                .orElse(null);
        if (progress == null) {
            progress = new LearnProgress();
            progress.setStudentId(studentId);
            progress.setLessonId(lessonId);
        }
        if (!"COMPLETED".equals(progress.getStatus())) {
            progress.setStatus("IN_PROGRESS");
        }
        progress.setLastVisitAt(LocalDateTime.now());
        if (durationSec != null) {
            int base = progress.getStudySeconds() != null ? progress.getStudySeconds() : 0;
            progress.setStudySeconds(base + durationSec);
        }
        try {
            progressRepository.save(progress);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            progressRepository.findByStudentIdAndLessonId(studentId, lessonId).ifPresent(existing -> {
                if (!"COMPLETED".equals(existing.getStatus())) {
                    existing.setStatus("IN_PROGRESS");
                }
                existing.setLastVisitAt(LocalDateTime.now());
                if (durationSec != null) {
                    int base = existing.getStudySeconds() != null ? existing.getStudySeconds() : 0;
                    existing.setStudySeconds(base + durationSec);
                }
                progressRepository.save(existing);
            });
        }
    }

    private int resolveSubmissionScore(CourseTask task, String contentJson) {
        if (!"EXTERNAL".equals(task.getTaskType()) || !StringUtils.hasText(contentJson)) {
            return task.getMaxScore() != null ? task.getMaxScore() : 0;
        }
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            if (node.has("userScore")) {
                return node.get("userScore").asInt();
            }
            if (node.has("quizScore")) {
                return node.get("quizScore").asInt();
            }
        } catch (Exception ignored) {
        }
        return task.getMaxScore() != null ? task.getMaxScore() : 0;
    }

    private void updateLessonProgress(Long studentId, Long lessonId) {
        List<CourseTask> tasks = taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
        if (tasks.isEmpty()) return;

        List<Long> taskIds = new java.util.ArrayList<Long>();
        for (CourseTask t : tasks) taskIds.add(t.getId());

        long submitted = submissionRepository.countByTaskIdInAndStudentIdIn(
                taskIds, java.util.Collections.singletonList(studentId));

        LearnProgress progress = progressRepository
                .findByStudentIdAndLessonId(studentId, lessonId)
                .orElse(new LearnProgress());
        if (progress.getId() == null) {
            progress.setStudentId(studentId);
            progress.setLessonId(lessonId);
        }

        int percent = (int) (submitted * 100 / tasks.size());
        progress.setProgressPercent(percent);
        progress.setStatus(submitted >= tasks.size() ? "COMPLETED" : "IN_PROGRESS");
        if ("COMPLETED".equals(progress.getStatus())) {
            progress.setCompletedAt(LocalDateTime.now());
        }
        progressRepository.save(progress);
    }

    private void addPoints(Long studentId, Long sourceId, String description, String sourceType,
                           Long pointsSourceId, int points) {
        LearnPoints record = new LearnPoints();
        record.setStudentId(studentId);
        record.setSourceType(sourceType);
        record.setSourceId(pointsSourceId);
        record.setPoints(points);
        record.setDescription(description);
        pointsRepository.save(record);
    }

    private int pointsPerTask() {
        int points = 5;
        SysConfig config = configRepository.findByConfigKey("points_per_task").orElse(null);
        if (config != null) {
            try {
                points = Integer.parseInt(config.getConfigValue());
            } catch (Exception ignored) {
            }
        }
        return points;
    }

    private void appendSubmissionLog(Long studentId, CourseTask task, String contentJson,
                                     Integer score, int studySeconds) {
        LearnSubmissionLog log = new LearnSubmissionLog();
        log.setStudentId(studentId);
        log.setLessonId(task.getLessonId());
        log.setTaskId(task.getId());
        log.setContentJson(contentJson != null ? contentJson : "{}");
        log.setScore(score);
        log.setStudySeconds(studySeconds);
        parseActivityMeta(contentJson, log);
        submissionLogRepository.save(log);
    }

    private void parseActivityMeta(String contentJson, LearnSubmissionLog log) {
        if (!StringUtils.hasText(contentJson)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            String type = node.path("type").asText("");
            if ("quiz".equals(type)) {
                log.setActivityKey("quiz");
                log.setActivityIndex(99);
            } else if ("evaluation".equals(type)) {
                log.setActivityKey("evaluation");
                log.setActivityIndex(98);
            } else if ("reading".equals(type)) {
                log.setActivityKey("reading");
                log.setActivityIndex(0);
            } else if (node.has("activityIndex")) {
                int idx = node.get("activityIndex").asInt();
                log.setActivityIndex(idx);
                log.setActivityKey("act-" + idx);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isWorkspaceSubmission(String contentJson) {
        if (!StringUtils.hasText(contentJson)) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            return node.has("activityIndex") || node.has("completedActivities")
                    || "quiz".equals(node.path("type").asText())
                    || "evaluation".equals(node.path("type").asText())
                    || "reading".equals(node.path("type").asText());
        } catch (Exception ignored) {
            return false;
        }
    }

    /** 每节课每完成一个活动奖励 2 积分 */
    private void awardActivityPoints(Long studentId, Long lessonId, String contentJson) {
        if (!StringUtils.hasText(contentJson)) {
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            String type = node.path("type").asText("");
            int activityIndex = node.path("activityIndex").asInt(0);

            if ("quiz".equals(type)) {
                grantActivityPointsOnce(studentId, lessonId, 9, "完成课堂小测");
                return;
            }
            if ("evaluation".equals(type)) {
                grantActivityPointsOnce(studentId, lessonId, 8, "完成学习评价");
                return;
            }
            if (activityIndex >= 1 && activityIndex <= 6 && node.path("stepCompleted").asBoolean(false)) {
                CourseLesson lesson = lessonRepository.findById(lessonId).orElse(null);
                String lessonTitle = lesson != null ? lesson.getTitle() : "课时";
                grantActivityPointsOnce(studentId, lessonId, activityIndex,
                        "完成探究" + activityIndex + "：" + lessonTitle);
            }
        } catch (Exception ignored) {
        }
    }

    private void grantActivityPointsOnce(Long studentId, Long lessonId, int slot, String desc) {
        long sourceId = lessonId * 100L + slot;
        if (pointsRepository.existsByStudentIdAndSourceTypeAndSourceId(studentId, "ACTIVITY", sourceId)) {
            return;
        }
        addPoints(studentId, sourceId, desc, "ACTIVITY", sourceId, 2);
    }

    /** 本课学习记录：活动完成情况 + 每次提交历史 */
    public java.util.Map<String, Object> getLessonRecords(Long lessonId, Long studentId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));

        java.util.Set<Integer> completed = new java.util.HashSet<Integer>();
        java.util.List<java.util.Map<String, Object>> activityRows = new java.util.ArrayList<java.util.Map<String, Object>>();

        for (CourseTask task : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if (!"EXTERNAL".equals(task.getTaskType())) {
                continue;
            }
            try {
                JsonNode cfg = objectMapper.readTree(task.getConfigJson() != null ? task.getConfigJson() : "{}");
                if (!"student_workspace".equals(cfg.path("layout").asText())) {
                    continue;
                }
                java.util.Map<String, Object> readingRow = new java.util.HashMap<String, Object>();
                readingRow.put("index", 0);
                readingRow.put("key", "reading");
                readingRow.put("title", "阅读教材");
                readingRow.put("type", "READING");
                activityRows.add(readingRow);

                if (cfg.has("activities")) {
                    for (JsonNode act : cfg.get("activities")) {
                        int idx = act.path("index").asInt();
                        java.util.Map<String, Object> row = new java.util.HashMap<String, Object>();
                        row.put("index", idx);
                        row.put("key", "act-" + idx);
                        row.put("title", act.path("title").asText("活动" + idx));
                        row.put("type", "INTERACTIVE");
                        activityRows.add(row);
                    }
                }
                java.util.Map<String, Object> evalRow = new java.util.HashMap<String, Object>();
                evalRow.put("index", 98);
                evalRow.put("key", "evaluation");
                evalRow.put("title", "学习评价");
                evalRow.put("type", "EVALUATION");
                activityRows.add(evalRow);

                java.util.Map<String, Object> quizRow = new java.util.HashMap<String, Object>();
                quizRow.put("index", 99);
                quizRow.put("key", "quiz");
                quizRow.put("title", "课堂小测");
                quizRow.put("type", "QUIZ");
                activityRows.add(quizRow);
            } catch (Exception ignored) {
            }
        }

        submissionRepository.findByTaskIdIn(
                taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId).stream()
                        .map(CourseTask::getId).collect(java.util.stream.Collectors.toList()))
                .stream()
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

        java.util.List<LearnSubmissionLog> logs =
                submissionLogRepository.findByStudentIdAndLessonIdOrderByCreatedAtDesc(studentId, lessonId);

        java.util.Map<String, java.util.List<java.util.Map<String, Object>>> logsByKey =
                new java.util.HashMap<String, java.util.List<java.util.Map<String, Object>>>();
        for (LearnSubmissionLog log : logs) {
            String key = log.getActivityKey() != null ? log.getActivityKey() : "other";
            logsByKey.computeIfAbsent(key, k -> new java.util.ArrayList<>());
            java.util.Map<String, Object> item = new java.util.HashMap<String, Object>();
            item.put("id", log.getId());
            item.put("activityIndex", log.getActivityIndex());
            item.put("score", log.getScore());
            item.put("createdAt", log.getCreatedAt());
            logsByKey.get(key).add(item);
        }

        for (java.util.Map<String, Object> row : activityRows) {
            String key = (String) row.get("key");
            java.util.List<java.util.Map<String, Object>> list = logsByKey.getOrDefault(key,
                    java.util.Collections.emptyList());
            row.put("submitCount", list.size());
            row.put("submitted", !list.isEmpty() || completed.contains(row.get("index")));
            row.put("logs", list);
        }

        java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
        result.put("lessonId", lessonId);
        result.put("lessonTitle", lesson.getTitle());
        result.put("activities", activityRows);
        result.put("submissionLogs", logs);
        result.put("totalPoints", pointsRepository.sumPointsByStudentId(studentId));
        result.put("pointsHistory", pointsRepository.findByStudentIdOrderByCreatedAtDesc(studentId));
        return result;
    }

    /** 学生个人学习记录 */
    public java.util.Map<String, Object> getMyRecords(Long studentId) {
        java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
        result.put("progress", progressRepository.findByStudentId(studentId));
        result.put("submissions", submissionRepository.findByStudentId(studentId));
        result.put("submissionLogs", submissionLogRepository.findByStudentIdOrderByCreatedAtDesc(studentId));
        result.put("totalPoints", pointsRepository.sumPointsByStudentId(studentId));
        result.put("pointsHistory", pointsRepository.findByStudentIdOrderByCreatedAtDesc(studentId));
        return result;
    }
}
