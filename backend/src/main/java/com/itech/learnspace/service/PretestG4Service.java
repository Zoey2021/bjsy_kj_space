package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itech.learnspace.entity.LearnPretestG4;
import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.LearnPretestG4Repository;
import com.itech.learnspace.repository.SysClassRepository;
import com.itech.learnspace.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PretestG4Service {

    private static final String[] FILL_KEY = {
            "双击", "退格", "回车", "加粗", "幻灯片", "文件", "数据", "数据", "条形", "编码"
    };
    private static final String[] CHOICE_KEY = {"B", "B", "A", "B", "A", "A", "A", "A", "A", "A"};
    private static final Pattern USERNAME_NO = Pattern.compile("20\\d{2}(\\d{2})(\\d{2})$");

    private final LearnPretestG4Repository pretestRepository;
    private final SysClassRepository classRepository;
    private final SysUserRepository userRepository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    public PretestG4Service(LearnPretestG4Repository pretestRepository,
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

    public static String parseStudentNo(String username) {
        if (username == null) {
            return "";
        }
        Matcher m = USERNAME_NO.matcher(username);
        if (m.find()) {
            return String.valueOf(Integer.parseInt(m.group(2)));
        }
        return "";
    }

    @Transactional
    public Map<String, Object> submit(SysUser student, Map<String, Object> body) {
        if (student.getClassId() == null) {
            throw new BusinessException("当前账号未绑定班级，无法提交前测");
        }
        String className = classRepository.findById(student.getClassId())
                .map(SysClass::getName)
                .orElse("");
        String studentNo = parseStudentNo(student.getUsername());

        JsonNode node = objectMapper.valueToTree(body == null ? new HashMap<String, Object>() : body);
        int fillScore = scoreFill(node.path("fill"));
        int choiceRight = countChoiceRight(node.path("choice"));
        int choiceScore = choiceRight * 3;
        int opScore = node.path("op").path("opTotal").asInt(0);
        if (opScore < 0) {
            opScore = 0;
        }
        if (opScore > 40) {
            opScore = 40;
        }

        ObjectNode stored = (ObjectNode) node;
        stored.put("name", student.getRealName());
        stored.put("className", className);
        stored.put("no", studentNo);
        stored.put("studentId", student.getId());
        stored.put("fillScore", fillScore);
        stored.put("choiceRight", choiceRight);
        stored.put("choiceScore", choiceScore);
        stored.put("opScore", opScore);

        LearnPretestG4 row = pretestRepository.findByStudentId(student.getId()).orElseGet(LearnPretestG4::new);
        row.setStudentId(student.getId());
        row.setClassId(student.getClassId());
        row.setStudentName(student.getRealName());
        row.setClassName(className);
        row.setStudentNo(studentNo);
        row.setFillScore(fillScore);
        row.setChoiceRight(choiceRight);
        row.setChoiceScore(choiceScore);
        row.setOpScore(opScore);
        row.setTotalScore(fillScore + choiceScore + opScore);
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
        List<LearnPretestG4> rows = pretestRepository.findByClassIdOrderBySubmittedAtDesc(classId);
        Map<Long, LearnPretestG4> byStudent = new HashMap<Long, LearnPretestG4>();
        for (LearnPretestG4 row : rows) {
            byStudent.put(row.getStudentId(), row);
        }

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        int submitted = 0;
        int opFull = 0;
        for (SysUser s : students) {
            LearnPretestG4 row = byStudent.get(s.getId());
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("studentId", s.getId());
            item.put("realName", s.getRealName());
            item.put("username", s.getUsername());
            item.put("studentNo", parseStudentNo(s.getUsername()));
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
                item.put("totalScore", row.getTotalScore());
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

    private Map<String, Object> toStudentView(LearnPretestG4 row) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("submitted", true);
        m.put("studentName", row.getStudentName());
        m.put("className", row.getClassName());
        m.put("studentNo", row.getStudentNo());
        m.put("fillScore", row.getFillScore());
        m.put("choiceRight", row.getChoiceRight());
        m.put("choiceScore", row.getChoiceScore());
        m.put("opScore", row.getOpScore());
        m.put("totalScore", row.getTotalScore());
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

    private int scoreFill(JsonNode fill) {
        if (fill == null || !fill.isArray()) {
            return 0;
        }
        int score = 0;
        for (int i = 0; i < FILL_KEY.length && i < fill.size(); i++) {
            String ans = fill.get(i).asText("").replaceAll("\\s+", "");
            if (ans.contains(FILL_KEY[i])) {
                score += 2;
            }
        }
        return score;
    }

    private int countChoiceRight(JsonNode choice) {
        if (choice == null || !choice.isArray()) {
            return 0;
        }
        int right = 0;
        for (int i = 0; i < CHOICE_KEY.length && i < choice.size(); i++) {
            if (CHOICE_KEY[i].equalsIgnoreCase(choice.get(i).asText("").trim())) {
                right++;
            }
        }
        return right;
    }
}
