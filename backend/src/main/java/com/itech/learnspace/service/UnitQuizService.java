package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.entity.CourseLesson;
import com.itech.learnspace.entity.CourseTask;
import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.entity.UnitQuizSubmission;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.CourseLessonRepository;
import com.itech.learnspace.repository.CourseTaskRepository;
import com.itech.learnspace.repository.SysClassRepository;
import com.itech.learnspace.repository.SysUserRepository;
import com.itech.learnspace.repository.UnitQuizSubmissionRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UnitQuizService {

    private final JdbcTemplate jdbcTemplate;
    private final UnitQuizSubmissionRepository submissionRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseTaskRepository taskRepository;
    private final SysClassRepository classRepository;
    private final SysUserRepository userRepository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    public UnitQuizService(JdbcTemplate jdbcTemplate,
                           UnitQuizSubmissionRepository submissionRepository,
                           CourseLessonRepository lessonRepository,
                           CourseTaskRepository taskRepository,
                           SysClassRepository classRepository,
                           SysUserRepository userRepository,
                           DashboardService dashboardService,
                           ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.submissionRepository = submissionRepository;
        this.lessonRepository = lessonRepository;
        this.taskRepository = taskRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void ensureSchema() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS unit_quiz_submission ("
                + "id BIGINT NOT NULL AUTO_INCREMENT,"
                + "lesson_id BIGINT NOT NULL,"
                + "quiz_key VARCHAR(64) NULL,"
                + "student_id BIGINT NOT NULL,"
                + "class_id BIGINT NULL,"
                + "student_name VARCHAR(50) NOT NULL,"
                + "class_name VARCHAR(50) NULL,"
                + "student_no VARCHAR(20) NULL,"
                + "answers_json LONGTEXT NOT NULL,"
                + "status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',"
                + "score INT NULL,"
                + "grade_level VARCHAR(4) NULL,"
                + "feedback TEXT NULL,"
                + "submitted_at DATETIME NULL,"
                + "graded_at DATETIME NULL,"
                + "graded_by BIGINT NULL,"
                + "updated_at DATETIME NULL,"
                + "PRIMARY KEY (id),"
                + "UNIQUE KEY uk_unit_quiz_lesson_student (lesson_id, student_id),"
                + "KEY idx_unit_quiz_lesson_class (lesson_id, class_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    public Map<String, Object> studentBundle(Long lessonId, SysUser student) {
        ensureUnitQuizLesson(lessonId);
        JsonNode paper = loadPaper(lessonId);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("lessonId", lessonId);
        out.put("title", text(paper, "title", "单元小测"));
        out.put("subtitle", text(paper, "subtitle", ""));
        out.put("intro", text(paper, "intro", ""));
        out.put("maxScore", paper.path("maxScore").asInt(100));
        out.put("sections", paper.path("sections"));
        out.put("questions", sanitizeQuestionsForStudent(paper.path("questions")));
        Optional<UnitQuizSubmission> mine =
                submissionRepository.findByLessonIdAndStudentId(lessonId, student.getId());
        if (mine.isPresent() && !"GRADED".equals(mine.get().getStatus())) {
            mine = Optional.of(ensureAutoGraded(mine.get()));
        }
        out.put("mine", studentView(mine));
        return out;
    }

    @Transactional
    public Map<String, Object> submit(Long lessonId, SysUser student, Map<String, Object> body) {
        ensureUnitQuizLesson(lessonId);
        if (student.getClassId() == null) {
            throw new BusinessException("当前账号未绑定班级，无法提交");
        }
        JsonNode paper = loadPaper(lessonId);
        Optional<UnitQuizSubmission> existing =
                submissionRepository.findByLessonIdAndStudentId(lessonId, student.getId());
        if (existing.isPresent() && "GRADED".equals(existing.get().getStatus())) {
            throw new BusinessException("已批改，不可再修改作答");
        }

        Map<String, Object> payload = extractAnswers(body);
        validateRequired(paper, payload);

        String className = classRepository.findById(student.getClassId())
                .map(SysClass::getName)
                .orElse("");
        String studentNo = PretestG4Service.parseStudentNo(student.getUsername());

        UnitQuizSubmission row = existing.orElseGet(UnitQuizSubmission::new);
        row.setLessonId(lessonId);
        row.setQuizKey(text(paper, "quizKey", "unit-quiz"));
        row.setStudentId(student.getId());
        row.setClassId(student.getClassId());
        row.setStudentName(student.getRealName());
        row.setClassName(className);
        row.setStudentNo(studentNo);
        try {
            row.setAnswersJson(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new BusinessException("作答数据无法保存");
        }
        row.setSubmittedAt(LocalDateTime.now());
        // 提交即自动批改：按参考答案计分并给出等级（教师仍可事后改分）
        applyAutoGrade(row, paper, payload);
        submissionRepository.save(row);
        return studentView(Optional.of(row));
    }

    /** 将尚未批改的提交按参考答案自动打分（兼容旧数据）。 */
    @Transactional
    public UnitQuizSubmission ensureAutoGraded(UnitQuizSubmission row) {
        if (row == null) {
            return null;
        }
        if ("GRADED".equals(row.getStatus()) && row.getScore() != null && row.getGradeLevel() != null) {
            return row;
        }
        JsonNode paper = loadPaper(row.getLessonId());
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = (Map<String, Object>) parseAnswers(row.getAnswersJson());
        applyAutoGrade(row, paper, answers);
        return submissionRepository.save(row);
    }

    private void applyAutoGrade(UnitQuizSubmission row, JsonNode paper, Map<String, Object> answers) {
        Map<String, Object> scored = scoreAnswers(paper, answers);
        int score = 0;
        Object suggested = scored.get("suggestedScore");
        if (suggested instanceof Number) {
            score = ((Number) suggested).intValue();
        }
        GradeResult gr = resolveGrade(score, paper);
        row.setScore(score);
        row.setGradeLevel(gr.level);
        row.setFeedback(gr.feedback);
        row.setStatus("GRADED");
        row.setGradedAt(LocalDateTime.now());
        // null = 系统自动批改
        row.setGradedBy(null);
    }

    public Map<String, Object> teacherOverview(Long lessonId, Long classId, Long teacherId, String role) {
        ensureUnitQuizLesson(lessonId);
        if (!"ADMIN".equals(role)) {
            dashboardService.checkTeacherOwnsClass(teacherId, classId);
        }
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        JsonNode paper = loadPaper(lessonId);
        List<SysUser> students = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1);
        List<UnitQuizSubmission> rows =
                submissionRepository.findByLessonIdAndClassIdOrderBySubmittedAtDesc(lessonId, classId);
        Map<Long, UnitQuizSubmission> byStudent = new HashMap<Long, UnitQuizSubmission>();
        for (UnitQuizSubmission row : rows) {
            byStudent.put(row.getStudentId(), row);
        }

        int submitted = 0;
        int graded = 0;
        Map<String, Integer> levelCount = new LinkedHashMap<String, Integer>();
        levelCount.put("A", 0);
        levelCount.put("B", 0);
        levelCount.put("C", 0);
        levelCount.put("D", 0);

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (SysUser s : students) {
            UnitQuizSubmission row = byStudent.get(s.getId());
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("studentId", s.getId());
            item.put("realName", s.getRealName());
            item.put("username", s.getUsername());
            item.put("studentNo", PretestG4Service.parseStudentNo(s.getUsername()));
            item.put("className", cls.getName());
            if (row == null) {
                item.put("submitted", false);
                item.put("graded", false);
                item.put("status", "NOT_SUBMITTED");
            } else {
                if (!"GRADED".equals(row.getStatus()) || row.getScore() == null) {
                    row = ensureAutoGraded(row);
                    byStudent.put(s.getId(), row);
                }
                submitted++;
                boolean isGraded = "GRADED".equals(row.getStatus());
                if (isGraded) {
                    graded++;
                    String lv = row.getGradeLevel();
                    if (lv != null && levelCount.containsKey(lv)) {
                        levelCount.put(lv, levelCount.get(lv) + 1);
                    }
                }
                item.put("submitted", true);
                item.put("graded", isGraded);
                item.put("status", row.getStatus());
                item.put("score", row.getScore());
                item.put("gradeLevel", row.getGradeLevel());
                item.put("feedback", row.getFeedback());
                item.put("submittedAt", row.getSubmittedAt());
                item.put("gradedAt", row.getGradedAt());
                item.put("answers", parseAnswers(row.getAnswersJson()));
            }
            list.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("lessonId", lessonId);
        out.put("title", text(paper, "title", "单元小测"));
        out.put("subtitle", text(paper, "subtitle", ""));
        out.put("maxScore", paper.path("maxScore").asInt(100));
        out.put("sections", paper.path("sections"));
        out.put("questions", paper.path("questions"));
        out.put("paperHtmlTeacher", text(paper, "paperHtmlTeacher", ""));
        out.put("classId", classId);
        out.put("className", cls.getName());
        out.put("totalStudents", students.size());
        out.put("submittedCount", submitted);
        out.put("gradedCount", graded);
        out.put("levelCount", levelCount);
        out.put("students", list);
        return out;
    }

    /** 教师端：按参考答案自动计分（填空可再人工微调） */
    public Map<String, Object> autoScore(Long lessonId, Long studentId, Long teacherId, String role) {
        ensureUnitQuizLesson(lessonId);
        UnitQuizSubmission row = submissionRepository.findByLessonIdAndStudentId(lessonId, studentId)
                .orElseThrow(() -> new BusinessException("该学生尚未提交"));
        if (row.getClassId() != null && !"ADMIN".equals(role)) {
            dashboardService.checkTeacherOwnsClass(teacherId, row.getClassId());
        }
        JsonNode paper = loadPaper(lessonId);
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = (Map<String, Object>) parseAnswers(row.getAnswersJson());
        return scoreAnswers(paper, answers);
    }

    @Transactional
    public Map<String, Object> grade(Long lessonId, Long teacherId, String role, Map<String, Object> body) {
        ensureUnitQuizLesson(lessonId);
        if (body == null) {
            throw new BusinessException("缺少批改数据");
        }
        Long studentId = toLong(body.get("studentId"));
        if (studentId == null) {
            throw new BusinessException("缺少学生 ID");
        }
        Integer score = toInt(body.get("score"));
        if (score == null) {
            throw new BusinessException("请填写分数");
        }
        int maxScore = loadPaper(lessonId).path("maxScore").asInt(100);
        if (score < 0 || score > maxScore) {
            throw new BusinessException("分数须在 0～" + maxScore + " 之间");
        }

        UnitQuizSubmission row = submissionRepository.findByLessonIdAndStudentId(lessonId, studentId)
                .orElseThrow(() -> new BusinessException("该学生尚未提交"));
        if (row.getClassId() != null && !"ADMIN".equals(role)) {
            dashboardService.checkTeacherOwnsClass(teacherId, row.getClassId());
        }

        GradeResult gr = resolveGrade(score, loadPaper(lessonId));
        row.setScore(score);
        row.setGradeLevel(gr.level);
        row.setFeedback(gr.feedback);
        row.setStatus("GRADED");
        row.setGradedAt(LocalDateTime.now());
        row.setGradedBy(teacherId);
        submissionRepository.save(row);

        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("studentId", studentId);
        out.put("score", score);
        out.put("gradeLevel", gr.level);
        out.put("feedback", gr.feedback);
        out.put("status", "GRADED");
        out.put("gradedAt", row.getGradedAt());
        return out;
    }

    public static GradeResult resolveGrade(int score, JsonNode paper) {
        JsonNode rules = paper.path("gradeRules");
        if (rules.isObject() && rules.has("A")) {
            if (score >= rules.path("A").path("min").asInt(80)) {
                return new GradeResult("A", text(rules.path("A"), "feedback", "本单元学的很好！继续保持"));
            }
            if (score >= rules.path("B").path("min").asInt(60)) {
                return new GradeResult("B", text(rules.path("B"), "feedback",
                        "本单元学的还行！但是还需要查缺补漏，有问题多问问张老师噢！"));
            }
            if (score >= rules.path("C").path("min").asInt(40)) {
                return new GradeResult("C", text(rules.path("C"), "feedback",
                        "本单元学起来是不是感觉很困难！多找张老师聊一聊吧！"));
            }
            return new GradeResult("D", text(rules.path("D"), "feedback", "你是不是没有好好学？"));
        }
        if (score >= 80) {
            return new GradeResult("A", "本单元学的很好！继续保持");
        }
        if (score >= 60) {
            return new GradeResult("B", "本单元学的还行！但是还需要查缺补漏，有问题多问问张老师噢！");
        }
        if (score >= 40) {
            return new GradeResult("C", "本单元学起来是不是感觉很困难！多找张老师聊一聊吧！");
        }
        return new GradeResult("D", "你是不是没有好好学？");
    }

    public boolean isUnitQuizLesson(Long lessonId) {
        CourseLesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson != null && lesson.getTitle() != null && lesson.getTitle().contains("单元小测")) {
            return true;
        }
        for (CourseTask t : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if (t.getConfigJson() != null && t.getConfigJson().contains("\"layout\"")
                    && t.getConfigJson().contains("unit_quiz")) {
                return true;
            }
        }
        return false;
    }

    private void ensureUnitQuizLesson(Long lessonId) {
        if (!isUnitQuizLesson(lessonId)) {
            throw new BusinessException("该课时不是单元小测");
        }
    }

    private JsonNode loadPaper(Long lessonId) {
        for (CourseTask t : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if ("EXTERNAL".equals(t.getTaskType()) && t.getConfigJson() != null
                    && t.getConfigJson().contains("unit_quiz")) {
                try {
                    return objectMapper.readTree(t.getConfigJson());
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }
        try {
            ClassPathResource res = new ClassPathResource("seed/g6-up-unit1-quiz.json");
            String json = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new BusinessException("单元小测试卷配置缺失");
        }
    }

    private Map<String, Object> studentView(Optional<UnitQuizSubmission> opt) {
        Map<String, Object> mine = new LinkedHashMap<String, Object>();
        if (!opt.isPresent()) {
            mine.put("submitted", false);
            mine.put("graded", false);
            mine.put("status", "NOT_SUBMITTED");
            return mine;
        }
        UnitQuizSubmission row = opt.get();
        boolean graded = "GRADED".equals(row.getStatus());
        mine.put("submitted", true);
        mine.put("graded", graded);
        mine.put("status", row.getStatus());
        mine.put("submittedAt", row.getSubmittedAt());
        mine.put("answers", parseAnswers(row.getAnswersJson()));
        if (graded) {
            // 学生端只返回等级与评语，不返回分数
            mine.put("gradeLevel", row.getGradeLevel());
            mine.put("feedback", row.getFeedback());
            mine.put("gradedAt", row.getGradedAt());
        }
        return mine;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAnswers(Map<String, Object> body) {
        if (body == null) {
            return new LinkedHashMap<String, Object>();
        }
        Object answers = body.get("answers");
        if (answers instanceof Map) {
            return new LinkedHashMap<String, Object>((Map<String, Object>) answers);
        }
        Map<String, Object> copy = new LinkedHashMap<String, Object>(body);
        copy.remove("answers");
        return copy;
    }

    private void validateRequired(JsonNode paper, Map<String, Object> payload) {
        JsonNode questions = paper.path("questions");
        if (!questions.isArray()) {
            return;
        }
        for (JsonNode q : questions) {
            if (!q.path("required").asBoolean(false)) {
                continue;
            }
            String type = q.path("type").asText("single");
            String id = q.path("id").asText("");
            String stem = q.path("stem").asText(id);
            if ("fill_group".equals(type)) {
                for (JsonNode blank : q.path("blanks")) {
                    String bid = blank.path("id").asText("");
                    Object val = payload.get(bid);
                    if (val == null || String.valueOf(val).trim().isEmpty()) {
                        throw new BusinessException("请完成填空：" + blank.path("label").asText(stem));
                    }
                }
                continue;
            }
            Object val = payload.get(id);
            if ("multi".equals(type)) {
                if (!(val instanceof List) || ((List<?>) val).isEmpty()) {
                    throw new BusinessException("请完成多选题第 " + q.path("no").asInt(0) + " 题");
                }
                continue;
            }
            if (val == null || String.valueOf(val).trim().isEmpty()) {
                throw new BusinessException("请完成第 " + q.path("no").asInt(0) + " 题");
            }
        }
    }

    private Object sanitizeQuestionsForStudent(JsonNode questions) {
        if (!questions.isArray()) {
            return questions;
        }
        try {
            com.fasterxml.jackson.databind.node.ArrayNode arr = objectMapper.createArrayNode();
            for (JsonNode q : questions) {
                com.fasterxml.jackson.databind.node.ObjectNode copy =
                        (com.fasterxml.jackson.databind.node.ObjectNode) q.deepCopy();
                copy.remove("answer");
                copy.remove("accept");
                copy.remove("halfAccept");
                if (copy.has("blanks") && copy.get("blanks").isArray()) {
                    for (JsonNode blank : copy.get("blanks")) {
                        if (blank instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                            com.fasterxml.jackson.databind.node.ObjectNode b =
                                    (com.fasterxml.jackson.databind.node.ObjectNode) blank;
                            b.remove("answer");
                            b.remove("accept");
                            b.remove("halfAccept");
                        }
                    }
                }
                arr.add(copy);
            }
            return arr;
        } catch (Exception e) {
            return questions;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> scoreAnswers(JsonNode paper, Map<String, Object> answers) {
        if (answers == null) {
            answers = new LinkedHashMap<String, Object>();
        }
        int total = 0;
        int max = 0;
        List<Map<String, Object>> detail = new ArrayList<Map<String, Object>>();
        for (JsonNode q : paper.path("questions")) {
            String type = q.path("type").asText("single");
            if ("fill_group".equals(type)) {
                for (JsonNode blank : q.path("blanks")) {
                    int blankMax = blank.path("score").asInt(4);
                    max += blankMax;
                    String bid = blank.path("id").asText("");
                    String raw = answers.get(bid) == null ? "" : String.valueOf(answers.get(bid)).trim();
                    int got = scoreFillBlank(blank, raw);
                    total += got;
                    Map<String, Object> item = new LinkedHashMap<String, Object>();
                    item.put("id", bid);
                    item.put("questionId", q.path("id").asText(""));
                    item.put("no", q.path("no").asInt(0));
                    item.put("label", blank.path("label").asText(""));
                    item.put("type", "fill");
                    item.put("student", raw);
                    item.put("answer", blank.path("answer").asText(""));
                    item.put("score", got);
                    item.put("maxScore", blankMax);
                    detail.add(item);
                }
                continue;
            }
            int qMax = q.path("score").asInt(4);
            max += qMax;
            String id = q.path("id").asText("");
            int got;
            Object studentAns = answers.get(id);
            if ("multi".equals(type)) {
                got = scoreMulti(q, studentAns);
            } else {
                got = scoreSingle(q, studentAns);
            }
            total += got;
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("id", id);
            item.put("questionId", id);
            item.put("no", q.path("no").asInt(0));
            item.put("type", type);
            item.put("student", studentAns);
            item.put("answer", "multi".equals(type) ? q.path("answer") : q.path("answer").asText(""));
            item.put("score", got);
            item.put("maxScore", qMax);
            detail.add(item);
        }
        Object unsure = answers.get("unsure");
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("suggestedScore", total);
        out.put("maxScore", max > 0 ? max : paper.path("maxScore").asInt(100));
        out.put("detail", detail);
        out.put("unsure", unsure);
        return out;
    }

    private int scoreSingle(JsonNode q, Object studentAns) {
        String expect = q.path("answer").asText("");
        String got = studentAns == null ? "" : String.valueOf(studentAns).trim().toUpperCase();
        if (!got.isEmpty() && got.equalsIgnoreCase(expect)) {
            return q.path("score").asInt(4);
        }
        return 0;
    }

    private int scoreMulti(JsonNode q, Object studentAns) {
        List<String> expect = new ArrayList<String>();
        for (JsonNode n : q.path("answer")) {
            expect.add(n.asText().toUpperCase());
        }
        java.util.Collections.sort(expect);
        List<String> got = new ArrayList<String>();
        if (studentAns instanceof List) {
            for (Object o : (List<?>) studentAns) {
                if (o != null && !String.valueOf(o).trim().isEmpty()) {
                    got.add(String.valueOf(o).trim().toUpperCase());
                }
            }
        } else if (studentAns != null) {
            String s = String.valueOf(studentAns).replace(",", "").replace(" ", "").toUpperCase();
            for (int i = 0; i < s.length(); i++) {
                got.add(String.valueOf(s.charAt(i)));
            }
        }
        java.util.Collections.sort(got);
        if (got.isEmpty()) {
            return 0;
        }
        if (got.equals(expect)) {
            return q.path("score").asInt(4);
        }
        // 漏选：所选全部正确且为期望子集
        if (expect.containsAll(got)) {
            return q.path("partialScore").asInt(2);
        }
        return 0;
    }

    private int scoreFillBlank(JsonNode blank, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        String norm = normalizeFill(raw);
        String answer = blank.path("answer").asText("");
        if (!answer.isEmpty() && normalizeFill(answer).equals(norm)) {
            return blank.path("score").asInt(4);
        }
        for (JsonNode a : blank.path("accept")) {
            if (normalizeFill(a.asText()).equals(norm) || norm.contains(normalizeFill(a.asText()))) {
                return blank.path("score").asInt(4);
            }
        }
        // 半对：如 19(1) 写 ≠ 给 2 分
        for (JsonNode a : blank.path("halfAccept")) {
            String h = normalizeFill(a.asText());
            if (!h.isEmpty() && (norm.equals(h) || norm.contains(h) || h.contains(norm))) {
                return Math.max(1, blank.path("score").asInt(4) / 2);
            }
        }
        return 0;
    }

    private static String normalizeFill(String s) {
        if (s == null) {
            return "";
        }
        return s.trim()
                .replace(" ", "")
                .replace("　", "")
                .replace("\"", "")
                .replace("'", "")
                .replace("（", "(")
                .replace("）", ")")
                .toLowerCase();
    }

    private Object parseAnswers(String json) {
        if (json == null || json.isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<String, Object>();
        }
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return fallback;
        }
        String s = v.asText();
        return s == null || s.isEmpty() ? fallback : s;
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static class GradeResult {
        public final String level;
        public final String feedback;

        public GradeResult(String level, String feedback) {
            this.level = level;
            this.feedback = feedback;
        }
    }
}
