package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.dto.ParkReviewRequest;
import com.itech.learnspace.entity.*;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ParkService {

    static final String CLASS_OVER_MESSAGE = "下课时间到！请推进椅子，离开教室。";

    private static final Map<String, String> GAMES = new LinkedHashMap<String, String>();

    static {
        GAMES.put("snake", "贪吃蛇");
        GAMES.put("typing", "打字小能手");
        GAMES.put("memory", "记忆翻翻乐");
        GAMES.put("2048", "2048");
        GAMES.put("fruit", "接水果");
        GAMES.put("rope", "切绳接球");
    }

    private final LearnParkAccessRepository parkRepository;
    private final LearnParkPlayRepository playRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseTaskRepository taskRepository;
    private final LearnSubmissionRepository submissionRepository;
    private final LearnSubmissionLogRepository submissionLogRepository;
    private final DashboardService dashboardService;
    private final SseService sseService;
    private final LearnPointsRepository pointsRepository;

    static final int PARK_POINTS_PER_MINUTE = 2;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParkService(LearnParkAccessRepository parkRepository,
                       LearnParkPlayRepository playRepository,
                       JdbcTemplate jdbcTemplate,
                       SysUserRepository userRepository,
                       SysClassRepository classRepository,
                       CourseLessonRepository lessonRepository,
                       CourseTaskRepository taskRepository,
                       LearnSubmissionRepository submissionRepository,
                       LearnSubmissionLogRepository submissionLogRepository,
                       DashboardService dashboardService,
                       SseService sseService,
                       LearnPointsRepository pointsRepository) {
        this.parkRepository = parkRepository;
        this.playRepository = playRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.lessonRepository = lessonRepository;
        this.taskRepository = taskRepository;
        this.submissionRepository = submissionRepository;
        this.submissionLogRepository = submissionLogRepository;
        this.dashboardService = dashboardService;
        this.sseService = sseService;
        this.pointsRepository = pointsRepository;
    }

    @PostConstruct
    public void ensurePlayTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS learn_park_play ("
                + "id BIGINT NOT NULL AUTO_INCREMENT,"
                + "student_id BIGINT NOT NULL,"
                + "class_id BIGINT NOT NULL,"
                + "game_id VARCHAR(32) NOT NULL,"
                + "game_title VARCHAR(50) NOT NULL,"
                + "duration_seconds INT NOT NULL,"
                + "played_at DATETIME NOT NULL,"
                + "PRIMARY KEY (id),"
                + "KEY idx_park_play_class (class_id, played_at),"
                + "KEY idx_park_play_student (student_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        try {
            jdbcTemplate.execute("ALTER TABLE learn_park_play ADD COLUMN lesson_id BIGINT NULL");
        } catch (Exception ignored) {
            // 列已存在
        }
        try {
            jdbcTemplate.execute("ALTER TABLE learn_park_play ADD KEY idx_park_play_lesson (class_id, lesson_id, played_at)");
        } catch (Exception ignored) {
            // 索引已存在
        }
        try {
            jdbcTemplate.execute("ALTER TABLE learn_park_access DROP INDEX uk_park_student");
        } catch (Exception ignored) {
            // 旧的一人一条约束已经去掉
        }
        try {
            jdbcTemplate.execute("ALTER TABLE learn_park_access ADD UNIQUE KEY uk_park_student_lesson (student_id, lesson_id)");
        } catch (Exception ignored) {
            // 一人一课的约束已经存在
        }
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS learn_park_setting ("
                + "id INT NOT NULL,"
                + "config_json TEXT NOT NULL,"
                + "updated_at DATETIME NULL,"
                + "PRIMARY KEY (id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS learn_class_dismiss ("
                + "class_id BIGINT NOT NULL,"
                + "active TINYINT NOT NULL DEFAULT 0,"
                + "message VARCHAR(200) NULL,"
                + "updated_at DATETIME NULL,"
                + "PRIMARY KEY (class_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public Map<String, Object> getSettings() {
        return readConfig();
    }

    public Map<String, Object> saveSettings(Map<String, Object> body) {
        Map<String, Object> clean = normalizeConfig(body);
        try {
            String json = objectMapper.writeValueAsString(clean);
            jdbcTemplate.update("INSERT INTO learn_park_setting (id, config_json, updated_at) VALUES (1, ?, NOW()) "
                    + "ON DUPLICATE KEY UPDATE config_json = VALUES(config_json), updated_at = NOW()", json);
        } catch (Exception e) {
            throw new BusinessException("游戏设置保存失败");
        }
        return clean;
    }

    public Map<String, Object> getStatus(Long studentId) {
        SysUser student = userRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (student.getClassId() == null) {
            return withPoints(lockedStatus(null, null, false, "未加入班级，暂无法使用游学乐园"), studentId);
        }

        SysClass clazz = classRepository.findById(student.getClassId()).orElse(null);
        if (clazz == null) {
            return withPoints(lockedStatus(null, null, false, "班级信息不存在"), studentId);
        }

        Long lessonId = clazz.getCurrentLessonId();
        String lessonTitle = null;
        if (lessonId != null) {
            lessonTitle = lessonRepository.findById(lessonId).map(CourseLesson::getTitle).orElse(null);
        }

        boolean taskCompleted = lessonId != null && isLessonCompleted(studentId, lessonId);
        LearnParkAccess access = lessonId == null ? null
                : parkRepository.findByStudentIdAndLessonId(studentId, lessonId).orElse(null);
        String rawStatus = access != null ? access.getStatus() : "NONE";
        boolean approvedForCurrent = "APPROVED".equals(rawStatus);
        boolean pendingForCurrent = "PENDING".equals(rawStatus);

        Map<String, Object> result = new HashMap<>();
        result.put("currentLessonId", lessonId);
        result.put("currentLessonTitle", lessonTitle);
        result.put("taskCompleted", taskCompleted);
        result.put("accessStatus", rawStatus);
        result.put("unlocked", approvedForCurrent);
        result.put("reviewNote", access != null ? access.getReviewNote() : null);
        result.put("appliedAt", access != null ? access.getAppliedAt() : null);
        result.put("parkConfig", readConfig());

        if (lessonId == null) {
            result.put("canApply", false);
            result.put("message", "教师尚未设置当前课时，请先完成课堂学习任务");
            return withPoints(result, studentId);
        }
        if (!taskCompleted) {
            result.put("canApply", false);
            result.put("message", unfinishedMessage(studentId, lessonId, lessonTitle));
            return withPoints(result, studentId);
        }
        if (approvedForCurrent) {
            result.put("canApply", false);
            result.put("message", "教师已授权。游学乐园每分钟消耗 2 积分");
            return withPoints(result, studentId);
        }
        if (pendingForCurrent) {
            result.put("canApply", false);
            result.put("message", "申请已提交，等待教师授权开启");
            return withPoints(result, studentId);
        }

        result.put("canApply", true);
        if ("REJECTED".equals(rawStatus) || "REVOKED".equals(rawStatus)) {
            result.put("message", "当前课时任务已完成，可重新申请开启游学乐园");
        } else {
            result.put("message", "当前课时任务已完成，可向教师申请开启游学乐园");
        }
        return withPoints(result, studentId);
    }

    /** 教师查看游学乐园：游戏可以打开，不扣积分，也不写游玩记录。 */
    public Map<String, Object> previewStatus() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("preview", Boolean.TRUE);
        result.put("unlocked", Boolean.TRUE);
        result.put("canApply", Boolean.FALSE);
        result.put("taskCompleted", Boolean.TRUE);
        result.put("accessStatus", "PREVIEW");
        result.put("message", "教师查看。可以打开游戏看看，不会扣学生积分。");
        result.put("parkConfig", readConfig());
        result.put("redeemablePoints", 20);
        result.put("parkPointsPerMinute", PARK_POINTS_PER_MINUTE);
        result.put("parkMinutes", 20 / PARK_POINTS_PER_MINUTE);
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

        LearnParkAccess access = parkRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .orElse(new LearnParkAccess());
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

    public List<Map<String, Object>> listApplications(Long classId, Long lessonId, Long teacherId) {
        dashboardService.checkTeacherOwnsClass(teacherId, classId);
        if (lessonId == null) {
            return new ArrayList<Map<String, Object>>();
        }
        List<LearnParkAccess> list = parkRepository.findByClassIdAndLessonIdOrderByAppliedAtDesc(classId, lessonId);
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

        Long lessonId = req.getLessonId();
        if (lessonId == null && student.getClassId() != null) {
            SysClass clazz = classRepository.findById(student.getClassId()).orElse(null);
            lessonId = clazz == null ? null : clazz.getCurrentLessonId();
        }
        if (lessonId == null) {
            throw new BusinessException("请先选择课时");
        }
        final Long reviewLessonId = lessonId;
        LearnParkAccess access = parkRepository.findByStudentIdAndLessonId(req.getStudentId(), reviewLessonId)
                .orElseThrow(() -> new BusinessException("该学生尚未申请这一课的游学乐园"));

        if ("approve".equals(action)) {
            if (!isLessonCompleted(student.getId(), access.getLessonId())) {
                throw new BusinessException("学生还需完成全部探究活动、课堂小测和学习评价后才能授权");
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

    /** 一键授权或一键关闭指定课时的游学乐园，不影响其他课。 */
    @Transactional
    public Map<String, Object> reviewClass(Long classId, Long lessonId, String action, Long teacherId) {
        dashboardService.checkTeacherOwnsClass(teacherId, classId);
        if (!StringUtils.hasText(action)) {
            throw new BusinessException("参数不完整");
        }
        if (lessonId == null) {
            throw new BusinessException("请先选择课时");
        }
        lessonRepository.findById(lessonId).orElseThrow(() -> new BusinessException("课时不存在"));
        String act = action.trim().toLowerCase(Locale.ROOT);
        int changed = 0;
        int skipped = 0;

        if ("approve".equals(act)) {
            List<SysUser> students = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1);
            for (SysUser student : students) {
                if (!isLessonCompleted(student.getId(), lessonId)) {
                    skipped++;
                    continue;
                }
                LearnParkAccess access = parkRepository.findByStudentIdAndLessonId(student.getId(), lessonId)
                        .orElse(new LearnParkAccess());
                access.setStudentId(student.getId());
                access.setClassId(classId);
                access.setLessonId(lessonId);
                access.setStatus("APPROVED");
                if (access.getAppliedAt() == null) {
                    access.setAppliedAt(LocalDateTime.now());
                }
                access.setReviewedAt(LocalDateTime.now());
                access.setReviewerId(teacherId);
                access.setReviewNote("教师一键授权");
                parkRepository.save(access);
                pushPark(student.getId(), "approve", lessonId);
                changed++;
            }
            markClassDismiss(classId, false);
            List<Long> studentIds = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1)
                    .stream().map(SysUser::getId).collect(Collectors.toList());
            sseService.pushToStudents(studentIds, "class_dismiss_clear", new HashMap<String, Object>());
        } else if ("revoke".equals(act)) {
            List<LearnParkAccess> approved = parkRepository
                    .findByClassIdAndLessonIdAndStatusOrderByAppliedAtAsc(classId, lessonId, "APPROVED");
            for (LearnParkAccess access : approved) {
                access.setStatus("REVOKED");
                access.setReviewedAt(LocalDateTime.now());
                access.setReviewerId(teacherId);
                access.setReviewNote("教师一键关闭");
                parkRepository.save(access);
                pushPark(access.getStudentId(), "revoke", lessonId);
                changed++;
            }
            markClassDismiss(classId, false);
            List<Long> studentIds = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1)
                    .stream().map(SysUser::getId).collect(Collectors.toList());
            sseService.pushToStudents(studentIds, "class_dismiss_clear", new HashMap<String, Object>());
        } else {
            throw new BusinessException("不支持的操作");
        }

        Map<String, Object> out = new HashMap<String, Object>();
        out.put("action", act);
        out.put("changed", changed);
        out.put("skipped", skipped);
        return out;
    }

    @Transactional
    public Map<String, Object> recordPlay(Long studentId, String gameId, Integer durationSeconds) {
        Map<String, Object> status = getStatus(studentId);
        if (!Boolean.TRUE.equals(status.get("unlocked"))) {
            throw new BusinessException("游学乐园尚未开启");
        }
        String title = GAMES.get(gameId);
        if (title == null) {
            throw new BusinessException("未知的游戏");
        }
        if (!gameOpen(gameId)) {
            throw new BusinessException("这个游戏已关闭");
        }
        int seconds = durationSeconds == null ? 0 : durationSeconds;
        if (seconds < 1) {
            Map<String, Object> ignored = new HashMap<String, Object>();
            ignored.put("saved", false);
            return ignored;
        }
        if (seconds > 4 * 60 * 60) {
            seconds = 4 * 60 * 60;
        }
        int balance = remainingPoints(studentId);
        if (balance < 0) {
            balance = 0;
        }
        int affordable = balance / PARK_POINTS_PER_MINUTE;
        if (affordable < 1) {
            throw new BusinessException("积分不够玩 1 分钟，还差 " + (PARK_POINTS_PER_MINUTE - balance) + " 分");
        }
        int wanted = (seconds + 59) / 60;
        int minutes = Math.min(wanted, affordable);
        int charged = minutes * PARK_POINTS_PER_MINUTE;
        int billedSeconds = Math.min(seconds, minutes * 60);
        SysUser student = userRepository.findById(studentId).orElseThrow(() -> new BusinessException("用户不存在"));
        Object lid = status.get("currentLessonId");
        Long lessonId = lid == null ? null : Long.valueOf(String.valueOf(lid));
        LearnParkPlay play = new LearnParkPlay();
        play.setStudentId(studentId);
        play.setClassId(student.getClassId());
        play.setLessonId(lessonId);
        play.setGameId(gameId);
        play.setGameTitle(title);
        play.setDurationSeconds(billedSeconds);
        play.setPlayedAt(LocalDateTime.now());
        play = playRepository.save(play);

        LearnPoints bill = new LearnPoints();
        bill.setStudentId(studentId);
        bill.setSourceType("PARK");
        bill.setSourceId(play.getId());
        bill.setPoints(-charged);
        bill.setDescription("游学乐园：" + title + " " + minutes + " 分钟");
        pointsRepository.save(bill);

        int left = remainingPoints(studentId);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("saved", true);
        out.put("durationSeconds", billedSeconds);
        out.put("minutes", minutes);
        out.put("charged", charged);
        out.put("redeemablePoints", left);
        out.put("parkMinutes", left / PARK_POINTS_PER_MINUTE);
        return out;
    }

    private int remainingPoints(Long studentId) {
        Integer n = pointsRepository.sumPointsByStudentId(studentId);
        return n == null ? 0 : n;
    }

    private Map<String, Object> withPoints(Map<String, Object> result, Long studentId) {
        int remaining = remainingPoints(studentId);
        if (remaining < 0) {
            remaining = 0;
        }
        result.put("redeemablePoints", remaining);
        result.put("parkPointsPerMinute", PARK_POINTS_PER_MINUTE);
        result.put("parkMinutes", remaining / PARK_POINTS_PER_MINUTE);
        return result;
    }

    public Map<String, Object> playStats(Long classId, Long lessonId, Long teacherId) {
        dashboardService.checkTeacherOwnsClass(teacherId, classId);
        List<Map<String, Object>> summary = new ArrayList<Map<String, Object>>();
        List<LearnParkPlay> recentPlays = new ArrayList<LearnParkPlay>();
        if (lessonId != null) {
            for (Object[] row : playRepository.summarizeByClassAndLesson(classId, lessonId)) {
                long players = toLong(row[2]);
                long sessions = toLong(row[3]);
                long total = toLong(row[4]);
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("gameId", row[0]);
                item.put("gameTitle", row[1]);
                item.put("playerCount", players);
                item.put("sessionCount", sessions);
                item.put("totalSeconds", total);
                item.put("avgSeconds", players == 0 ? 0 : Math.round((double) total / players));
                summary.add(item);
            }
            recentPlays = playRepository.findTop40ByClassIdAndLessonIdOrderByPlayedAtDesc(classId, lessonId);
        }

        Map<Long, SysUser> students = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        List<Map<String, Object>> recent = new ArrayList<Map<String, Object>>();
        for (LearnParkPlay play : recentPlays) {
            SysUser stu = students.get(play.getStudentId());
            if (stu == null) {
                stu = userRepository.findById(play.getStudentId()).orElse(null);
            }
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("realName", stu != null ? stu.getRealName() : ("学生" + play.getStudentId()));
            item.put("gameTitle", play.getGameTitle());
            item.put("durationSeconds", play.getDurationSeconds());
            item.put("playedAt", play.getPlayedAt());
            recent.add(item);
        }

        Map<String, Object> out = new HashMap<String, Object>();
        out.put("summary", summary);
        out.put("recent", recent);
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
        result.put("parkConfig", readConfig());
        return result;
    }

    private Map<String, Object> readConfig() {
        try {
            List<String> rows = jdbcTemplate.query(
                    "SELECT config_json FROM learn_park_setting WHERE id = 1",
                    (rs, rowNum) -> rs.getString(1));
            if (!rows.isEmpty() && StringUtils.hasText(rows.get(0))) {
                JsonNode node = objectMapper.readTree(rows.get(0));
                return normalizeConfig(objectMapper.convertValue(node, Map.class));
            }
        } catch (Exception ignored) {
            // 配置读失败时用默认参数，不挡住学生进入乐园
        }
        return normalizeConfig(null);
    }

    private boolean gameOpen(String gameId) {
        Object games = readConfig().get("games");
        if (!(games instanceof Map)) {
            return true;
        }
        return !Boolean.FALSE.equals(((Map<?, ?>) games).get(gameId));
    }

    private Map<String, Object> normalizeConfig(Map<String, Object> body) {
        Map<String, Object> games = new LinkedHashMap<String, Object>();
        Object rawGames = body == null ? null : body.get("games");
        Map<?, ?> given = rawGames instanceof Map ? (Map<?, ?>) rawGames : Collections.emptyMap();
        for (String id : GAMES.keySet()) {
            games.put(id, !Boolean.FALSE.equals(given.get(id)));
        }
        int lives = 3;
        if (body != null && body.get("fruitLives") != null) {
            try {
                lives = Integer.parseInt(String.valueOf(body.get("fruitLives")));
            } catch (NumberFormatException ignored) {
                lives = 3;
            }
        }
        if (lives < 1) lives = 1;
        if (lives > 8) lives = 8;
        String speed = body == null || body.get("fruitSpeed") == null ? "normal" : String.valueOf(body.get("fruitSpeed"));
        if (!"slow".equals(speed) && !"fast".equals(speed)) speed = "normal";
        int pairs = 8;
        if (body != null && body.get("memoryPairs") != null) {
            try {
                pairs = Integer.parseInt(String.valueOf(body.get("memoryPairs")));
            } catch (NumberFormatException ignored) {
                pairs = 8;
            }
        }
        if (pairs != 4 && pairs != 6) pairs = 8;
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("games", games);
        out.put("fruitLives", lives);
        out.put("fruitSpeed", speed);
        out.put("memoryPairs", pairs);
        return out;
    }

    private void pushPark(Long studentId, String action, Long lessonId) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("type", "park_" + action);
        payload.put("status", "approve".equals(action) ? "APPROVED" : "REVOKED");
        payload.put("message", parkNotifyMessage(action));
        payload.put("lessonId", lessonId);
        sseService.pushToStudent(studentId, "park_" + action, payload);
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
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

    public Map<String, Object> classDismissStatus(Long studentId) {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("active", Boolean.FALSE);
        out.put("message", "");
        return out;
    }

    /**
     * 游学乐园可申请的条件：当节课的探究活动、课堂小测、学习评价都已提交。
     * 只交过其中一项，或课时进度被标成已完成，都不算完成。
     */
    boolean isLessonCompleted(Long studentId, Long lessonId) {
        return missingParts(studentId, lessonId).isEmpty();
    }

    private String unfinishedMessage(Long studentId, Long lessonId, String lessonTitle) {
        List<String> missing = missingParts(studentId, lessonId);
        if (missing.isEmpty()) {
            return "当前课时任务已完成，可向教师申请开启游学乐园";
        }
        String title = StringUtils.hasText(lessonTitle) ? "「" + lessonTitle + "」的" : "";
        return "请先完成当前课程" + title + String.join("、", missing) + "后再申请";
    }

    private List<String> missingParts(Long studentId, Long lessonId) {
        List<String> missing = new ArrayList<String>();
        if (studentId == null || lessonId == null) {
            missing.add("全部探究活动、课堂小测和学习评价");
            return missing;
        }
        List<CourseTask> tasks = taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
        if (tasks.isEmpty()) {
            missing.add("全部探究活动、课堂小测和学习评价");
            return missing;
        }
        for (CourseTask task : tasks) {
            if (!"EXTERNAL".equals(task.getTaskType())) {
                continue;
            }
            try {
                JsonNode cfg = objectMapper.readTree(task.getConfigJson() != null ? task.getConfigJson() : "{}");
                if (!"student_workspace".equals(cfg.path("layout").asText())) {
                    continue;
                }
                Set<Integer> required = new HashSet<Integer>();
                if (cfg.has("activities")) {
                    for (JsonNode act : cfg.get("activities")) {
                        int idx = act.path("index").asInt(0);
                        if (idx > 0) {
                            required.add(idx);
                        }
                    }
                }
                boolean requireQuiz = hasQuiz(cfg);
                boolean requireEval = hasEvaluation(cfg);
                Set<Integer> done = collectCompletedActivities(studentId, lessonId, tasks);
                if (!required.isEmpty() && !done.containsAll(required)) {
                    missing.add("全部探究活动");
                }
                if (requireQuiz && !done.contains(99)) {
                    missing.add("课堂小测");
                }
                if (requireEval && !done.contains(98)) {
                    missing.add("学习评价");
                }
                if (required.isEmpty() && !requireQuiz && !requireEval) {
                    missing.add("全部探究活动、课堂小测和学习评价");
                }
                return missing;
            } catch (Exception ignored) {
            }
        }
        List<Long> taskIds = tasks.stream().map(CourseTask::getId).collect(Collectors.toList());
        long submitted = submissionRepository.countByTaskIdInAndStudentIdIn(
                taskIds, Collections.singletonList(studentId));
        if (submitted < tasks.size()) {
            missing.add("全部任务");
        }
        return missing;
    }

    private boolean hasQuiz(JsonNode cfg) {
        JsonNode quiz = cfg.get("quiz");
        if (quiz == null || quiz.isNull()) {
            return false;
        }
        if (quiz.isArray()) {
            return quiz.size() > 0;
        }
        return quiz.path("questions").size() > 0;
    }

    private boolean hasEvaluation(JsonNode cfg) {
        JsonNode form = cfg.get("evaluationForm");
        return form != null && !form.isNull() && form.size() > 0;
    }

    private void markClassDismiss(Long classId, boolean active) {
        jdbcTemplate.update("INSERT INTO learn_class_dismiss (class_id, active, message, updated_at) "
                        + "VALUES (?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE active = VALUES(active), "
                        + "message = VALUES(message), updated_at = NOW()",
                classId, active ? 1 : 0, CLASS_OVER_MESSAGE);
    }

    private boolean isClassDismissed(Long classId) {
        if (classId == null) {
            return false;
        }
        try {
            List<Integer> rows = jdbcTemplate.query(
                    "SELECT active FROM learn_class_dismiss WHERE class_id = ? "
                            + "AND active = 1 AND updated_at > DATE_SUB(NOW(), INTERVAL 4 HOUR)",
                    (rs, rowNum) -> rs.getInt(1), classId);
            return !rows.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private Set<Integer> collectCompletedActivities(Long studentId, Long lessonId, List<CourseTask> tasks) {
        Set<Integer> completed = new HashSet<Integer>();
        List<Long> taskIds = tasks.stream().map(CourseTask::getId).collect(Collectors.toList());
        for (LearnSubmission sub : submissionRepository.findByTaskIdIn(taskIds)) {
            if (!studentId.equals(sub.getStudentId()) || !StringUtils.hasText(sub.getContentJson())) {
                continue;
            }
            try {
                absorbCompletion(completed, objectMapper.readTree(sub.getContentJson()));
            } catch (Exception ignored) {
            }
        }

        for (LearnSubmissionLog log : submissionLogRepository.findByStudentIdAndLessonIdOrderByCreatedAtDesc(studentId, lessonId)) {
            if (log.getActivityIndex() != null && log.getActivityIndex() > 0) {
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
            if (StringUtils.hasText(log.getContentJson())) {
                try {
                    absorbCompletion(completed, objectMapper.readTree(log.getContentJson()));
                } catch (Exception ignored) {
                }
            }
        }
        return completed;
    }

    /** 正式提交里记下的探究序号，以及草稿覆盖后仍留在提交原文里的完成列表。 */
    private void absorbCompletion(Set<Integer> completed, JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.has("completedActivities") && node.get("completedActivities").isArray()) {
            for (JsonNode n : node.get("completedActivities")) {
                int idx = n.asInt(0);
                if (idx > 0) {
                    completed.add(idx);
                }
            }
        }
        String type = node.path("type").asText("");
        int idx = node.path("activityIndex").asInt(0);
        if ("quiz".equals(type) || node.has("quizScore") || node.has("dimensionScores")
                || node.path("quizSubmitted").asBoolean(false)) {
            completed.add(99);
        }
        if ("evaluation".equals(type)
                || node.path("evaluationSubmitted").asBoolean(false)
                || (node.has("evaluationAnswers") && node.get("evaluationAnswers").size() > 0)) {
            completed.add(98);
        }
        if (!"quiz".equals(type) && !"evaluation".equals(type) && idx > 0 && idx < 10) {
            completed.add(idx);
        }
        if ("reading".equals(type)) {
            completed.add(0);
        }
    }
}
