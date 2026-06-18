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
