package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itech.learnspace.dto.SubmitRequest;
import com.itech.learnspace.dto.VisitRequest;
import com.itech.learnspace.entity.*;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String contentToStore = req.getContentJson();
        boolean draftSave = false;
        boolean progressSave = false;
        try {
            JsonNode draftNode = objectMapper.readTree(contentToStore == null ? "{}" : contentToStore);
            if (draftNode instanceof ObjectNode) {
                ObjectNode obj = (ObjectNode) draftNode;
                if (obj.path("draftSave").asBoolean(false)) {
                    draftSave = true;
                    obj.remove("draftSave");
                }
                if (obj.path("progressSave").asBoolean(false)) {
                    progressSave = true;
                    obj.remove("progressSave");
                    if ("progress".equals(obj.path("type").asText())) {
                        obj.remove("type");
                    }
                }
                contentToStore = objectMapper.writeValueAsString(obj);
            }
        } catch (Exception ignored) {
            draftSave = false;
            progressSave = false;
        }
        contentToStore = mergeKeptProgress(submission.getContentJson(), contentToStore);
        if (draftSave || progressSave) {
            submission.setContentJson(contentToStore);
            if (submission.getId() == null) {
                submission.setStudySeconds(0);
                submission.setStatus("SUBMITTED");
            }
            submissionRepository.save(submission);
            return submission;
        }

        submission.setContentJson(contentToStore);
        submission.setStudySeconds(req.getStudySeconds() != null ? req.getStudySeconds() : 0);
        submission.setScore(resolveSubmissionScore(task, contentToStore));
        submission.setStatus("SUBMITTED");
        submissionRepository.save(submission);

        appendSubmissionLog(studentId, task, contentToStore, submission.getScore(),
                req.getStudySeconds() != null ? req.getStudySeconds() : 0);

        // 更新课时学习进度
        updateLessonProgress(studentId, task.getLessonId());

        // 每完成一个活动 +2 积分（同一活动仅奖励一次）
        int awarded = awardActivityPoints(studentId, task.getLessonId(), req.getContentJson());
        submission.setPointsAwarded(awarded);
        submission.setEarnedPoints(nz(pointsRepository.sumEarnedByStudentId(studentId)));
        submission.setRedeemablePoints(nz(pointsRepository.sumPointsByStudentId(studentId)));

        // 非工作台任务：首次提交仍按任务奖励（兼容旧逻辑）
        if (isFirstSubmit && !isWorkspaceSubmission(req.getContentJson())) {
            addPoints(studentId, task.getId(), task.getTitle(), "TASK", task.getId(), pointsPerTask());
        }

        // 事务提交后再通知教师端刷新，避免学生提交时扫描全校课时
        SysUser student = userRepository.findById(studentId).orElse(null);
        if (student != null && student.getClassId() != null) {
            Long classId = student.getClassId();
            Long lessonId = task.getLessonId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    Map<String, Object> ping = new HashMap<String, Object>();
                    ping.put("refresh", Boolean.TRUE);
                    ping.put("classId", classId);
                    ping.put("lessonId", lessonId);
                    sseService.broadcast(classId, ping);
                }
            });
        }

        return submission;
    }

    /** 记录学生访问课时 */
    @Transactional
    public void recordVisit(VisitRequest req, Long studentId) {
        if (req == null || req.getLessonId() == null || studentId == null) {
            return;
        }
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
        if (studentId == null || lessonId == null) {
            return;
        }
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

    /** 新的保存不能把已经完成的探究、小测、评价从记录里清掉。 */
    private String mergeKeptProgress(String oldJson, String newJson) {
        try {
            JsonNode newNode = objectMapper.readTree(newJson == null ? "{}" : newJson);
            if (!(newNode instanceof ObjectNode)) {
                return newJson;
            }
            ObjectNode base = (ObjectNode) newNode;
            if (!StringUtils.hasText(oldJson)) {
                return objectMapper.writeValueAsString(base);
            }
            JsonNode oldNode = objectMapper.readTree(oldJson);
            if (!(oldNode instanceof ObjectNode)) {
                return objectMapper.writeValueAsString(base);
            }
            ObjectNode oldObj = (ObjectNode) oldNode;
            java.util.LinkedHashSet<Integer> done = new java.util.LinkedHashSet<Integer>();
            addCompletedIndexes(done, oldObj.get("completedActivities"));
            addCompletedIndexes(done, base.get("completedActivities"));
            if (base.path("stepCompleted").asBoolean(false)) {
                String type = base.path("type").asText("");
                int act = base.path("activityIndex").asInt(0);
                if (act > 0 && act < 10 && !"quiz".equals(type) && !"evaluation".equals(type)
                        && !"progress".equals(type) && !"handin".equals(type)) {
                    done.add(act);
                }
            }
            ArrayNode arr = objectMapper.createArrayNode();
            for (Integer idx : done) {
                arr.add(idx);
            }
            if (arr.size() > 0 || oldObj.has("completedActivities")) {
                base.set("completedActivities", arr);
            }
            copyIfAbsent(base, oldObj, "quizScore");
            copyIfAbsent(base, oldObj, "correctCount");
            copyIfAbsent(base, oldObj, "quizAnswers");
            copyIfAbsent(base, oldObj, "dimensionScores");
            copyIfAbsent(base, oldObj, "evaluationAnswers");
            copyIfAbsent(base, oldObj, "activityDrafts");
            if (!base.path("quizSubmitted").asBoolean(false) && quizKept(oldObj)) {
                base.put("quizSubmitted", true);
            }
            if (!base.path("evaluationSubmitted").asBoolean(false) && evalKept(oldObj)) {
                base.put("evaluationSubmitted", true);
            }
            if (!base.path("handedIn").asBoolean(false) && oldObj.path("handedIn").asBoolean(false)) {
                base.put("handedIn", true);
            }
            if (!base.path("readingCompleted").asBoolean(false) && oldObj.path("readingCompleted").asBoolean(false)) {
                base.put("readingCompleted", true);
            }
            return objectMapper.writeValueAsString(base);
        } catch (Exception ex) {
            return newJson;
        }
    }

    private void addCompletedIndexes(java.util.Set<Integer> done, JsonNode array) {
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

    private void copyIfAbsent(ObjectNode base, ObjectNode oldObj, String field) {
        if (!base.has(field) || base.get(field).isNull()) {
            if (oldObj.has(field) && !oldObj.get(field).isNull()) {
                base.set(field, oldObj.get(field));
            }
        }
    }

    private boolean quizKept(ObjectNode node) {
        return node.path("quizSubmitted").asBoolean(false) || node.has("quizScore")
                || node.has("dimensionScores") || "quiz".equals(node.path("type").asText());
    }

    private boolean evalKept(ObjectNode node) {
        return node.path("evaluationSubmitted").asBoolean(false)
                || "evaluation".equals(node.path("type").asText())
                || (node.has("evaluationAnswers") && node.get("evaluationAnswers").size() > 0);
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
        return readConfigInt("points_per_task", 5);
    }

    private int pointsPerActivity() {
        return readConfigInt("points_per_activity", 2);
    }

    private int extensionBonusPoints() {
        return readConfigInt("points_extension_bonus", 1);
    }

    private int readConfigInt(String key, int fallback) {
        SysConfig config = configRepository.findByConfigKey(key).orElse(null);
        if (config != null) {
            try {
                return Integer.parseInt(config.getConfigValue().trim());
            } catch (Exception ignored) {
            }
        }
        return fallback;
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

    /** 每完成一个探究、课堂小测或学习评价奖励 2 分；同一环节只奖一次。返回本次新加的分数。 */
    private int awardActivityPoints(Long studentId, Long lessonId, String contentJson) {
        if (!StringUtils.hasText(contentJson)) {
            return 0;
        }
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            String type = node.path("type").asText("");
            int activityIndex = node.path("activityIndex").asInt(0);

            if ("quiz".equals(type)) {
                return grantActivityPointsOnce(studentId, lessonId, 9, "完成课堂小测");
            }
            if ("evaluation".equals(type)) {
                return grantActivityPointsOnce(studentId, lessonId, 8, "完成学习评价");
            }
            if (activityIndex >= 1 && activityIndex <= 6 && node.path("stepCompleted").asBoolean(false)) {
                CourseLesson lesson = lessonRepository.findById(lessonId).orElse(null);
                String lessonTitle = lesson != null ? lesson.getTitle() : "课时";
                int gained = grantActivityPointsOnce(studentId, lessonId, activityIndex,
                        "完成探究" + activityIndex + "：" + lessonTitle);
                if (isExtensionActivity(lessonId, activityIndex)) {
                    gained += grantBonusOnce(studentId, lessonId, activityIndex,
                            "完整完成拓展任务：" + lessonTitle);
                }
                return gained;
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private int grantActivityPointsOnce(Long studentId, Long lessonId, int slot, String desc) {
        long sourceId = lessonId * 100L + slot;
        if (pointsRepository.existsByStudentIdAndSourceTypeAndSourceId(studentId, "ACTIVITY", sourceId)) {
            return 0;
        }
        int points = pointsPerActivity();
        addPoints(studentId, sourceId, desc, "ACTIVITY", sourceId, points);
        return points;
    }

    private int grantBonusOnce(Long studentId, Long lessonId, int slot, String desc) {
        int bonus = extensionBonusPoints();
        if (bonus <= 0) {
            return 0;
        }
        long sourceId = lessonId * 100L + slot;
        if (pointsRepository.existsByStudentIdAndSourceTypeAndSourceId(studentId, "BONUS", sourceId)) {
            return 0;
        }
        addPoints(studentId, sourceId, desc, "BONUS", sourceId, bonus);
        return bonus;
    }

    private boolean isExtensionActivity(Long lessonId, int activityIndex) {
        JsonNode activities = loadWorkspaceActivities(lessonId);
        if (activities == null || !activities.isArray() || activities.size() == 0) {
            return false;
        }
        JsonNode matched = null;
        for (JsonNode act : activities) {
            if (act.path("index").asInt(0) == activityIndex) {
                matched = act;
                break;
            }
        }
        if (matched == null) {
            return false;
        }
        return matched.path("title").asText("").contains("拓展");
    }

    private JsonNode loadWorkspaceActivities(Long lessonId) {
        for (CourseTask task : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if (!"EXTERNAL".equals(task.getTaskType()) || !StringUtils.hasText(task.getConfigJson())) {
                continue;
            }
            try {
                JsonNode cfg = objectMapper.readTree(task.getConfigJson());
                if ("student_workspace".equals(cfg.path("layout").asText()) && cfg.has("activities")) {
                    return cfg.get("activities");
                }
            } catch (Exception ignored) {
            }
        }
        return null;
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
                        if ("reading".equals(node.path("type").asText())
                                || node.path("readingCompleted").asBoolean(false)) {
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
        result.put("totalPoints", nz(pointsRepository.sumPointsByStudentId(studentId)));
        result.put("earnedPoints", nz(pointsRepository.sumEarnedByStudentId(studentId)));
        result.put("redeemablePoints", nz(pointsRepository.sumPointsByStudentId(studentId)));
        result.put("pointsHistory", pointsRepository.findByStudentIdOrderByCreatedAtDesc(studentId));
        return result;
    }

    /** 学生个人学习记录 */
    public java.util.Map<String, Object> getMyRecords(Long studentId) {
        java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
        result.put("progress", progressRepository.findByStudentId(studentId));
        result.put("submissions", submissionRepository.findByStudentId(studentId));
        result.put("submissionLogs", submissionLogRepository.findByStudentIdOrderByCreatedAtDesc(studentId));
        result.put("totalPoints", nz(pointsRepository.sumPointsByStudentId(studentId)));
        result.put("earnedPoints", nz(pointsRepository.sumEarnedByStudentId(studentId)));
        result.put("redeemablePoints", nz(pointsRepository.sumPointsByStudentId(studentId)));
        result.put("pointsHistory", pointsRepository.findByStudentIdOrderByCreatedAtDesc(studentId));
        return result;
    }

    private static int nz(Integer n) {
        return n == null ? 0 : n;
    }
}
