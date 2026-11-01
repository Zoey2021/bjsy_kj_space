package com.itech.learnspace.service;

import com.itech.learnspace.dto.ActivitySlotRequest;
import com.itech.learnspace.dto.GenerateActivitiesRequest;
import com.itech.learnspace.dto.LessonSaveRequest;
import com.itech.learnspace.dto.PublishActivityRequest;
import com.itech.learnspace.dto.ResourceSaveRequest;
import com.itech.learnspace.dto.TaskSaveRequest;
import com.itech.learnspace.config.ActivityProperties;
import com.itech.learnspace.entity.*;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherCourseService {

    private final CourseGradeRepository gradeRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseResourceRepository resourceRepository;
    private final CourseTaskRepository taskRepository;
    private final CourseService courseService;
    private final LearnSubmissionRepository submissionRepository;
    private final LearnProgressRepository progressRepository;
    private final SysUserRepository userRepository;
    private final ActivityHtmlService activityHtmlService;
    private final DifyInteractiveService difyInteractiveService;
    private final ActivityProperties activityProperties;
    private final CourseActivityPublishRepository activityPublishRepository;
    private final SysClassRepository classRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TeacherCourseService(CourseGradeRepository gradeRepository,
                                CourseUnitRepository unitRepository,
                                CourseLessonRepository lessonRepository,
                                CourseResourceRepository resourceRepository,
                                CourseTaskRepository taskRepository,
                                CourseService courseService,
                                LearnSubmissionRepository submissionRepository,
                                LearnProgressRepository progressRepository,
                                SysUserRepository userRepository,
                                ActivityHtmlService activityHtmlService,
                                DifyInteractiveService difyInteractiveService,
                                ActivityProperties activityProperties,
                                CourseActivityPublishRepository activityPublishRepository,
                                SysClassRepository classRepository) {
        this.gradeRepository = gradeRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
        this.resourceRepository = resourceRepository;
        this.taskRepository = taskRepository;
        this.courseService = courseService;
        this.submissionRepository = submissionRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.activityHtmlService = activityHtmlService;
        this.difyInteractiveService = difyInteractiveService;
        this.activityProperties = activityProperties;
        this.activityPublishRepository = activityPublishRepository;
        this.classRepository = classRepository;
    }

    public List<Map<String, Object>> listTextbooks(String type) {
        return courseService.listTextbooks(type);
    }

    public Map<String, Object> getGradeOutline(Long gradeId) {
        return courseService.getGradeOutline(gradeId, null);
    }

    public Map<String, Object> getLessonEditorData(Long lessonId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        CourseUnit unit = unitRepository.findById(lesson.getUnitId())
                .orElseThrow(() -> new BusinessException("单元不存在"));
        CourseGrade grade = gradeRepository.findById(unit.getGradeId())
                .orElseThrow(() -> new BusinessException("册次不存在"));

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("lesson", lesson);
        result.put("resources", resourceRepository.findByLessonIdOrderBySortOrderAsc(lessonId));
        result.put("tasks", taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId));
        result.put("unit", unit);
        result.put("grade", grade);
        return result;
    }

    @Transactional
    public CourseLesson createLesson(LessonSaveRequest req) {
        if (req.getUnitId() == null || !StringUtils.hasText(req.getTitle())) {
            throw new BusinessException("请选择单元并填写课时标题");
        }
        unitRepository.findById(req.getUnitId())
                .orElseThrow(() -> new BusinessException("单元不存在"));
        CourseLesson lesson = new CourseLesson();
        lesson.setUnitId(req.getUnitId());
        lesson.setTitle(req.getTitle().trim());
        lesson.setContent(req.getContent());
        lesson.setDurationMin(req.getDurationMin() != null ? req.getDurationMin() : 40);
        lesson.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : nextLessonSort(req.getUnitId()));
        lesson.setStatus(1);
        return lessonRepository.save(lesson);
    }

    @Transactional
    public CourseLesson updateLesson(Long lessonId, LessonSaveRequest req) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        if (StringUtils.hasText(req.getTitle())) {
            lesson.setTitle(req.getTitle().trim());
        }
        if (req.getContent() != null) {
            lesson.setContent(req.getContent());
        }
        if (req.getDurationMin() != null) {
            lesson.setDurationMin(req.getDurationMin());
        }
        return lessonRepository.save(lesson);
    }

    @Transactional
    public CourseResource saveResource(ResourceSaveRequest req) {
        if (req.getLessonId() == null || !StringUtils.hasText(req.getTitle())) {
            throw new BusinessException("课时与资源标题不能为空");
        }
        lessonRepository.findById(req.getLessonId())
                .orElseThrow(() -> new BusinessException("课时不存在"));
        CourseResource res = new CourseResource();
        res.setLessonId(req.getLessonId());
        res.setTitle(req.getTitle().trim());
        res.setResType(StringUtils.hasText(req.getResType()) ? req.getResType() : "TEXT");
        res.setContentText(req.getContentText());
        res.setContentUrl(req.getContentUrl());
        res.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        return resourceRepository.save(res);
    }

    @Transactional
    public CourseResource updateResource(Long id, ResourceSaveRequest req) {
        CourseResource res = resourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("资源不存在"));
        if (StringUtils.hasText(req.getTitle())) {
            res.setTitle(req.getTitle().trim());
        }
        if (req.getResType() != null) {
            res.setResType(req.getResType());
        }
        if (req.getContentText() != null) {
            res.setContentText(req.getContentText());
        }
        if (req.getContentUrl() != null) {
            res.setContentUrl(req.getContentUrl());
        }
        if (req.getSortOrder() != null) {
            res.setSortOrder(req.getSortOrder());
        }
        return resourceRepository.save(res);
    }

    @Transactional
    public void deleteResource(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new BusinessException("资源不存在");
        }
        resourceRepository.deleteById(id);
    }

    @Transactional
    public List<CourseTask> createTasks(Long lessonId, List<TaskSaveRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("请至少选择一项活动");
        }
        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));

        int sort = nextTaskSort(lessonId);
        List<CourseTask> saved = new ArrayList<CourseTask>();
        for (TaskSaveRequest req : requests) {
            if (!StringUtils.hasText(req.getTitle())) {
                continue;
            }
            CourseTask task = new CourseTask();
            task.setLessonId(lessonId);
            task.setTitle(req.getTitle().trim());
            task.setDescription(req.getDescription());
            task.setTaskType(StringUtils.hasText(req.getTaskType()) ? req.getTaskType() : "FORM");
            task.setConfigJson(StringUtils.hasText(req.getConfigJson())
                    ? req.getConfigJson()
                    : "{\"fields\":[{\"name\":\"content\",\"label\":\"我的作答\",\"type\":\"textarea\",\"required\":true}]}");
            task.setMaxScore(req.getMaxScore() != null ? req.getMaxScore() : 10);
            task.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : sort++);
            saved.add(taskRepository.save(task));
        }
        if (saved.isEmpty()) {
            throw new BusinessException("没有有效的活动可保存");
        }
        return saved;
    }

    /**
     * 课时配置页：读取当前已发布探究（含平台预置课）。
     */
    public Map<String, Object> getLessonHtmlConfig(Long lessonId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        CourseTask external = findExternalTask(lessonId);
        Map<String, Object> cfg = readConfigMap(external);
        List<Map<String, Object>> activities = extractActivityList(cfg);

        String source = "empty";
        if (!activities.isEmpty()) {
            String firstPath = String.valueOf(activities.get(0).get("path"));
            if (firstPath.contains("/lessons/generated/lesson-" + lessonId + "-pack/")) {
                source = "teacher_pack";
            } else if (firstPath.contains("/lessons/generated/")) {
                source = "generated";
            } else {
                source = "platform";
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("lessonId", lessonId);
        result.put("lessonTitle", lesson.getTitle());
        result.put("status", activities.isEmpty() ? "empty" : "published");
        result.put("source", source);
        result.put("objectives", cfg.get("objectives") == null ? "" : String.valueOf(cfg.get("objectives")));
        result.put("activityCount", Integer.valueOf(activities.size()));
        result.put("activities", activities);
        result.put("hasRichWorkspace", Boolean.valueOf(
                cfg.containsKey("quiz") || cfg.containsKey("evaluationForm") || cfg.containsKey("introMap")));
        return result;
    }

    /** 仅修改某一探究标题（保留测验、评价等其它配置） */
    @Transactional
    public Map<String, Object> updateLessonActivityTitle(Long lessonId, int seq, String title) {
        if (seq < 1 || seq > 3) {
            throw new BusinessException("探究序号无效");
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessException("请填写探究标题");
        }
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        CourseTask external = findExternalTask(lessonId);
        if (external == null || !StringUtils.hasText(external.getConfigJson())) {
            throw new BusinessException("本课尚未配置探究活动");
        }
        Map<String, Object> cfg = readConfigMap(external);
        List<Map<String, Object>> activities = extractActivityList(cfg);
        boolean found = false;
        for (Map<String, Object> act : activities) {
            int idx = toInt(act.get("index"), toInt(act.get("seq"), 0));
            if (idx == seq) {
                act.put("title", title.trim());
                found = true;
                break;
            }
        }
        if (!found) {
            throw new BusinessException("未找到探究 " + seq);
        }
        cfg.put("activities", activities);
        if (!StringUtils.hasText(String.valueOf(cfg.get("lessonTitle")))) {
            cfg.put("lessonTitle", lesson.getTitle());
        }
        external.setConfigJson(writeConfigMap(cfg));
        taskRepository.save(external);
        return getLessonHtmlConfig(lessonId);
    }

    /** 保存学习目标（合并进现有 workspace 配置） */
    @Transactional
    public Map<String, Object> updateLessonHtmlMeta(Long lessonId, Map<String, Object> body) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        CourseTask external = findExternalTask(lessonId);
        if (external == null || !StringUtils.hasText(external.getConfigJson())) {
            throw new BusinessException("本课尚未配置探究活动");
        }
        Map<String, Object> cfg = readConfigMap(external);
        if (body != null && body.get("objectives") != null) {
            cfg.put("objectives", String.valueOf(body.get("objectives")).trim());
        }
        if (!StringUtils.hasText(String.valueOf(cfg.get("lessonTitle")))) {
            cfg.put("lessonTitle", lesson.getTitle());
        }
        external.setConfigJson(writeConfigMap(cfg));
        taskRepository.save(external);
        return getLessonHtmlConfig(lessonId);
    }

    /**
     * 单独替换某一探究 HTML（写入 pack，并只更新该槽 path；不覆盖其它探究与测验配置）。
     */
    @Transactional
    public Map<String, Object> replaceLessonActivityHtml(Long lessonId, int seq, MultipartFile file) {
        if (seq < 1 || seq > 3) {
            throw new BusinessException("探究序号无效");
        }
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择 HTML 文件");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase(Locale.ROOT).endsWith(".html")) {
            throw new BusinessException("仅支持 .html 文件");
        }
        String html;
        try {
            html = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("读取文件失败：" + e.getMessage());
        }
        if (!StringUtils.hasText(html)) {
            throw new BusinessException("HTML 内容为空");
        }
        if (!html.toLowerCase(Locale.ROOT).contains("<html")) {
            html = wrapHtmlFragment(html, "探究" + seq);
        }

        String publishSlug = "lesson-" + lessonId + "-pack";
        Path publishDir = Paths.get(activityProperties.getHtmlOutputDir(), publishSlug);
        String actFile = "act" + seq + ".html";
        String publicPath = "/lessons/generated/" + publishSlug + "/" + actFile;
        try {
            Files.createDirectories(publishDir);
            Files.write(publishDir.resolve(actFile), html.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BusinessException("保存失败：" + e.getMessage());
        }

        CourseTask external = findExternalTask(lessonId);
        Map<String, Object> cfg;
        if (external == null) {
            external = new CourseTask();
            external.setLessonId(lessonId);
            external.setSortOrder(1);
            external.setMaxScore(100);
            external.setTitle(lesson.getTitle() + " · 探究");
            external.setDescription("教师配置的探究活动");
            external.setTaskType("EXTERNAL");
            cfg = new HashMap<String, Object>();
            cfg.put("layout", "student_workspace");
            cfg.put("lessonTitle", lesson.getTitle());
            cfg.put("objectives", "");
            cfg.put("evaluationHint", "完成探究后，请填写学习评价并作答课堂小测。");
            cfg.put("activities", new ArrayList<Map<String, Object>>());
        } else {
            cfg = readConfigMap(external);
            if (!StringUtils.hasText(String.valueOf(cfg.get("layout")))) {
                cfg.put("layout", "student_workspace");
            }
        }

        List<Map<String, Object>> activities = extractActivityList(cfg);
        Map<String, Object> target = null;
        for (Map<String, Object> act : activities) {
            int idx = toInt(act.get("index"), toInt(act.get("seq"), 0));
            if (idx == seq) {
                target = act;
                break;
            }
        }
        if (target == null) {
            target = new HashMap<String, Object>();
            target.put("index", Integer.valueOf(seq));
            target.put("step", Integer.valueOf(seq));
            target.put("title", "探究" + seq);
            target.put("unlocked", Boolean.TRUE);
            activities.add(target);
            // 按 index 排序
            Collections.sort(activities, new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> a, Map<String, Object> b) {
                    return Integer.compare(
                            toInt(a.get("index"), toInt(a.get("seq"), 0)),
                            toInt(b.get("index"), toInt(b.get("seq"), 0)));
                }
            });
        }
        target.put("index", Integer.valueOf(seq));
        target.put("step", Integer.valueOf(seq));
        target.put("path", publicPath);
        target.put("unlocked", Boolean.TRUE);
        if (!StringUtils.hasText(String.valueOf(target.get("title")))) {
            target.put("title", "探究" + seq);
        }

        cfg.put("activities", activities);
        if (!StringUtils.hasText(String.valueOf(cfg.get("lessonTitle")))) {
            cfg.put("lessonTitle", lesson.getTitle());
        }
        external.setConfigJson(writeConfigMap(cfg));
        external.setTaskType("EXTERNAL");
        taskRepository.save(external);

        Map<String, Object> result = getLessonHtmlConfig(lessonId);
        result.put("replacedSeq", Integer.valueOf(seq));
        result.put("replacedPath", publicPath);
        return result;
    }

    /**
     * 教师上传整课 HTML，自动拆成 2～3 个探究草稿，供预览确认。
     */
    public Map<String, Object> uploadAndSplitHtml(Long lessonId, MultipartFile file) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择 HTML 文件");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase(Locale.ROOT).endsWith(".html")) {
            throw new BusinessException("仅支持 .html 文件");
        }
        String html;
        try {
            html = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("读取文件失败：" + e.getMessage());
        }
        if (!StringUtils.hasText(html)) {
            throw new BusinessException("HTML 内容为空");
        }

        List<Map<String, String>> parts = splitTeacherHtml(html);
        if (parts.isEmpty()) {
            throw new BusinessException("未能从 HTML 中拆出探究内容");
        }
        if (parts.size() > 3) {
            parts = new ArrayList<Map<String, String>>(parts.subList(0, 3));
        }
        if (parts.size() == 1) {
            // 尽量保证至少有两个探究入口：复制一份并改标题，便于教师后续替换
            Map<String, String> second = new HashMap<String, String>();
            second.put("title", "探究二");
            second.put("html", wrapHtmlFragment(parts.get(0).get("html"), "探究二"));
            parts.add(second);
        }

        String draftSlug = "lesson-" + lessonId + "-draft";
        Path dir = Paths.get(activityProperties.getHtmlOutputDir(), draftSlug);
        List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        try {
            Files.createDirectories(dir);
            // 保留原稿
            Files.write(dir.resolve("source.html"), html.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < parts.size(); i++) {
                int seq = i + 1;
                String actFile = "act" + seq + ".html";
                String body = parts.get(i).get("html");
                Files.write(dir.resolve(actFile), body.getBytes(StandardCharsets.UTF_8));
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("seq", Integer.valueOf(seq));
                item.put("title", parts.get(i).get("title"));
                item.put("previewPath", "/lessons/generated/" + draftSlug + "/" + actFile);
                activities.add(item);
            }
        } catch (IOException e) {
            throw new BusinessException("保存草稿失败：" + e.getMessage());
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("lessonId", lessonId);
        result.put("lessonTitle", lesson.getTitle());
        result.put("draftSlug", draftSlug);
        result.put("count", Integer.valueOf(activities.size()));
        result.put("activities", activities);
        result.put("tip", "已拆成 " + activities.size() + " 个探究，请预览确认标题后发布");
        return result;
    }

    /**
     * 确认草稿无误后，发布到该课时（学生课程地图进入本课即可看到）。
     */
    @Transactional
    public Map<String, Object> publishHtmlPack(Long lessonId, Map<String, Object> body, Long teacherId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        String draftSlug = "lesson-" + lessonId + "-draft";
        Path draftDir = Paths.get(activityProperties.getHtmlOutputDir(), draftSlug);
        if (!Files.isDirectory(draftDir)) {
            throw new BusinessException("请先上传并拆分 HTML");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> titleOverrides = body == null ? null
                : (List<Map<String, Object>>) body.get("activities");
        List<Map<String, Object>> activityMeta = new ArrayList<Map<String, Object>>();
        String publishSlug = "lesson-" + lessonId + "-pack";
        Path publishDir = Paths.get(activityProperties.getHtmlOutputDir(), publishSlug);

        try {
            Files.createDirectories(publishDir);
            for (int seq = 1; seq <= 3; seq++) {
                Path draftFile = draftDir.resolve("act" + seq + ".html");
                if (!Files.exists(draftFile)) {
                    break;
                }
                Path target = publishDir.resolve("act" + seq + ".html");
                Files.copy(draftFile, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                String title = "探究" + seq;
                if (titleOverrides != null) {
                    for (Map<String, Object> row : titleOverrides) {
                        Object s = row.get("seq");
                        if (s != null && Integer.parseInt(String.valueOf(s)) == seq
                                && row.get("title") != null
                                && StringUtils.hasText(String.valueOf(row.get("title")))) {
                            title = String.valueOf(row.get("title")).trim();
                        }
                    }
                }
                Map<String, Object> meta = new HashMap<String, Object>();
                meta.put("index", Integer.valueOf(seq));
                meta.put("step", Integer.valueOf(seq));
                meta.put("title", title);
                meta.put("path", "/lessons/generated/" + publishSlug + "/act" + seq + ".html");
                meta.put("unlocked", Boolean.TRUE);
                activityMeta.add(meta);
            }
        } catch (IOException e) {
            throw new BusinessException("发布失败：" + e.getMessage());
        }
        if (activityMeta.isEmpty()) {
            throw new BusinessException("没有可发布的探究页面");
        }

        String objectives = body != null && body.get("objectives") != null
                ? String.valueOf(body.get("objectives")).trim() : "";

        CourseResource web = null;
        for (CourseResource r : resourceRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if ("WEB".equals(r.getResType()) && r.getContentUrl() != null
                    && r.getContentUrl().contains("/lessons/generated/lesson-" + lessonId + "-pack/")) {
                web = r;
                break;
            }
        }
        if (web == null) {
            web = new CourseResource();
            web.setLessonId(lessonId);
            web.setSortOrder(1);
        }
        web.setTitle(lesson.getTitle() + " · 探究活动");
        web.setResType("WEB");
        web.setContentUrl(String.valueOf(activityMeta.get(0).get("path")));
        web = resourceRepository.save(web);

        CourseTask external = findExternalTask(lessonId);
        Map<String, Object> cfg = readConfigMap(external);
        if (cfg.isEmpty()) {
            cfg.put("layout", "student_workspace");
            cfg.put("evaluationHint", "完成探究后，请填写学习评价并作答课堂小测。");
        }
        if (!StringUtils.hasText(String.valueOf(cfg.get("layout")))) {
            cfg.put("layout", "student_workspace");
        }
        cfg.put("lessonTitle", lesson.getTitle());
        if (StringUtils.hasText(objectives)) {
            cfg.put("objectives", objectives);
        } else if (cfg.get("objectives") == null) {
            cfg.put("objectives", "");
        }
        // 整课覆盖只替换探究列表，保留测验 / 评价 / 导图等既有字段
        cfg.put("activities", activityMeta);

        if (external == null) {
            external = new CourseTask();
            external.setLessonId(lessonId);
            external.setSortOrder(1);
            external.setMaxScore(100);
        }
        external.setTitle(lesson.getTitle() + " · 探究");
        external.setDescription("教师上传 HTML 发布的探究活动");
        external.setTaskType("EXTERNAL");
        external.setConfigJson(writeConfigMap(cfg));
        external = taskRepository.save(external);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("lessonId", lessonId);
        result.put("lessonTitle", lesson.getTitle());
        result.put("resourceId", web.getId());
        result.put("taskId", external.getId());
        result.put("activityCount", Integer.valueOf(activityMeta.size()));
        result.put("activities", activityMeta);
        result.put("publishedBy", teacherId);
        return result;
    }

    private String buildStudentWorkspaceConfigJson(String lessonTitle, String objectives,
                                                   List<Map<String, Object>> activities) {
        try {
            Map<String, Object> cfg = new HashMap<String, Object>();
            cfg.put("layout", "student_workspace");
            cfg.put("lessonTitle", lessonTitle);
            cfg.put("objectives", objectives);
            cfg.put("evaluationHint", "完成探究后，请填写学习评价并作答课堂小测。");
            cfg.put("activities", activities);
            return objectMapper.writeValueAsString(cfg);
        } catch (Exception e) {
            return "{\"layout\":\"student_workspace\",\"activities\":[]}";
        }
    }

    private CourseTask findExternalTask(Long lessonId) {
        for (CourseTask t : taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId)) {
            if ("EXTERNAL".equals(t.getTaskType())) {
                return t;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readConfigMap(CourseTask external) {
        Map<String, Object> empty = new LinkedHashMap<String, Object>();
        if (external == null || !StringUtils.hasText(external.getConfigJson())) {
            return empty;
        }
        try {
            Map<String, Object> cfg = objectMapper.readValue(external.getConfigJson(), Map.class);
            return cfg == null ? empty : cfg;
        } catch (Exception e) {
            return empty;
        }
    }

    private String writeConfigMap(Map<String, Object> cfg) {
        try {
            return objectMapper.writeValueAsString(cfg);
        } catch (Exception e) {
            return "{\"layout\":\"student_workspace\",\"activities\":[]}";
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractActivityList(Map<String, Object> cfg) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (cfg == null || cfg.get("activities") == null) {
            return out;
        }
        Object raw = cfg.get("activities");
        if (!(raw instanceof List)) {
            return out;
        }
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<String, Object> src = (Map<String, Object>) item;
            int idx = toInt(src.get("index"), toInt(src.get("seq"), out.size() + 1));
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("seq", Integer.valueOf(idx));
            row.put("index", Integer.valueOf(idx));
            row.put("step", Integer.valueOf(toInt(src.get("step"), idx)));
            row.put("title", src.get("title") == null ? ("探究" + idx) : String.valueOf(src.get("title")));
            String path = src.get("path") == null ? "" : String.valueOf(src.get("path"));
            row.put("path", path);
            row.put("previewPath", path);
            row.put("unlocked", src.get("unlocked") == null ? Boolean.TRUE : src.get("unlocked"));
            out.add(row);
        }
        return out;
    }

    private static int toInt(Object v, int fallback) {
        if (v == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    /** 按「探究一/二/三」或标题标签拆分；不足时按大标题拆 */
    private List<Map<String, String>> splitTeacherHtml(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        int bodyStart = lower.indexOf("<body");
        int bodyOpenEnd = bodyStart >= 0 ? lower.indexOf(">", bodyStart) : -1;
        int bodyEnd = lower.lastIndexOf("</body>");
        String headStyles = extractHeadStyles(html);
        String body = (bodyOpenEnd >= 0 && bodyEnd > bodyOpenEnd)
                ? html.substring(bodyOpenEnd + 1, bodyEnd)
                : html;

        List<int[]> marks = new ArrayList<int[]>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "探究\\s*([一二三四五六123456])|探究活动\\s*([一二三四五六123456])");
        java.util.regex.Matcher m = p.matcher(body);
        while (m.find()) {
            marks.add(new int[] { m.start(), m.end() });
        }
        List<Map<String, String>> parts = new ArrayList<Map<String, String>>();
        if (marks.size() >= 2) {
            for (int i = 0; i < marks.size() && parts.size() < 3; i++) {
                int start = marks.get(i)[0];
                int end = (i + 1 < marks.size()) ? marks.get(i + 1)[0] : body.length();
                String fragment = body.substring(start, end).trim();
                String title = defaultInquiryTitle(parts.size() + 1);
                java.util.regex.Matcher tm = java.util.regex.Pattern
                        .compile("探究\\s*([一二三四五六123456][^\\s<]{0,12})")
                        .matcher(fragment);
                if (tm.find()) {
                    title = "探究" + tm.group(1).replaceAll("[：:].*$", "").trim();
                    if (title.length() > 16) {
                        title = defaultInquiryTitle(parts.size() + 1);
                    }
                }
                Map<String, String> row = new HashMap<String, String>();
                row.put("title", title);
                row.put("html", wrapHtmlFragment(fragment, title, headStyles));
                parts.add(row);
            }
            return parts;
        }

        // 按 h1/h2 拆
        java.util.regex.Pattern hp = java.util.regex.Pattern.compile("(?is)<h[12][^>]*>.*?</h[12]>");
        java.util.regex.Matcher hm = hp.matcher(body);
        List<Integer> heads = new ArrayList<Integer>();
        while (hm.find()) {
            heads.add(Integer.valueOf(hm.start()));
        }
        if (heads.size() >= 2) {
            for (int i = 0; i < heads.size() && parts.size() < 3; i++) {
                int start = heads.get(i).intValue();
                int end = (i + 1 < heads.size()) ? heads.get(i + 1).intValue() : body.length();
                String fragment = body.substring(start, end).trim();
                String title = defaultInquiryTitle(parts.size() + 1);
                java.util.regex.Matcher tm = java.util.regex.Pattern
                        .compile("(?is)<h[12][^>]*>(.*?)</h[12]>")
                        .matcher(fragment);
                if (tm.find()) {
                    String t = tm.group(1).replaceAll("<[^>]+>", "").trim();
                    if (StringUtils.hasText(t) && t.length() <= 20) {
                        title = t;
                    }
                }
                Map<String, String> row = new HashMap<String, String>();
                row.put("title", title);
                row.put("html", wrapHtmlFragment(fragment, title, headStyles));
                parts.add(row);
            }
            return parts;
        }

        Map<String, String> only = new HashMap<String, String>();
        only.put("title", defaultInquiryTitle(1));
        only.put("html", wrapHtmlFragment(body, defaultInquiryTitle(1), headStyles));
        parts.add(only);
        return parts;
    }

    private static String defaultInquiryTitle(int seq) {
        if (seq == 1) {
            return "探究一";
        }
        if (seq == 2) {
            return "探究二";
        }
        return "探究三";
    }

    private static String extractHeadStyles(String html) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?is)<style[^>]*>(.*?)</style>")
                .matcher(html);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            sb.append("<style>").append(m.group(1)).append("</style>\n");
        }
        return sb.toString();
    }

    private static String wrapHtmlFragment(String fragment, String title) {
        return wrapHtmlFragment(fragment, title, "");
    }

    private static String wrapHtmlFragment(String fragment, String title, String headStyles) {
        String safeTitle = title == null ? "探究" : title.replace("<", "").replace(">", "");
        return "<!DOCTYPE html>\n<html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + safeTitle + "</title>"
                + (headStyles == null ? "" : headStyles)
                + "<style>body{margin:16px;font-family:system-ui,-apple-system,sans-serif;line-height:1.6;color:#0f172a;background:#fff}"
                + "img{max-width:100%}table{border-collapse:collapse}</style>"
                + "</head><body>\n" + fragment + "\n</body></html>";
    }

    /** 上传飞象老师 HTML（活动槽选用 feixiang 源时使用） */
    public Map<String, String> uploadFeixiangHtml(Long lessonId, MultipartFile file, int slotIndex) {
        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择 HTML 文件");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase(Locale.ROOT).endsWith(".html")) {
            throw new BusinessException("仅支持 .html 文件");
        }
        String uploadDir = "lesson-" + lessonId + "/uploads";
        String fileName = "slot" + slotIndex + "-" + System.currentTimeMillis() + ".html";
        Path target = Paths.get(activityProperties.getHtmlOutputDir(), uploadDir, fileName);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("上传失败：" + e.getMessage());
        }
        String publicPath = "/lessons/generated/" + uploadDir + "/" + fileName;
        Map<String, String> result = new HashMap<String, String>();
        result.put("uploadedPath", publicPath);
        result.put("fileName", fileName);
        return result;
    }

    /** 按教师勾选的活动槽生成互动页（草稿目录，供预览；同步时再发布到班级） */
    public Map<String, Object> generateActivitySlots(Long lessonId, GenerateActivitiesRequest req, Long teacherId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        String studentObjectives = resolveStudentObjectivesFromGenerate(req);
        List<ActivitySlotRequest> slots = filterEnabledSlots(req.getActivitySlots());
        if (slots.isEmpty()) {
            throw new BusinessException("请至少勾选并填写一个活动");
        }
        for (int i = 0; i < slots.size(); i++) {
            validateSlotForGenerate(slots.get(i), i + 1);
        }

        String slug = "lesson-" + lessonId + "-draft";
        Path dir = Paths.get(activityProperties.getHtmlOutputDir(), slug);
        List<Map<String, Object>> generated = new ArrayList<Map<String, Object>>();

        try {
            Files.createDirectories(dir);
            int seq = 1;
            for (ActivitySlotRequest slot : slots) {
                String actFile = "act" + seq + ".html";
                String publicPath = "/lessons/generated/" + slug + "/" + actFile;
                String html = resolveSlotHtml(lesson, studentObjectives, slot, seq, teacherId);
                Files.write(dir.resolve(actFile), html.getBytes(StandardCharsets.UTF_8));
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("seq", seq);
                item.put("title", StringUtils.hasText(slot.getTitle()) ? slot.getTitle() : ("活动" + seq));
                item.put("previewPath", publicPath);
                generated.add(item);
                seq++;
            }
        } catch (IOException e) {
            throw new BusinessException("生成活动页失败：" + e.getMessage());
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("lessonId", lessonId);
        result.put("draftSlug", slug);
        result.put("activities", generated);
        result.put("count", generated.size());
        return result;
    }

    /** 教师确认后同步到班级：仅启用活动 + 侧栏学习布局 */
    @Transactional
    public Map<String, Object> publishActivity(Long lessonId, PublishActivityRequest req, Long teacherId) {
        if (req.getClassId() == null) {
            throw new BusinessException("请选择班级");
        }
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        SysClass clazz = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new BusinessException("班级不存在"));
        if (clazz.getTeacherId() != null && !clazz.getTeacherId().equals(teacherId)) {
            throw new BusinessException("只能发布到自己负责的班级");
        }

        String studentObjectives = resolveStudentObjectives(req);
        String evaluation = resolveEvaluation(req);
        List<ActivitySlotRequest> slots = filterEnabledSlots(req.getActivitySlots());
        if (slots.isEmpty()) {
            throw new BusinessException("请至少勾选一个学习活动");
        }
        for (int i = 0; i < slots.size(); i++) {
            validateSlotForGenerate(slots.get(i), i + 1);
        }

        String slug = "lesson-" + lessonId + "-class-" + req.getClassId();
        String draftSlug = "lesson-" + lessonId + "-draft";
        Path dir = Paths.get(activityProperties.getHtmlOutputDir(), slug);
        Path draftDir = Paths.get(activityProperties.getHtmlOutputDir(), draftSlug);
        List<Map<String, Object>> activityMeta = new ArrayList<Map<String, Object>>();

        try {
            Files.createDirectories(dir);
            int seq = 1;
            for (ActivitySlotRequest slot : slots) {
                String actFile = "act" + seq + ".html";
                String publicActPath = "/lessons/generated/" + slug + "/" + actFile;
                Path draftFile = draftDir.resolve(actFile);
                Path targetFile = dir.resolve(actFile);
                if (Files.exists(draftFile)) {
                    Files.copy(draftFile, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } else {
                    String html = resolveSlotHtml(lesson, studentObjectives, slot, seq, teacherId);
                    Files.write(targetFile, html.getBytes(StandardCharsets.UTF_8));
                }
                Map<String, Object> meta = new HashMap<String, Object>();
                meta.put("index", seq);
                meta.put("title", StringUtils.hasText(slot.getTitle()) ? slot.getTitle().trim() : ("活动" + seq));
                meta.put("path", publicActPath);
                activityMeta.add(meta);
                seq++;
            }
        } catch (IOException e) {
            throw new BusinessException("同步学生活动失败：" + e.getMessage());
        }

        String publicPath = "/lessons/generated/" + slug + "/act1.html";
        String resourceTitle = StringUtils.hasText(req.getActivityTitle())
                ? req.getActivityTitle().trim()
                : lesson.getTitle() + " · 课堂学习";

        String configJson = buildSidebarConfigJson(lesson.getTitle(), studentObjectives, evaluation, activityMeta);

        CourseActivityPublish publish = activityPublishRepository
                .findByLessonIdAndClassId(lessonId, req.getClassId())
                .orElse(new CourseActivityPublish());

        CourseResource resource;
        if (publish.getResourceId() != null) {
            resource = resourceRepository.findById(publish.getResourceId())
                    .orElseGet(CourseResource::new);
        } else {
            resource = new CourseResource();
            resource.setLessonId(lessonId);
            resource.setSortOrder(10);
        }
        resource.setTitle(resourceTitle);
        resource.setResType("WEB");
        resource.setContentUrl(publicPath);
        resource = resourceRepository.save(resource);

        CourseTask task;
        if (publish.getTaskId() != null) {
            task = taskRepository.findById(publish.getTaskId())
                    .orElseGet(CourseTask::new);
        } else {
            task = new CourseTask();
            task.setLessonId(lessonId);
            task.setSortOrder(10);
        }
        task.setTitle(resourceTitle);
        task.setDescription("课堂学习 · 侧栏导航");
        task.setTaskType("EXTERNAL");
        task.setConfigJson(configJson);
        task.setMaxScore(100);
        task = taskRepository.save(task);

        publish.setLessonId(lessonId);
        publish.setClassId(req.getClassId());
        publish.setResourceId(resource.getId());
        publish.setTaskId(task.getId());
        publish.setHtmlPath(publicPath);
        publish.setPublishedBy(teacherId);
        publish.setPublishedAt(new Date());
        try {
            Map<String, Object> plan = new HashMap<String, Object>();
            plan.put("layout", "sidebar");
            plan.put("studentObjectives", studentObjectives);
            plan.put("evaluation", evaluation);
            plan.put("activities", activityMeta);
            publish.setPlanJson(objectMapper.writeValueAsString(plan));
        } catch (Exception ignored) {
            publish.setPlanJson(null);
        }
        activityPublishRepository.save(publish);

        long studentCount = userRepository.findByClassId(req.getClassId()).size();
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("lessonId", lessonId);
        result.put("classId", req.getClassId());
        result.put("className", clazz.getName());
        result.put("htmlPath", publicPath);
        result.put("resourceId", resource.getId());
        result.put("taskId", task.getId());
        result.put("studentCount", studentCount);
        result.put("activityCount", activityMeta.size());
        return result;
    }

    private String resolveStudentObjectivesFromGenerate(GenerateActivitiesRequest req) {
        if (StringUtils.hasText(req.getStudentObjectives())) {
            return req.getStudentObjectives().trim();
        }
        if (StringUtils.hasText(req.getObjectives())) {
            return req.getObjectives().trim();
        }
        return "";
    }

    private void validateSlotForGenerate(ActivitySlotRequest slot, int displayIndex) {
        if ("feixiang".equals(slot.getSource()) && !StringUtils.hasText(slot.getUploadedPath())) {
            throw new BusinessException("活动" + displayIndex + "：请先上传飞象网页");
        }
        if (!"feixiang".equals(slot.getSource()) && !StringUtils.hasText(slot.getContent())) {
            throw new BusinessException("活动" + displayIndex + "：请填写活动描述");
        }
    }

    private List<ActivitySlotRequest> filterEnabledSlots(List<ActivitySlotRequest> slots) {
        List<ActivitySlotRequest> out = new ArrayList<ActivitySlotRequest>();
        if (slots == null) {
            return out;
        }
        for (ActivitySlotRequest slot : slots) {
            if (slot == null) {
                continue;
            }
            if (slot.getEnabled() != null && !slot.getEnabled()) {
                continue;
            }
            out.add(slot);
        }
        return out;
    }

    private String buildSidebarConfigJson(String lessonTitle, String objectives, String evaluation,
                                          List<Map<String, Object>> activities) {
        try {
            Map<String, Object> cfg = new HashMap<String, Object>();
            cfg.put("layout", "sidebar");
            cfg.put("lessonTitle", lessonTitle);
            cfg.put("objectives", objectives);
            cfg.put("evaluation", evaluation);
            cfg.put("activities", activities);
            return objectMapper.writeValueAsString(cfg);
        } catch (Exception e) {
            return "{\"layout\":\"sidebar\",\"path\":\"\"}";
        }
    }

    private String resolveTeacherObjectives(PublishActivityRequest req) {
        if (StringUtils.hasText(req.getTeacherObjectives())) {
            return req.getTeacherObjectives().trim();
        }
        if (StringUtils.hasText(req.getObjectives())) {
            return req.getObjectives().trim();
        }
        return "";
    }

    private String resolveStudentObjectives(PublishActivityRequest req) {
        if (StringUtils.hasText(req.getStudentObjectives())) {
            return req.getStudentObjectives().trim();
        }
        if (StringUtils.hasText(req.getObjectives())) {
            return req.getObjectives().trim();
        }
        return "";
    }

    private String resolveEvaluation(PublishActivityRequest req) {
        if (StringUtils.hasText(req.getEvaluation())) {
            return req.getEvaluation().trim();
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(req.getProgress())) {
            sb.append("学习后的进步：\n").append(req.getProgress().trim()).append("\n\n");
        }
        if (StringUtils.hasText(req.getLiteracy())) {
            sb.append("核心素养：\n").append(req.getLiteracy().trim());
        }
        return sb.toString().trim();
    }

    private List<ActivitySlotRequest> normalizeActivitySlots(PublishActivityRequest req) {
        List<ActivitySlotRequest> slots = filterEnabledSlots(req.getActivitySlots());
        if (slots.isEmpty() && StringUtils.hasText(req.getActivities())) {
            ActivitySlotRequest one = new ActivitySlotRequest();
            one.setTitle("活动1");
            one.setSource("platform");
            one.setContent(req.getActivities());
            one.setEnabled(true);
            slots.add(one);
        }
        if (slots.size() > 3) {
            return new ArrayList<ActivitySlotRequest>(slots.subList(0, 3));
        }
        return slots;
    }

    private String resolveSlotHtml(CourseLesson lesson, String studentObjectives,
                                   ActivitySlotRequest slot, int index, Long teacherId) {
        String source = StringUtils.hasText(slot.getSource()) ? slot.getSource().trim() : "platform";
        String title = StringUtils.hasText(slot.getTitle()) ? slot.getTitle().trim() : ("活动" + index);
        String content = slot.getContent() != null ? slot.getContent() : "";

        if ("feixiang".equals(source)) {
            if (!StringUtils.hasText(slot.getUploadedPath())) {
                throw new BusinessException("活动" + index + "：请先上传飞象互动网页");
            }
            return readUploadedHtml(slot.getUploadedPath());
        }
        if ("dify".equals(source)) {
            String difyHtml = difyInteractiveService.generateActivityHtml(
                    lesson.getTitle(), studentObjectives, title, content,
                    "teacher-" + teacherId);
            if (StringUtils.hasText(difyHtml)) {
                return difyHtml;
            }
        }
        return activityHtmlService.generateMiniActivity(title, content);
    }

    private String readUploadedHtml(String publicPath) {
        String prefix = "/lessons/generated/";
        if (!publicPath.startsWith(prefix)) {
            throw new BusinessException("无效的上传路径");
        }
        String rel = publicPath.substring(prefix.length());
        Path file = Paths.get(activityProperties.getHtmlOutputDir(), rel);
        if (!Files.exists(file)) {
            throw new BusinessException("上传的 HTML 文件不存在，请重新上传");
        }
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败：" + e.getMessage());
        }
    }

    /** 某班在某课时的学情（飞象等外部活动提交） */
    public Map<String, Object> getLessonClassStats(Long lessonId, Long classId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("课时不存在"));
        List<SysUser> students = userRepository.findByClassId(classId);
        List<CourseTask> tasks = taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
        List<Long> taskIds = tasks.stream().map(CourseTask::getId).collect(Collectors.toList());

        Map<Long, LearnSubmission> subByStudent = new HashMap<Long, LearnSubmission>();
        if (!taskIds.isEmpty()) {
            for (LearnSubmission s : submissionRepository.findByTaskIdIn(taskIds)) {
                if (taskIds.contains(s.getTaskId())) {
                    subByStudent.put(s.getStudentId(), s);
                }
            }
        }

        int submitted = 0;
        int scoreSum = 0;
        double rateSum = 0;
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

        for (SysUser stu : students) {
            LearnSubmission sub = subByStudent.get(stu.getId());
            LearnProgress prog = progressRepository.findByStudentIdAndLessonId(stu.getId(), lessonId).orElse(null);
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("studentId", stu.getId());
            row.put("realName", stu.getRealName());
            row.put("status", prog != null ? prog.getStatus() : "NOT_STARTED");
            row.put("progressPercent", prog != null ? prog.getProgressPercent() : 0);

            if (sub != null) {
                submitted++;
                row.put("submitted", true);
                row.put("score", sub.getScore());
                row.put("submittedAt", sub.getSubmittedAt());
                double rate = parseCorrectRate(sub.getContentJson());
                row.put("correctRate", rate);
                scoreSum += sub.getScore() != null ? sub.getScore() : 0;
                rateSum += rate;
            } else {
                row.put("submitted", false);
                row.put("score", null);
                row.put("correctRate", null);
            }
            rows.add(row);
        }

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("lessonId", lessonId);
        summary.put("lessonTitle", lesson.getTitle());
        summary.put("classId", classId);
        summary.put("totalStudents", students.size());
        summary.put("submittedCount", submitted);
        summary.put("completionRate", students.isEmpty() ? 0
                : Math.round(submitted * 1000.0 / students.size()) / 10.0);
        summary.put("avgScore", submitted > 0 ? Math.round(scoreSum * 10.0 / submitted) / 10.0 : 0);
        summary.put("avgCorrectRate", submitted > 0 ? Math.round(rateSum * 1000.0 / submitted) / 10.0 : 0);
        summary.put("students", rows);
        return summary;
    }

    private double parseCorrectRate(String contentJson) {
        if (!StringUtils.hasText(contentJson)) {
            return 0;
        }
        try {
            JsonNode n = objectMapper.readTree(contentJson);
            if (n.has("correctRate")) {
                double r = n.get("correctRate").asDouble();
                return r <= 1 ? r * 100 : r;
            }
            if (n.has("quizScore")) {
                return n.get("quizScore").asDouble();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private int nextLessonSort(Long unitId) {
        List<CourseLesson> list = lessonRepository.findByUnitIdOrderBySortOrderAsc(unitId);
        int max = 0;
        for (CourseLesson l : list) {
            if (l.getSortOrder() != null && l.getSortOrder() > max) {
                max = l.getSortOrder();
            }
        }
        return max + 1;
    }

    private int nextTaskSort(Long lessonId) {
        List<CourseTask> list = taskRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
        int max = 0;
        for (CourseTask t : list) {
            if (t.getSortOrder() != null && t.getSortOrder() > max) {
                max = t.getSortOrder();
            }
        }
        return max + 1;
    }
}
