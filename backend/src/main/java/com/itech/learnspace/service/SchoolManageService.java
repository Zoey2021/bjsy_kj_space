package com.itech.learnspace.service;

import com.itech.learnspace.dto.ClassSaveRequest;
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
        if (classId != null) {
            list = userRepository.findByClassId(classId).stream()
                    .filter(u -> "STUDENT".equals(u.getRole()))
                    .collect(Collectors.toList());
        } else {
            list = userRepository.findByRole("STUDENT");
        }
        Map<Long, String> classNames = classRepository.findAll().stream()
                .collect(Collectors.toMap(SysClass::getId, SysClass::getName, (a, b) -> a));
        return list.stream()
                .sorted(Comparator.comparing(SysUser::getId))
                .map(u -> {
                    Map<String, Object> m = toUserMap(u);
                    m.put("className", u.getClassId() != null ? classNames.getOrDefault(u.getClassId(), "") : "");
                    return m;
                })
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
}
