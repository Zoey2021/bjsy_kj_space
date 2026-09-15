package com.itech.learnspace.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.config.ActivityProperties;
import com.itech.learnspace.dto.HintUsedRequest;
import com.itech.learnspace.dto.ScaffoldGenerateRequest;
import com.itech.learnspace.dto.ScaffoldPublishRequest;
import com.itech.learnspace.dto.TierOverrideRequest;
import com.itech.learnspace.entity.CourseLesson;
import com.itech.learnspace.entity.CourseTask;
import com.itech.learnspace.entity.LearnHintLog;
import com.itech.learnspace.entity.LearnPretestG6;
import com.itech.learnspace.entity.LearnSubmission;
import com.itech.learnspace.entity.LessonScaffold;
import com.itech.learnspace.entity.ScaffoldTemplate;
import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.CourseGradeRepository;
import com.itech.learnspace.repository.CourseLessonRepository;
import com.itech.learnspace.repository.CourseTaskRepository;
import com.itech.learnspace.repository.CourseUnitRepository;
import com.itech.learnspace.repository.LearnHintLogRepository;
import com.itech.learnspace.repository.LearnPretestG6Repository;
import com.itech.learnspace.repository.LessonScaffoldRepository;
import com.itech.learnspace.repository.ScaffoldTemplateRepository;
import com.itech.learnspace.repository.SysClassRepository;
import com.itech.learnspace.repository.SysUserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScaffoldService {

    private final ScaffoldTemplateRepository templateRepository;
    private final LessonScaffoldRepository scaffoldRepository;
    private final LearnHintLogRepository hintLogRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseTaskRepository taskRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseGradeRepository gradeRepository;
    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final LearnPretestG6Repository pretestRepository;
    private final BailianAppClient aiClient;
    private final ObjectMapper objectMapper;
    private final DashboardService dashboardService;
    private final ActivityProperties activityProperties;

    public ScaffoldService(ScaffoldTemplateRepository templateRepository,
                           LessonScaffoldRepository scaffoldRepository,
                           LearnHintLogRepository hintLogRepository,
                           CourseLessonRepository lessonRepository,
                           CourseTaskRepository taskRepository,
                           CourseUnitRepository unitRepository,
                           CourseGradeRepository gradeRepository,
                           SysUserRepository userRepository,
                           SysClassRepository classRepository,
                           LearnPretestG6Repository pretestRepository,
                           BailianAppClient aiClient,
                           ObjectMapper objectMapper,
                           @Lazy DashboardService dashboardService,
                           ActivityProperties activityProperties) {
        this.templateRepository = templateRepository;
        this.scaffoldRepository = scaffoldRepository;
        this.hintLogRepository = hintLogRepository;
        this.lessonRepository = lessonRepository;
        this.taskRepository = taskRepository;
        this.unitRepository = unitRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.pretestRepository = pretestRepository;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.dashboardService = dashboardService;
        this.activityProperties = activityProperties;
    }

    public String effectiveTier(SysUser student) {
        String override = student.getTierOverride();
        if (isTier(override)) {
            return override.toUpperCase();
        }
        return pretestRepository.findByStudentId(student.getId())
                .map(this::resolvedPretestLevel)
                .filter(this::isTier)
                .map(String::toUpperCase)
                .orElse("B");
    }

    private String resolvedPretestLevel(LearnPretestG6 row) {
        if (row == null) {
            return "B";
        }
        JsonNode content;
        try {
            content = objectMapper.readTree(row.getContentJson() == null ? "{}" : row.getContentJson());
        } catch (Exception e) {
            content = null;
        }
        int total = row.getTotalScore() == null ? 0 : row.getTotalScore();
        String level = PretestG6Service.computeLevel(total, content);
        if (level != null && !level.equalsIgnoreCase(row.getLevelCode() == null ? "" : row.getLevelCode())) {
            row.setLevelCode(level);
            pretestRepository.save(row);
        }
        return level;
    }

    public List<Map<String, Object>> listTemplates() {
        ensureDefaultTemplate();
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (ScaffoldTemplate t : templateRepository.findAll()) {
            out.add(toTemplateMap(t));
        }
        return out;
    }

    @Transactional
    public Map<String, Object> saveTemplate(Map<String, Object> body) {
        if (body == null) {
            throw new BusinessException("模板内容不能为空");
        }
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("请填写模板名称");
        }
        ScaffoldTemplate row;
        Object idObj = body.get("id");
        if (idObj instanceof Number) {
            row = templateRepository.findById(((Number) idObj).longValue())
                    .orElseThrow(() -> new BusinessException("模板不存在"));
        } else {
            row = new ScaffoldTemplate();
        }
        row.setName(name);
        row.setTierAJson(writeJson(body.get("tierA")));
        row.setTierBJson(writeJson(body.get("tierB")));
        row.setTierCJson(writeJson(body.get("tierC")));
        return toTemplateMap(templateRepository.save(row));
    }

    public Map<String, Object> getLessonScaffold(Long lessonId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        List<Map<String, Object>> activities = listLessonActivities(lessonId);
        LessonScaffold row = scaffoldRepository.findByLessonId(lessonId).orElse(null);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("lessonId", lessonId);
        out.put("lessonTitle", lesson.getTitle());
        out.put("aiReady", aiClient.isConfigured());
        out.put("activities", activities);
        out.put("templates", listTemplates());
        if (row == null) {
            out.put("status", "draft");
            out.put("templateId", null);
            out.put("items", emptyItems(activities));
            return out;
        }
        out.put("status", row.getStatus());
        out.put("templateId", row.getTemplateId());
        out.put("items", mergeItems(activities, parseItems(row.getItemsJson())));
        return out;
    }

    @Transactional
    public Map<String, Object> saveDraft(Long lessonId, ScaffoldPublishRequest req) {
        LessonScaffold row = upsertRow(lessonId, req == null ? null : req.getTemplateId());
        List<Map<String, Object>> items = req == null || req.getItems() == null
                ? emptyItems(listLessonActivities(lessonId)) : req.getItems();
        row.setItemsJson(writeJson(items));
        row.setStatus("draft");
        scaffoldRepository.save(row);
        return getLessonScaffold(lessonId);
    }

    @Transactional
    public Map<String, Object> publish(Long lessonId, ScaffoldPublishRequest req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException("请先审核活动内容");
        }
        for (Map<String, Object> item : req.getItems()) {
            if (!Boolean.TRUE.equals(item.get("reviewed"))) {
                throw new BusinessException("请审核全部活动后再发布");
            }
        }
        LessonScaffold row = upsertRow(lessonId, req.getTemplateId());
        row.setItemsJson(writeJson(req.getItems()));
        row.setStatus("published");
        scaffoldRepository.save(row);
        return getLessonScaffold(lessonId);
    }

    public Map<String, Object> generate(ScaffoldGenerateRequest req) {
        if (req == null || req.getLessonId() == null) {
            throw new BusinessException("请选择课时");
        }
        CourseLesson lesson = lessonRepository.findById(req.getLessonId())
                .orElseThrow(() -> new BusinessException("课时不存在"));
        List<Map<String, Object>> activities = listLessonActivities(req.getLessonId());
        if (activities.isEmpty()) {
            throw new BusinessException("该课时还没有探究活动，请先在活动编辑中配置");
        }
        LessonScaffold row = upsertRow(req.getLessonId(), req.getTemplateId());
        List<Map<String, Object>> items = mergeItems(activities, parseItems(row.getItemsJson()));
        List<Integer> targets = resolveTargets(req.getActivityIndex(), activities);
        Map<String, Object> defs = req.getTierDefinitions() == null ? defaultTierDefs() : req.getTierDefinitions();
        for (Integer idx : targets) {
            Map<String, Object> act = findActivity(activities, idx);
            Map<String, Object> generated = generateOne(lesson, act, defs, activities);
            replaceItemTiers(items, idx, generated);
        }
        row.setItemsJson(writeJson(items));
        row.setStatus("draft");
        scaffoldRepository.save(row);
        return getLessonScaffold(req.getLessonId());
    }

    public Map<String, Object> studentLessonContent(SysUser student, Long lessonId) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("lessonId", lessonId);
        out.put("published", false);
        LessonScaffold row = scaffoldRepository.findByLessonId(lessonId).orElse(null);
        if (row == null || !"published".equals(row.getStatus())) {
            return out;
        }
        String tier = effectiveTier(student);
        List<Map<String, Object>> items = parseItems(row.getItemsJson());
        List<Map<String, Object>> filtered = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : items) {
            filtered.add(studentItem(item, tier));
        }
        out.put("published", true);
        out.put("activities", filtered);
        return out;
    }

    @Transactional
    public Map<String, Object> hintUsed(SysUser student, HintUsedRequest req) {
        if (req == null || req.getLessonId() == null || req.getActivityIndex() == null || req.getHintIndex() == null) {
            throw new BusinessException("参数不完整");
        }
        int hintIndex = req.getHintIndex();
        if (hintIndex < 0 || hintIndex > 3) {
            throw new BusinessException("提示层数无效");
        }
        if (!hintLogRepository.existsByStudentIdAndLessonIdAndActivityIndexAndHintIndex(
                student.getId(), req.getLessonId(), req.getActivityIndex(), hintIndex)) {
            LearnHintLog log = new LearnHintLog();
            log.setStudentId(student.getId());
            log.setLessonId(req.getLessonId());
            log.setActivityIndex(req.getActivityIndex());
            log.setHintIndex(hintIndex);
            hintLogRepository.save(log);
        }
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("ok", true);
        out.put("usedCount", hintLogRepository.countByStudentAndActivity(
                student.getId(), req.getLessonId(), req.getActivityIndex()));
        return out;
    }

    public Map<String, Object> classTiers(Long teacherId, String role, Long classId) {
        if (!"ADMIN".equals(role)) {
            dashboardService.checkTeacherOwnsClass(teacherId, classId);
        }
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        List<SysUser> students = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1);
        Map<Long, LearnPretestG6> pretest = new HashMap<Long, LearnPretestG6>();
        for (LearnPretestG6 row : pretestRepository.findByClassIdOrderBySubmittedAtDesc(classId)) {
            pretest.put(row.getStudentId(), row);
        }
        int a = 0, b = 0, c = 0;
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (SysUser s : students) {
            String pretestLevel = pretest.containsKey(s.getId())
                    ? resolvedPretestLevel(pretest.get(s.getId())) : "";
            String effective = effectiveTier(s);
            if ("A".equals(effective)) a++;
            else if ("C".equals(effective)) c++;
            else b++;
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("studentId", s.getId());
            item.put("realName", s.getRealName());
            item.put("username", s.getUsername());
            item.put("pretestLevel", pretestLevel);
            item.put("tierOverride", isTier(s.getTierOverride()) ? s.getTierOverride().toUpperCase() : null);
            item.put("effectiveTier", effective);
            list.add(item);
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("classId", classId);
        out.put("className", cls.getName());
        out.put("totalStudents", students.size());
        out.put("countA", a);
        out.put("countB", b);
        out.put("countC", c);
        out.put("students", list);
        return out;
    }

    @Transactional
    public Map<String, Object> overrideTier(Long teacherId, String role, TierOverrideRequest req) {
        if (req == null || req.getStudentId() == null) {
            throw new BusinessException("请选择学生");
        }
        SysUser student = userRepository.findById(req.getStudentId())
                .orElseThrow(() -> new BusinessException("学生不存在"));
        if (!"STUDENT".equals(student.getRole())) {
            throw new BusinessException("只能调整学生档位");
        }
        if (student.getClassId() != null && !"ADMIN".equals(role)) {
            dashboardService.checkTeacherOwnsClass(teacherId, student.getClassId());
        }
        String tier = req.getTier();
        if (tier == null || "CLEAR".equalsIgnoreCase(tier) || "".equals(tier.trim())) {
            student.setTierOverride(null);
        } else if (isTier(tier)) {
            student.setTierOverride(tier.toUpperCase());
        } else {
            throw new BusinessException("档位只能是 A / B / C");
        }
        userRepository.save(student);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("studentId", student.getId());
        out.put("effectiveTier", effectiveTier(student));
        return out;
    }

    public Map<Long, String> effectiveTiers(List<SysUser> students) {
        Map<Long, String> map = new HashMap<Long, String>();
        for (SysUser s : students) {
            if (s == null || !"STUDENT".equals(s.getRole())) {
                continue;
            }
            map.put(s.getId(), effectiveTier(s));
        }
        return map;
    }

    public Map<String, Integer> hintCountByStudentActivity(Long lessonId) {
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (LearnHintLog h : hintLogRepository.findByLessonId(lessonId)) {
            String key = h.getStudentId() + "_" + h.getActivityIndex();
            Integer n = map.get(key);
            map.put(key, n == null ? 1 : n + 1);
        }
        return map;
    }

    public Map<String, Object> buildActivityTierSlice(int activityIndex, List<SysUser> students,
                                                      Map<Long, String> tierByStudent,
                                                      Map<Long, LearnSubmission> subByStudent,
                                                      Map<String, Integer> hintCounts,
                                                      java.util.function.BiPredicate<SysUser, LearnSubmission> doneFn) {
        int[] head = {0, 0, 0};
        int[] done = {0, 0, 0};
        long[] hintSum = {0, 0, 0};
        int[] hintPeople = {0, 0, 0};
        for (SysUser s : students) {
            if (s == null || !"STUDENT".equals(s.getRole())) {
                continue;
            }
            String tier = tierByStudent.get(s.getId());
            if (!isTier(tier)) {
                tier = "B";
            }
            int i = "A".equals(tier) ? 0 : ("C".equals(tier) ? 2 : 1);
            head[i]++;
            LearnSubmission sub = subByStudent.get(s.getId());
            if (doneFn.test(s, sub)) {
                done[i]++;
            }
            Integer hc = hintCounts.get(s.getId() + "_" + activityIndex);
            hintSum[i] += hc == null ? 0 : hc;
            hintPeople[i]++;
        }
        Map<String, Object> slice = new LinkedHashMap<String, Object>();
        slice.put("A", oneTier(head[0], done[0], hintSum[0], hintPeople[0]));
        slice.put("B", oneTier(head[1], done[1], hintSum[1], hintPeople[1]));
        slice.put("C", oneTier(head[2], done[2], hintSum[2], hintPeople[2]));
        return slice;
    }

    private Map<String, Object> oneTier(int head, int done, long hintSum, int hintPeople) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("count", head);
        m.put("submitted", done);
        m.put("rate", head > 0 ? Math.round(done * 1000.0 / head) / 10.0 : 0);
        m.put("hintAvg", hintPeople > 0 ? Math.round(hintSum * 10.0 / hintPeople) / 10.0 : 0);
        return m;
    }

    private LessonScaffold upsertRow(Long lessonId, Long templateId) {
        lessonRepository.findById(lessonId).orElseThrow(() -> new BusinessException("课时不存在"));
        LessonScaffold row = scaffoldRepository.findByLessonId(lessonId).orElseGet(LessonScaffold::new);
        row.setLessonId(lessonId);
        if (templateId != null) {
            row.setTemplateId(templateId);
        }
        if (!StringUtils.hasText(row.getItemsJson())) {
            row.setItemsJson("[]");
        }
        if (!StringUtils.hasText(row.getStatus())) {
            row.setStatus("draft");
        }
        return row;
    }

    private Map<String, Object> generateOne(CourseLesson lesson, Map<String, Object> act,
                                            Map<String, Object> defs, List<Map<String, Object>> allActs) {
        int idx = asInt(act.get("index"));
        String title = String.valueOf(act.getOrDefault("title", "探究活动"));
        String path = asText(act.get("path"));
        String unitName = unitRepository.findById(lesson.getUnitId())
                .map(u -> u.getName()).orElse("");
        String gradeName = unitRepository.findById(lesson.getUnitId())
                .map(u -> gradeRepository.findById(u.getGradeId()).map(g -> g.getName()).orElse(""))
                .orElse("");
        StringBuilder siblings = new StringBuilder();
        for (Map<String, Object> a : allActs) {
            siblings.append("探究").append(a.get("index")).append("：")
                    .append(a.get("title")).append("\n");
        }
        String pageText = extractActivityPageText(path, idx);
        String intro = clip(stripHtml(lesson.getContent()), 800);
        String objectives = asText(act.get("objectives"));

        Map<String, Object> biz = new LinkedHashMap<String, Object>();
        biz.put("lesson_title", nvl(lesson.getTitle()));
        biz.put("objectives", nvl(objectives));
        biz.put("activity_index", idx + "｜" + nvl(title));
        biz.put("activity_page", nvl(pageText));
        biz.put("all_activities", siblings.toString().trim());
        biz.put("tier_goals", writeJson(defs));
        biz.put("class_tier_summary", "未选择班级。请按模板目标设计三档支架，不要针对个别学生。");

        String prompt = workflowPrompt(gradeName, unitName, lesson.getTitle(), objectives,
                siblings.toString(), idx, title, intro, pageText, defs);
        String raw = aiClient.runWorkflow(prompt, biz);
        Map<String, Object> parsed = parseAiJson(raw);
        if (parsed == null) {
            throw new BusinessException("百炼工作流返回无法解析，请让结束节点只输出 JSON");
        }
        return normalizeGenerated(parsed);
    }

    private String workflowPrompt(String gradeName, String unitName, String lessonTitle, String objectives,
                                  String siblings, int idx, String title, String intro, String pageText,
                                  Map<String, Object> defs) {
        return "请根据下列课时数据，为本探究环节生成 A/B/C 三档教学支架，只输出 JSON。\n"
                + "【课程】" + nvl(gradeName) + " / " + nvl(unitName) + " / " + nvl(lessonTitle) + "\n"
                + "【本课学习目标】" + (StringUtils.hasText(objectives) ? objectives : "（未填写）") + "\n"
                + "【本课全部探究环节】\n" + siblings
                + "【当前要出支架的环节】探究" + idx + "「" + title + "」\n"
                + "【课时介绍摘录】" + (StringUtils.hasText(intro) ? intro : "（无）") + "\n"
                + "【本环节页面任务原文】\n"
                + (StringUtils.hasText(pageText) ? pageText : "（未能读取页面，请严格按环节标题来写，不要换成其他课题）") + "\n"
                + "【三档目标】" + writeJson(defs) + "\n"
                + "分层规则：三档是同一任务的不同支架，不是三道不同的题。"
                + "A 档只说任务（可加变式），少给甚至不给操作提示；"
                + "B 档在任务后给规范提示（若是画流程图，提示起止、输入输出、处理、判断四种形状）；"
                + "C 档在 B 档基础上再给逐步操作提示（先拖什么、再连什么）。"
                + "禁止编造本环节没有的情境；语言口语化，面向小学生；不要 markdown。\n"
                + "只输出 JSON：{\"A\":{\"task\":\"\",\"hints\":[],\"challenge\":\"\"},"
                + "\"B\":{\"task\":\"\",\"hints\":[\"\"],\"example\":\"\"},"
                + "\"C\":{\"task\":\"\",\"hints\":[\"\"],\"workedExample\":\"\","
                + "\"traceQuestions\":[{\"question\":\"\",\"options\":[\"\",\"\",\"\"],\"answer\":0}]}}";
    }

    private Map<String, Object> normalizeGenerated(Map<String, Object> parsed) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("A", mergeTier("A", parsed.get("A")));
        out.put("B", mergeTier("B", parsed.get("B")));
        out.put("C", mergeTier("C", parsed.get("C")));
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeTier(String code, Object raw) {
        Map<String, Object> base = emptyTier(code);
        if (!(raw instanceof Map)) {
            return base;
        }
        Map<String, Object> src = (Map<String, Object>) raw;
        if (src.get("task") != null) {
            base.put("task", asText(src.get("task")));
        }
        if (src.get("hints") != null) {
            List<String> hints = normalizeHints(src.get("hints"));
            if (hints.isEmpty()) {
                hints.add("");
                hints.add("");
            }
            base.put("hints", hints);
        }
        if ("A".equals(code) && src.get("challenge") != null) {
            base.put("challenge", asText(src.get("challenge")));
        }
        if ("B".equals(code) && src.get("example") != null) {
            base.put("example", asText(src.get("example")));
        }
        if ("C".equals(code)) {
            if (src.get("workedExample") != null) {
                base.put("workedExample", asText(src.get("workedExample")));
            }
            if (src.get("traceQuestions") instanceof List) {
                base.put("traceQuestions", src.get("traceQuestions"));
            }
        }
        if (src.get("ui") instanceof Map) {
            base.put("ui", src.get("ui"));
        }
        return base;
    }

    private String nvl(String v) {
        return StringUtils.hasText(v) ? v : "";
    }

    private String extractActivityPageText(String webPath, int activityIndex) {
        Path file = resolveLessonHtml(webPath);
        if (file == null || !Files.exists(file)) {
            return "";
        }
        try {
            String html = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            String panel = slicePanel(html, activityIndex);
            return clip(stripHtml(StringUtils.hasText(panel) ? panel : html), 1600);
        } catch (Exception e) {
            return "";
        }
    }

    private Path resolveLessonHtml(String webPath) {
        if (!StringUtils.hasText(webPath) || !webPath.contains("/lessons/")) {
            return null;
        }
        int i = webPath.indexOf("/lessons/");
        String rel = webPath.substring(i + "/lessons/".length());
        List<Path> candidates = new ArrayList<Path>();
        if (activityProperties != null && StringUtils.hasText(activityProperties.getLessonHtmlDir())) {
            candidates.add(Paths.get(activityProperties.getLessonHtmlDir(), rel));
        }
        candidates.add(Paths.get("/app/lessons", rel));
        candidates.add(Paths.get("frontend/public/lessons", rel));
        for (Path p : candidates) {
            if (Files.exists(p)) {
                return p;
            }
        }
        return null;
    }

    private String slicePanel(String html, int index) {
        String[] ids = {
                "id=\"panel" + index + "\"",
                "id='panel" + index + "'",
                "data-step=\"" + index + "\"",
                "id=\"step" + index + "\""
        };
        int start = -1;
        for (String token : ids) {
            start = html.indexOf(token);
            if (start >= 0) {
                break;
            }
        }
        if (start < 0) {
            return "";
        }
        int next = html.indexOf("id=\"panel" + (index + 1) + "\"", start + 1);
        if (next < 0) {
            next = html.indexOf("id='panel" + (index + 1) + "'", start + 1);
        }
        return next > start ? html.substring(start, next) : html.substring(start);
    }

    private String stripHtml(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String text = raw.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
        return text;
    }

    private String clip(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private Map<String, Object> parseAiJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String text = raw.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return objectMapper.readValue(text.substring(start, end + 1),
                    new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            return null;
        }
    }

    private void replaceItemTiers(List<Map<String, Object>> items, int idx, Map<String, Object> generated) {
        for (Map<String, Object> item : items) {
            if (idx == asInt(item.get("activityIndex"))) {
                Map<String, Object> tiers = new LinkedHashMap<String, Object>();
                tiers.put("A", generated.getOrDefault("A", emptyTier("A")));
                tiers.put("B", generated.getOrDefault("B", emptyTier("B")));
                tiers.put("C", generated.getOrDefault("C", emptyTier("C")));
                item.put("tiers", tiers);
                item.put("reviewed", false);
                return;
            }
        }
    }

    private List<Integer> resolveTargets(Object activityIndex, List<Map<String, Object>> activities) {
        List<Integer> ids = new ArrayList<Integer>();
        if (activityIndex == null || "all".equals(String.valueOf(activityIndex))) {
            for (Map<String, Object> a : activities) {
                ids.add(asInt(a.get("index")));
            }
            return ids;
        }
        ids.add(asInt(activityIndex));
        return ids;
    }

    private Map<String, Object> findActivity(List<Map<String, Object>> activities, int idx) {
        for (Map<String, Object> a : activities) {
            if (asInt(a.get("index")) == idx) {
                return a;
            }
        }
        throw new BusinessException("活动不存在");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> studentItem(Map<String, Object> item, String tier) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("activityIndex", item.get("activityIndex"));
        out.put("title", item.get("title"));
        Map<String, Object> tiers = item.get("tiers") instanceof Map
                ? (Map<String, Object>) item.get("tiers") : Collections.emptyMap();
        Object raw = tiers.get(tier);
        Map<String, Object> t = raw instanceof Map ? (Map<String, Object>) raw : emptyTier(tier);
        out.put("task", t.get("task"));
        out.put("hints", normalizeHints(t.get("hints")));
        String worked = asText(t.get("workedExample"));
        String example = asText(t.get("example"));
        String challenge = asText(t.get("challenge"));
        if (StringUtils.hasText(worked)) {
            out.put("extraTitle", "完整示例");
            out.put("extraText", worked);
        } else if (StringUtils.hasText(example)) {
            out.put("extraTitle", "半成品示例");
            out.put("extraText", example);
        } else if (StringUtils.hasText(challenge)) {
            out.put("extraTitle", "想一想");
            out.put("extraText", challenge);
        }
        if (t.get("traceQuestions") instanceof List) {
            out.put("traceQuestions", t.get("traceQuestions"));
        }
        if (t.get("ui") instanceof Map) {
            out.put("ui", t.get("ui"));
        }
        return out;
    }

    public List<Map<String, Object>> listLessonActivities(Long lessonId) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (CourseTask task : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if (!"EXTERNAL".equals(task.getTaskType()) || !StringUtils.hasText(task.getConfigJson())) {
                continue;
            }
            try {
                JsonNode cfg = objectMapper.readTree(task.getConfigJson());
                if (!"student_workspace".equals(cfg.path("layout").asText()) || !cfg.has("activities")) {
                    continue;
                }
                for (JsonNode act : cfg.get("activities")) {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("index", act.path("index").asInt());
                    m.put("title", act.path("title").asText("活动"));
                    m.put("path", act.path("path").asText(""));
                    m.put("objectives", cfg.path("objectives").asText(""));
                    m.put("workspaceTitle", cfg.path("lessonTitle").asText(""));
                    list.add(m);
                }
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    private List<Map<String, Object>> emptyItems(List<Map<String, Object>> activities) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> a : activities) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("activityIndex", a.get("index"));
            item.put("title", a.get("title"));
            item.put("reviewed", false);
            Map<String, Object> tiers = new LinkedHashMap<String, Object>();
            tiers.put("A", emptyTier("A"));
            tiers.put("B", emptyTier("B"));
            tiers.put("C", emptyTier("C"));
            item.put("tiers", tiers);
            items.add(item);
        }
        return items;
    }

    private List<Map<String, Object>> mergeItems(List<Map<String, Object>> activities, List<Map<String, Object>> stored) {
        Map<Integer, Map<String, Object>> byIdx = new HashMap<Integer, Map<String, Object>>();
        for (Map<String, Object> item : stored) {
            byIdx.put(asInt(item.get("activityIndex")), item);
        }
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> a : activities) {
            int idx = asInt(a.get("index"));
            Map<String, Object> item = byIdx.get(idx);
            if (item == null) {
                Map<String, Object> created = new LinkedHashMap<String, Object>();
                created.put("activityIndex", idx);
                created.put("title", a.get("title"));
                created.put("reviewed", false);
                Map<String, Object> tiers = new LinkedHashMap<String, Object>();
                tiers.put("A", emptyTier("A"));
                tiers.put("B", emptyTier("B"));
                tiers.put("C", emptyTier("C"));
                created.put("tiers", tiers);
                out.add(created);
            } else {
                item.put("title", a.get("title"));
                out.add(item);
            }
        }
        return out;
    }

    private Map<String, Object> emptyTier(String code) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("task", "");
        List<String> hints = new ArrayList<String>();
        hints.add("");
        hints.add("");
        m.put("hints", hints);
        if ("A".equals(code)) {
            m.put("challenge", "");
        } else if ("B".equals(code)) {
            m.put("example", "");
        } else {
            m.put("workedExample", "");
            m.put("traceQuestions", new ArrayList<Object>());
        }
        return m;
    }

    private static final String DEFAULT_TEMPLATE_NAME = "六年级算法单元通用模板";
    private static final String DEFAULT_TIER_A =
            "{\"goal\":\"不仅完成基础任务，还能优化、改造算法，解决开放性变式问题\",\"criteria\":[\"能独立完成基础算法任务\",\"能提出优化或改造思路\",\"能解决开放性变式问题\"]}";
    private static final String DEFAULT_TIER_B =
            "{\"goal\":\"全部完成课标基础任务；模仿迁移解决同类简单问题（课堂达成基准）\",\"criteria\":[\"能按步骤完成基础任务\",\"能模仿示例解决同类问题\",\"提示使用后能独立完成\"]}";
    private static final String DEFAULT_TIER_C =
            "{\"goal\":\"能看懂别人的算法实例，会识别顺序/分支/循环三种基本结构，完成观察、识别即可\",\"criteria\":[\"能看懂完整示例\",\"能识别顺序、分支、循环\",\"能完成 tracing 观察题\"]}";

    private void ensureDefaultTemplate() {
        List<ScaffoldTemplate> all = templateRepository.findAll();
        if (all.isEmpty()) {
            templateRepository.save(fillDefaultTemplate(new ScaffoldTemplate()));
            return;
        }
        for (ScaffoldTemplate t : all) {
            if (needsEncodingRepair(t.getName()) || needsEncodingRepair(t.getTierAJson())) {
                templateRepository.save(fillDefaultTemplate(t));
            }
        }
    }

    private ScaffoldTemplate fillDefaultTemplate(ScaffoldTemplate t) {
        t.setName(DEFAULT_TEMPLATE_NAME);
        t.setTierAJson(DEFAULT_TIER_A);
        t.setTierBJson(DEFAULT_TIER_B);
        t.setTierCJson(DEFAULT_TIER_C);
        return t;
    }

    /** latin1 误读 UTF-8 后只剩 æ/ç/å，不含汉字 */
    private boolean needsEncodingRepair(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return false;
            }
        }
        return text.indexOf('\u00e6') >= 0 || text.indexOf('\u00e7') >= 0
                || text.indexOf('\u00e5') >= 0 || text.indexOf('\u00c3') >= 0;
    }

    private Map<String, Object> toTemplateMap(ScaffoldTemplate t) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("tierA", readJson(t.getTierAJson()));
        m.put("tierB", readJson(t.getTierBJson()));
        m.put("tierC", readJson(t.getTierCJson()));
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    private Map<String, Object> defaultTierDefs() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        List<ScaffoldTemplate> all = templateRepository.findAll();
        if (!all.isEmpty()) {
            ScaffoldTemplate t = all.get(0);
            m.put("A", readJson(t.getTierAJson()));
            m.put("B", readJson(t.getTierBJson()));
            m.put("C", readJson(t.getTierCJson()));
            return m;
        }
        return m;
    }

    private Object readJson(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj == null ? new HashMap<String, Object>() : obj);
        } catch (Exception e) {
            throw new BusinessException("内容无法保存");
        }
    }

    private List<Map<String, Object>> parseItems(String json) {
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() { });
            return list == null ? new ArrayList<Map<String, Object>>() : list;
        } catch (Exception e) {
            return new ArrayList<Map<String, Object>>();
        }
    }

    private boolean isTier(String v) {
        return "A".equalsIgnoreCase(v) || "B".equalsIgnoreCase(v) || "C".equalsIgnoreCase(v);
    }

    private int asInt(Object v) {
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return 0;
        }
    }

    private String asText(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> normalizeHints(Object raw) {
        List<String> out = new ArrayList<String>();
        if (raw instanceof List) {
            for (Object item : (List<Object>) raw) {
                String text = asText(item);
                if (StringUtils.hasText(text)) {
                    out.add(text);
                }
            }
        }
        return out;
    }
}
