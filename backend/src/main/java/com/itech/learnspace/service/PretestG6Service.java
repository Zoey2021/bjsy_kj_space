package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itech.learnspace.entity.LearnPretestG6;
import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.LearnPretestG6Repository;
import com.itech.learnspace.repository.SysClassRepository;
import com.itech.learnspace.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PretestG6Service {

    private final LearnPretestG6Repository pretestRepository;
    private final SysClassRepository classRepository;
    private final SysUserRepository userRepository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    public PretestG6Service(LearnPretestG6Repository pretestRepository,
                            SysClassRepository classRepository,
                            SysUserRepository userRepository,
                            DashboardService dashboardService,
                            ObjectMapper objectMapper) {
        this.pretestRepository = pretestRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> submit(SysUser student, Map<String, Object> body) {
        if (student.getClassId() == null) {
            throw new BusinessException("当前账号未绑定班级，无法提交前测");
        }
        String className = classRepository.findById(student.getClassId())
                .map(SysClass::getName)
                .orElse("");
        String studentNo = PretestG4Service.parseStudentNo(student.getUsername());

        JsonNode node = objectMapper.valueToTree(body == null ? new HashMap<String, Object>() : body);
        JsonNode scores = node.path("scores");
        int fillScore = clamp(scores.path("fill").asInt(0), 0, 20);
        int choiceScore = clamp(scores.path("choice").asInt(0), 0, 30);
        int choiceRight = clamp(scores.path("choiceRight").asInt(choiceScore / 3), 0, 10);
        int opScore = clamp(scores.path("op").asInt(0), 0, 40);
        int attScore = clamp(scores.path("att").asInt(0), 0, 10);
        int total = clamp(scores.path("total").asInt(fillScore + choiceScore + opScore + attScore), 0, 100);
        String level = normalizeLevel(scores.path("level").asText(""));

        ObjectNode stored = (ObjectNode) node;
        stored.put("name", student.getRealName());
        stored.put("className", className);
        stored.put("no", studentNo);
        stored.put("studentId", student.getId());

        LearnPretestG6 row = pretestRepository.findByStudentId(student.getId()).orElseGet(LearnPretestG6::new);
        row.setStudentId(student.getId());
        row.setClassId(student.getClassId());
        row.setStudentName(student.getRealName());
        row.setClassName(className);
        row.setStudentNo(studentNo);
        row.setFillScore(fillScore);
        row.setChoiceRight(choiceRight);
        row.setChoiceScore(choiceScore);
        row.setOpScore(opScore);
        row.setAttScore(attScore);
        row.setTotalScore(total);
        row.setLevelCode(level);
        try {
            row.setContentJson(objectMapper.writeValueAsString(stored));
        } catch (Exception e) {
            throw new BusinessException("作答数据无法保存");
        }
        pretestRepository.save(row);
        return toStudentView(row);
    }

    public Map<String, Object> mySubmission(Long studentId) {
        return pretestRepository.findByStudentId(studentId)
                .map(this::toStudentView)
                .orElse(null);
    }

    public Map<String, Object> classOverview(Long teacherId, String role, Long classId) {
        if (!"ADMIN".equals(role)) {
            dashboardService.checkTeacherOwnsClass(teacherId, classId);
        }
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        List<SysUser> students = userRepository.findByClassIdAndRoleAndStatus(classId, "STUDENT", 1);
        List<LearnPretestG6> rows = pretestRepository.findByClassIdOrderBySubmittedAtDesc(classId);
        Map<Long, LearnPretestG6> byStudent = new HashMap<Long, LearnPretestG6>();
        for (LearnPretestG6 row : rows) {
            byStudent.put(row.getStudentId(), row);
        }

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        int submitted = 0;
        int opFull = 0;
        for (SysUser s : students) {
            LearnPretestG6 row = byStudent.get(s.getId());
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("studentId", s.getId());
            item.put("realName", s.getRealName());
            item.put("username", s.getUsername());
            item.put("studentNo", PretestG4Service.parseStudentNo(s.getUsername()));
            item.put("className", cls.getName());
            if (row != null) {
                submitted++;
                if (row.getOpScore() != null && row.getOpScore() == 40) {
                    opFull++;
                }
                item.put("submitted", true);
                item.put("fillScore", row.getFillScore());
                item.put("choiceRight", row.getChoiceRight());
                item.put("choiceScore", row.getChoiceScore());
                item.put("opScore", row.getOpScore());
                item.put("attScore", row.getAttScore());
                item.put("totalScore", row.getTotalScore());
                item.put("level", row.getLevelCode());
                item.put("submittedAt", row.getSubmittedAt());
                item.put("detail", parseContent(row.getContentJson()));
            } else {
                item.put("submitted", false);
            }
            list.add(item);
        }
        list.sort((a, b) -> {
            int sa = Boolean.TRUE.equals(a.get("submitted")) ? 0 : 1;
            int sb = Boolean.TRUE.equals(b.get("submitted")) ? 0 : 1;
            if (sa != sb) {
                return sa - sb;
            }
            return String.valueOf(a.get("realName")).compareTo(String.valueOf(b.get("realName")));
        });

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("classId", classId);
        result.put("className", cls.getName());
        result.put("totalStudents", students.size());
        result.put("submittedCount", submitted);
        result.put("opFullCount", opFull);
        result.put("students", list);
        return result;
    }

    @Transactional
    public int clearClass(Long teacherId, String role, Long classId) {
        if (!"ADMIN".equals(role)) {
            dashboardService.checkTeacherOwnsClass(teacherId, classId);
        }
        classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        return (int) pretestRepository.deleteByClassId(classId);
    }

    private Map<String, Object> toStudentView(LearnPretestG6 row) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("submitted", true);
        m.put("studentName", row.getStudentName());
        m.put("className", row.getClassName());
        m.put("studentNo", row.getStudentNo());
        m.put("fillScore", row.getFillScore());
        m.put("choiceRight", row.getChoiceRight());
        m.put("choiceScore", row.getChoiceScore());
        m.put("opScore", row.getOpScore());
        m.put("attScore", row.getAttScore());
        m.put("totalScore", row.getTotalScore());
        m.put("level", row.getLevelCode());
        m.put("submittedAt", row.getSubmittedAt());
        return m;
    }

    private Object parseContent(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    private static int clamp(int n, int min, int max) {
        return Math.max(min, Math.min(max, n));
    }

    private static String normalizeLevel(String raw) {
        if ("A".equalsIgnoreCase(raw) || "B".equalsIgnoreCase(raw) || "C".equalsIgnoreCase(raw)) {
            return raw.toUpperCase();
        }
        return "B";
    }
}
