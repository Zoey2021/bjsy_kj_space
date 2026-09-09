package com.itech.learnspace.service;

import com.itech.learnspace.dto.ClassSaveRequest;
import com.itech.learnspace.dto.StudentBatchRequest;
import com.itech.learnspace.dto.StudentSaveRequest;
import com.itech.learnspace.dto.TeacherSaveRequest;
import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.SysClassRepository;
import com.itech.learnspace.repository.SysUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchoolManageService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_CLASS_SIZE = 50;

    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final PasswordEncoder passwordEncoder;

    public SchoolManageService(SysUserRepository userRepository,
                               SysClassRepository classRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- 教师 ----------

    public List<Map<String, Object>> listTeachers() {
        return userRepository.findByRole("TEACHER").stream()
                .sorted(Comparator.comparing(SysUser::getId))
                .map(this::toUserMap)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createTeacher(TeacherSaveRequest req) {
        requireText(req.getUsername(), "账号不能为空");
        requireText(req.getRealName(), "姓名不能为空");
        if (userRepository.findByUsername(req.getUsername().trim()).isPresent()) {
            throw new BusinessException("账号已存在");
        }
        boolean auto = !StringUtils.hasText(req.getPassword());
        String raw = auto ? generatePassword(6) : req.getPassword().trim();

        SysUser user = new SysUser();
        user.setUsername(req.getUsername().trim());
        user.setPassword(passwordEncoder.encode(raw));
        user.setRealName(req.getRealName().trim());
        user.setRole("TEACHER");
        user.setClassId(null);
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        user = userRepository.save(user);

        Map<String, Object> out = toUserMap(user);
        if (auto) {
            out.put("initialPassword", raw);
        }
        return out;
    }

    @Transactional
    public Map<String, Object> updateTeacher(Long id, TeacherSaveRequest req) {
        SysUser user = requireTeacher(id);
        if (StringUtils.hasText(req.getRealName())) {
            user.setRealName(req.getRealName().trim());
        }
        if (StringUtils.hasText(req.getUsername())) {
            String username = req.getUsername().trim();
            userRepository.findByUsername(username).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new BusinessException("账号已存在");
                }
            });
            user.setUsername(username);
        }
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        if (StringUtils.hasText(req.getPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        }
        return toUserMap(userRepository.save(user));
    }

    @Transactional
    public void deleteTeacher(Long id) {
        SysUser user = requireTeacher(id);
        List<SysClass> classes = classRepository.findByTeacherId(id);
        if (!classes.isEmpty()) {
            throw new BusinessException("该教师名下仍有 " + classes.size() + " 个班级，请先更换班级任课教师后再删除");
        }
        userRepository.delete(user);
    }

    @Transactional
    public Map<String, Object> resetTeacherPassword(Long id) {
        SysUser user = requireTeacher(id);
        String raw = generatePassword(6);
        user.setPassword(passwordEncoder.encode(raw));
        userRepository.save(user);
        Map<String, Object> out = toUserMap(user);
        out.put("initialPassword", raw);
        return out;
    }

    // ---------- 班级 ----------

    public List<Map<String, Object>> listClasses() {
        Map<Long, SysUser> teachers = userRepository.findByRole("TEACHER").stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        return classRepository.findAll().stream()
                .sorted(Comparator.comparing(SysClass::getId))
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getName());
                    m.put("gradeName", c.getGradeName());
                    m.put("teacherId", c.getTeacherId());
                    SysUser t = teachers.get(c.getTeacherId());
                    m.put("teacherName", t != null ? t.getRealName() : ("教师#" + c.getTeacherId()));
                    long studentCount = userRepository.findByClassIdAndRoleAndStatus(c.getId(), "STUDENT", 1).size();
                    // 也统计禁用学生，便于管理
                    long allStudents = userRepository.findByClassId(c.getId()).stream()
                            .filter(u -> "STUDENT".equals(u.getRole())).count();
                    m.put("studentCount", allStudents);
                    m.put("activeStudentCount", studentCount);
                    m.put("loginCode", c.getLoginCode());
                    m.put("createdAt", c.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createClass(ClassSaveRequest req, SysUser operator) {
        requireText(req.getName(), "班级名称不能为空");
        requireText(req.getGradeName(), "年级名称不能为空");
        Long teacherId = req.getTeacherId();
        if (teacherId == null) {
            if ("TEACHER".equals(operator.getRole())) {
                teacherId = operator.getId();
            } else {
                throw new BusinessException("请选择任课教师");
            }
        }
        requireTeacher(teacherId);

        SysClass clazz = new SysClass();
        clazz.setName(req.getName().trim());
        clazz.setGradeName(req.getGradeName().trim());
        clazz.setTeacherId(teacherId);
        clazz = classRepository.save(clazz);
        return classToMap(clazz);
    }

    @Transactional
    public Map<String, Object> updateClass(Long id, ClassSaveRequest req) {
        SysClass clazz = classRepository.findById(id)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        if (StringUtils.hasText(req.getName())) {
            clazz.setName(req.getName().trim());
        }
        if (StringUtils.hasText(req.getGradeName())) {
            clazz.setGradeName(req.getGradeName().trim());
        }
        if (req.getTeacherId() != null) {
            requireTeacher(req.getTeacherId());
            clazz.setTeacherId(req.getTeacherId());
        }
        return classToMap(classRepository.save(clazz));
    }

    @Transactional
    public void deleteClass(Long id) {
        SysClass clazz = classRepository.findById(id)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        long students = userRepository.findByClassId(id).stream()
                .filter(u -> "STUDENT".equals(u.getRole())).count();
        if (students > 0) {
            throw new BusinessException("该班级仍有 " + students + " 名学生，请先转移或删除学生后再删班级");
        }
        classRepository.delete(clazz);
    }

    // ---------- 学生 ----------

    public List<Map<String, Object>> listStudents(Long classId) {
        List<SysUser> list;
        if (classId == null) {
            return Collections.emptyList();
        }
        list = userRepository.findByClassId(classId).stream()
                .filter(u -> "STUDENT".equals(u.getRole()))
                .collect(Collectors.toList());
        Map<Long, String> classNames = classRepository.findAll().stream()
                .collect(Collectors.toMap(SysClass::getId, SysClass::getName, (a, b) -> a));
        return list.stream()
                .map(u -> {
                    Map<String, Object> m = toUserMap(u);
                    m.put("className", u.getClassId() != null ? classNames.getOrDefault(u.getClassId(), "") : "");
                    m.put("studentNo", PretestG4Service.parseStudentNo(u.getUsername()));
                    return m;
                })
                .sorted(Comparator
                        .comparingInt((Map<String, Object> m) -> parseStudentNoOrder(m.get("studentNo")))
                        .thenComparing(m -> String.valueOf(m.getOrDefault("realName", ""))))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createStudent(StudentSaveRequest req) {
        requireText(req.getUsername(), "账号不能为空");
        requireText(req.getRealName(), "姓名不能为空");
        if (req.getClassId() == null) {
            throw new BusinessException("请选择班级");
        }
        classRepository.findById(req.getClassId())
                .orElseThrow(() -> new BusinessException("班级不存在"));
        ensureClassCapacity(req.getClassId(), 1);
        if (userRepository.findByUsername(req.getUsername().trim()).isPresent()) {
            throw new BusinessException("账号已存在");
        }
        boolean auto = !StringUtils.hasText(req.getPassword());
        String raw = auto ? generatePassword(6) : req.getPassword().trim();

        SysUser user = new SysUser();
        user.setUsername(req.getUsername().trim());
        user.setPassword(passwordEncoder.encode(raw));
        user.setRealName(req.getRealName().trim());
        user.setRole("STUDENT");
        user.setClassId(req.getClassId());
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        user = userRepository.save(user);

        Map<String, Object> out = toUserMap(user);
        if (auto) {
            out.put("initialPassword", raw);
        }
        return out;
    }

    @Transactional
    public Map<String, Object> updateStudent(Long id, StudentSaveRequest req) {
        SysUser user = requireStudent(id);
        if (StringUtils.hasText(req.getRealName())) {
            user.setRealName(req.getRealName().trim());
        }
        if (StringUtils.hasText(req.getUsername())) {
            String username = req.getUsername().trim();
            userRepository.findByUsername(username).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new BusinessException("账号已存在");
                }
            });
            user.setUsername(username);
        }
        if (req.getClassId() != null) {
            classRepository.findById(req.getClassId())
                    .orElseThrow(() -> new BusinessException("班级不存在"));
            if (!req.getClassId().equals(user.getClassId())) {
                ensureClassCapacity(req.getClassId(), 1);
            }
            user.setClassId(req.getClassId());
        }
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        if (StringUtils.hasText(req.getPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        }
        return toUserMap(userRepository.save(user));
    }

    @Transactional
    public void deleteStudent(Long id) {
        SysUser user = requireStudent(id);
        userRepository.delete(user);
    }

    @Transactional
    public Map<String, Object> resetStudentPassword(Long id) {
        SysUser user = requireStudent(id);
        String raw = generatePassword(6);
        user.setPassword(passwordEncoder.encode(raw));
        userRepository.save(user);
        Map<String, Object> out = toUserMap(user);
        out.put("initialPassword", raw);
        return out;
    }

    @Transactional
    public Map<String, Object> batchCreateStudents(StudentBatchRequest request) {
        List<StudentBatchRequest.Row> rows = request == null || request.getRows() == null
                ? Collections.emptyList() : request.getRows();
        if (rows.isEmpty()) {
            throw new BusinessException("请先粘贴或上传学生名单");
        }
        if (rows.size() > 200) {
            throw new BusinessException("单次最多导入 200 人");
        }

        Map<String, SysClass> classByKey = new HashMap<String, SysClass>();
        for (SysClass c : classRepository.findAll()) {
            classByKey.put(normalizeClassKey(c.getName()), c);
            if (StringUtils.hasText(c.getGradeName())) {
                classByKey.put(normalizeClassKey(c.getGradeName() + c.getName()), c);
            }
        }

        Map<Long, Integer> existingCount = new HashMap<Long, Integer>();
        Map<Long, Set<String>> existingNos = new HashMap<Long, Set<String>>();
        Map<Long, Integer> incomingCount = new HashMap<Long, Integer>();

        List<Map<String, Object>> created = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> failed = new ArrayList<Map<String, Object>>();

        int line = 1;
        for (StudentBatchRequest.Row row : rows) {
            line++;
            String className = row.getClassName() == null ? "" : row.getClassName().trim();
            String studentNoRaw = row.getStudentNo() == null ? "" : row.getStudentNo().trim();
            String realName = row.getRealName() == null ? "" : row.getRealName().trim();
            if (!StringUtils.hasText(className) && !StringUtils.hasText(studentNoRaw) && !StringUtils.hasText(realName)) {
                continue;
            }
            if (!StringUtils.hasText(className) || !StringUtils.hasText(studentNoRaw) || !StringUtils.hasText(realName)) {
                failed.add(failRow(line, className, studentNoRaw, realName, "班级、学号、姓名都要填写"));
                continue;
            }
            SysClass clazz = classByKey.get(normalizeClassKey(className));
            if (clazz == null) {
                failed.add(failRow(line, className, studentNoRaw, realName, "找不到班级「" + className + "」，请与班级管理中的名称一致"));
                continue;
            }
            Integer no;
            try {
                String digits = studentNoRaw.replaceFirst("^0+(?=\\d)", "");
                no = Integer.parseInt(digits);
            } catch (Exception e) {
                failed.add(failRow(line, className, studentNoRaw, realName, "学号必须是数字"));
                continue;
            }
            if (no < 1 || no > MAX_CLASS_SIZE) {
                failed.add(failRow(line, className, studentNoRaw, realName, "学号须在 1～" + MAX_CLASS_SIZE + " 之间"));
                continue;
            }
            String studentNo = String.valueOf(no);
            Long classId = clazz.getId();
            if (!existingCount.containsKey(classId)) {
                List<SysUser> members = userRepository.findByClassId(classId).stream()
                        .filter(u -> "STUDENT".equals(u.getRole()))
                        .collect(Collectors.toList());
                existingCount.put(classId, members.size());
                Set<String> nos = new HashSet<String>();
                for (SysUser u : members) {
                    String parsed = PretestG4Service.parseStudentNo(u.getUsername());
                    if (StringUtils.hasText(parsed)) {
                        nos.add(parsed);
                    }
                }
                existingNos.put(classId, nos);
            }
            if (existingNos.get(classId).contains(studentNo)) {
                failed.add(failRow(line, className, studentNo, realName, "该班已有学号 " + studentNo));
                continue;
            }
            int nextCount = existingCount.get(classId) + incomingCount.getOrDefault(classId, 0) + 1;
            if (nextCount > MAX_CLASS_SIZE) {
                failed.add(failRow(line, className, studentNo, realName, "班级人数不能超过 " + MAX_CLASS_SIZE + " 人"));
                continue;
            }
            String username = buildStudentUsername(realName, clazz.getName(), no);
            if (userRepository.findByUsername(username).isPresent()) {
                failed.add(failRow(line, className, studentNo, realName, "账号已存在：" + username));
                continue;
            }
            String raw = generatePassword(6);
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(raw));
            user.setRealName(realName);
            user.setRole("STUDENT");
            user.setClassId(classId);
            user.setStatus(1);
            user = userRepository.save(user);
            existingNos.get(classId).add(studentNo);
            incomingCount.put(classId, incomingCount.getOrDefault(classId, 0) + 1);

            Map<String, Object> out = toUserMap(user);
            out.put("className", clazz.getName());
            out.put("studentNo", studentNo);
            out.put("initialPassword", raw);
            created.add(out);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("createdCount", created.size());
        result.put("failedCount", failed.size());
        result.put("created", created);
        result.put("failed", failed);
        return result;
    }

    // ---------- helpers ----------

    private SysUser requireTeacher(Long id) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("教师不存在"));
        if (!"TEACHER".equals(user.getRole())) {
            throw new BusinessException("该账号不是教师");
        }
        return user;
    }

    private SysUser requireStudent(Long id) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学生不存在"));
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException("该账号不是学生");
        }
        return user;
    }

    private Map<String, Object> toUserMap(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("role", u.getRole());
        m.put("classId", u.getClassId());
        m.put("status", u.getStatus());
        m.put("createdAt", u.getCreatedAt());
        m.put("updatedAt", u.getUpdatedAt());
        return m;
    }

    private Map<String, Object> classToMap(SysClass c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("gradeName", c.getGradeName());
        m.put("teacherId", c.getTeacherId());
        userRepository.findById(c.getTeacherId()).ifPresent(t -> m.put("teacherName", t.getRealName()));
        long allStudents = userRepository.findByClassId(c.getId()).stream()
                .filter(u -> "STUDENT".equals(u.getRole())).count();
        m.put("studentCount", allStudents);
        m.put("loginCode", c.getLoginCode());
        m.put("createdAt", c.getCreatedAt());
        return m;
    }

    private void requireText(String v, String msg) {
        if (!StringUtils.hasText(v)) {
            throw new BusinessException(msg);
        }
    }

    private String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private Map<String, Object> failRow(int line, String className, String studentNo, String realName, String reason) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("line", line);
        m.put("className", className);
        m.put("studentNo", studentNo);
        m.put("realName", realName);
        m.put("reason", reason);
        return m;
    }

    private static int parseStudentNoOrder(Object raw) {
        if (raw == null) {
            return Integer.MAX_VALUE;
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(s.replaceFirst("^0+(?=\\d)", ""));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private void ensureClassCapacity(Long classId, int adding) {
        long count = userRepository.findByClassId(classId).stream()
                .filter(u -> "STUDENT".equals(u.getRole()))
                .count();
        if (count + adding > MAX_CLASS_SIZE) {
            throw new BusinessException("班级人数不能超过 " + MAX_CLASS_SIZE + " 人");
        }
    }

    static String normalizeClassKey(String name) {
        if (name == null) {
            return "";
        }
        String s = name.replaceAll("\\s+", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20\\d{2})级0*(\\d+)班").matcher(s);
        if (m.find()) {
            return m.group(1) + "级" + Integer.parseInt(m.group(2)) + "班";
        }
        return s;
    }

    static String buildStudentUsername(String realName, String className, int studentNo) {
        String letters = realName == null ? "" : realName.replaceAll("[^A-Za-z]", "").toLowerCase();
        if (!StringUtils.hasText(letters)) {
            letters = "s";
        }
        String year = "0000";
        String classNo = "00";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20\\d{2})级0*(\\d+)班")
                .matcher(className == null ? "" : className);
        if (m.find()) {
            year = m.group(1);
            classNo = String.format("%02d", Integer.parseInt(m.group(2)));
        }
        return letters + year + classNo + String.format("%02d", studentNo);
    }
}
