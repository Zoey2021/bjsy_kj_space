package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.dto.InterveneRequest;
import com.itech.learnspace.entity.LearnNotification;
import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.entity.CourseLesson;
import com.itech.learnspace.repository.CourseLessonRepository;
import com.itech.learnspace.repository.LearnNotificationRepository;
import com.itech.learnspace.repository.SysClassRepository;
import com.itech.learnspace.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final LearnNotificationRepository notificationRepository;
    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final CourseLessonRepository lessonRepository;
    private final DashboardService dashboardService;
    private final SseService sseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificationService(LearnNotificationRepository notificationRepository,
                               SysUserRepository userRepository,
                               SysClassRepository classRepository,
                               CourseLessonRepository lessonRepository,
                               DashboardService dashboardService,
                               SseService sseService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.lessonRepository = lessonRepository;
        this.dashboardService = dashboardService;
        this.sseService = sseService;
    }

    @Transactional
    public LearnNotification intervene(InterveneRequest req, Long teacherId) {
        if (!StringUtils.hasText(req.getType())) {
            throw new BusinessException("消息类型不能为空");
        }
        Long classId = req.getClassId();
        if (classId == null) {
            throw new BusinessException("班级不能为空");
        }
        dashboardService.checkTeacherOwnsClass(teacherId, classId);

        String message = buildMessage(req);
        Map<String, Object> payload = new HashMap<>();
        payload.put("lessonId", req.getLessonId());
        payload.put("activityIndex", req.getActivityIndex());
        payload.put("content", req.getContent());
        payload.put("message", message);

        LearnNotification notification = new LearnNotification();
        notification.setClassId(classId);
        notification.setSenderId(teacherId);
        notification.setMsgType(req.getType());
        notification.setMessage(message);
        try {
            notification.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (Exception ignored) {
            notification.setPayloadJson("{}");
        }

        if ("broadcast".equals(req.getType())) {
            notification.setTargetStudentId(null);
            notificationRepository.save(notification);
            List<Long> studentIds = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1)
                    .stream().map(SysUser::getId).collect(Collectors.toList());
            Map<String, Object> data = toClientMap(notification);
            sseService.pushToStudents(studentIds, req.getType(), data);
        } else {
            if (req.getTargetStudentId() == null) {
                throw new BusinessException("请指定目标学生");
            }
            notification.setTargetStudentId(req.getTargetStudentId());
            notificationRepository.save(notification);
            sseService.pushToStudent(req.getTargetStudentId(), req.getType(), toClientMap(notification));
        }
        return notification;
    }

    public List<Map<String, Object>> getNotificationsSince(Long studentId, Long sinceMs) {
        LocalDateTime since = sinceMs != null && sinceMs > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(sinceMs), ZoneId.systemDefault())
                : LocalDateTime.now().minusDays(7);

        SysUser student = userRepository.findById(studentId).orElse(null);
        Long classId = student != null ? student.getClassId() : null;

        List<LearnNotification> direct = notificationRepository
                .findByTargetStudentIdAndCreatedAtAfterOrderByCreatedAtAsc(studentId, since);
        List<LearnNotification> broadcast = classId != null
                ? notificationRepository.findByTargetStudentIdIsNullAndClassIdAndCreatedAtAfterOrderByCreatedAtAsc(
                classId, since)
                : Collections.emptyList();

        List<LearnNotification> merged = new ArrayList<>(direct);
        merged.addAll(broadcast);
        merged.sort(Comparator.comparing(LearnNotification::getCreatedAt));

        List<Map<String, Object>> out = new ArrayList<>();
        for (LearnNotification n : merged) {
            out.add(toClientMap(n));
        }
        return out;
    }

    @Transactional
    public void setCurrentLesson(Long classId, Long lessonId, Long teacherId) {
        dashboardService.checkTeacherOwnsClass(teacherId, classId);
        SysClass clazz = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        clazz.setCurrentLessonId(lessonId);
        classRepository.save(clazz);
    }

    public Map<String, Object> getCurrentLessonForStudent(Long studentId) {
        SysUser student = userRepository.findById(studentId).orElse(null);
        if (student == null || student.getClassId() == null) {
            return Collections.singletonMap("lessonId", null);
        }
        SysClass clazz = classRepository.findById(student.getClassId()).orElse(null);
        if (clazz == null || clazz.getCurrentLessonId() == null) {
            return Collections.singletonMap("lessonId", null);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("lessonId", String.valueOf(clazz.getCurrentLessonId()));
        lessonRepository.findById(clazz.getCurrentLessonId()).ifPresent(lesson ->
                out.put("lessonTitle", lesson.getTitle()));
        return out;
    }

    private String buildMessage(InterveneRequest req) {
        if (StringUtils.hasText(req.getMessage())) {
            return req.getMessage();
        }
        if ("guide".equals(req.getType()) && StringUtils.hasText(req.getContent())) {
            return req.getContent();
        }
        if ("remind".equals(req.getType()) && req.getActivityIndex() != null) {
            return "请尽快完成活动" + req.getActivityIndex();
        }
        if ("broadcast".equals(req.getType())) {
            return "教师提醒：请抓紧完成当前任务";
        }
        return "教师发来一条学习提醒";
    }

    private Map<String, Object> toClientMap(LearnNotification n) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", n.getId());
        data.put("type", n.getMsgType());
        data.put("message", n.getMessage());
        data.put("classId", n.getClassId());
        data.put("targetStudentId", n.getTargetStudentId());
        data.put("createdAt", n.getCreatedAt());
        if (StringUtils.hasText(n.getPayloadJson())) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = objectMapper.readValue(n.getPayloadJson(), Map.class);
                data.putAll(payload);
            } catch (Exception ignored) {
            }
        }
        return data;
    }
}
