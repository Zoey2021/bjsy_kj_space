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
import com.itech.learnspace.security.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchoolManageService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String FIRST_PASSWORD_CLASS = "2021级6班";
    private static final DateTimeFormatter PASSWORD_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_CLASS_SIZE = 50;

    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final EntityManager entityManager;
    private final ClassMembershipService classMembershipService;
    private final TeacherClassService teacherClassService;

    public SchoolManageService(SysUserRepository userRepository,
                               SysClassRepository classRepository,
                               PasswordEncoder passwordEncoder,
                               PasswordPolicy passwordPolicy,
                               EntityManager entityManager,
                               ClassMembershipService classMembershipService,
                               TeacherClassService teacherClassService) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.entityManager = entityManager;
        this.classMembershipService = classMembershipService;
        this.teacherClassService = teacherClassService;
    }

    // ---------- 教师 ----------

    public List<Map<String, Object>> listTeachers() {
        Map<Long, SysClass> classes = classRepository.findAll().stream()
                .collect(Collectors.toMap(SysClass::getId, c -> c, (a, b) -> a));
        return userRepository.findByRole("TEACHER").stream()
                .sorted(Comparator.comparing(SysUser::getId))
                .map(user -> {
                    Map<String, Object> row = toUserMap(user);
                    List<Long> classIds = teacherClassService.matchIds(user.getId());
                    List<String> labels = new ArrayList<String>();
                    for (Long classId : classIds) {
                        labels.add(teacherClassService.classLabel(classes.get(classId)));
                    }
                    row.put("classIds", classIds);
                    row.put("classLabels", labels);
                    return row;
                })
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
        String raw = auto ? generatePassword(10) : req.getPassword().trim();
        if (!auto) {
            passwordPolicy.validateNewPassword("TEACHER", raw);
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername().trim());
        assignPassword(user, raw, "TEACHER", auto);
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
            String raw = req.getPassword().trim();
            passwordPolicy.validateNewPassword("TEACHER", raw);
            assignPassword(user, raw, "TEACHER", false);
        }
        return toUserMap(userRepository.save(user));
    }

    @Transactional
    public void deleteTeacher(Long id) {
        SysUser user = requireTeacher(id);
        teacherClassService.deleteByTeacher(id);
        List<SysClass> classes = classRepository.findByTeacherId(id);
        if (!classes.isEmpty()) {
            throw new BusinessException("该教师名下仍有 " + classes.size() + " 个班级，请先更换班级任课教师后再删除");
        }
        userRepository.delete(user);
    }

    @Transactional
    public Map<String, Object> resetTeacherPassword(Long id) {
        SysUser user = requireTeacher(id);
        String raw = generatePassword(10);
        assignPassword(user, raw, "TEACHER", true);
        userRepository.save(user);
        Map<String, Object> out = toUserMap(user);
        out.put("initialPassword", raw);
        return out;
    }

    // ---------- 班级 ----------

    public List<Map<String, Object>> listClasses(SysUser viewer) {
        Set<Long> allowed = viewer == null ? null : teacherClassService.allowedClassIds(viewer.getId());
        if (viewer != null && teacherClassService.canAssign(viewer)) {
            allowed = null;
        }
        Map<Long, SysUser> teachers = userRepository.findByRole("TEACHER").stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        final Set<Long> visible = allowed;
        return classRepository.findAll().stream()
                .filter(c -> visible == null || visible.contains(c.getId()))
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
        teacherClassService.deleteByClass(id);
        classRepository.delete(clazz);
    }

    // ---------- 学生 ----------

    public List<Map<String, Object>> listStudents(Long classId) {
        return listStudents(null, classId, null);
    }

    @Transactional
    public Map<String, Object> saveTeacherClasses(SysUser operator, Long teacherId, List<Long> classIds) {
        teacherClassService.assertCanAssign(operator);
        teacherClassService.replaceMatches(teacherId, classIds);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("teacherId", teacherId);
        out.put("classIds", teacherClassService.matchIds(teacherId));
        return out;
    }

    public List<Map<String, Object>> listStudents(SysUser viewer, Long classId, String keyword) {
        Set<Long> allowed = viewer == null || teacherClassService.canAssign(viewer)
                ? null : teacherClassService.allowedClassIds(viewer.getId());
        if (classId != null && allowed != null && !allowed.contains(classId)) {
            throw new BusinessException(403, "只能查看已匹配的班级");
        }
        String q = keyword == null ? "" : keyword.trim();
        List<SysUser> list;
        if (StringUtils.hasText(q) && classId == null) {
            list = searchStudents(q);
        } else if (classId == null) {
            return Collections.emptyList();
        } else {
            list = loadStudentsOfClass(classId);
            if (StringUtils.hasText(q)) {
                String needle = q.toLowerCase();
                list = list.stream().filter(u -> studentMatches(u, needle)).collect(Collectors.toList());
            }
        }
        Map<Long, String> classNames = classRepository.findAll().stream()
                .collect(Collectors.toMap(SysClass::getId, SysClass::getName, (a, b) -> a));
        final Set<Long> visible = allowed;
        return list.stream()
                .filter(u -> visible == null || (u.getClassId() != null && visible.contains(u.getClassId())))
                .map(u -> {
                    Map<String, Object> m = toUserMap(u);
                    String className = u.getClassId() != null ? classNames.getOrDefault(u.getClassId(), "") : "";
                    m.put("className", className);
                    m.put("gradeLabel", gradeLabelFromClassName(className));
                    m.put("classLabel", classLabelFromClassName(className));
                    m.put("studentNo", PretestG4Service.parseStudentNo(u.getUsername()));
                    if (FIRST_PASSWORD_CLASS.equals(className == null ? "" : className.trim())) {
                        m.put("passwordNote", u.getPasswordNote() == null ? "" : u.getPasswordNote());
                        m.put("passwordChangedAt", u.getPasswordChangedAt() == null
                                ? "" : PASSWORD_TIME.format(u.getPasswordChangedAt()));
                    }
                    return m;
                })
                .sorted(Comparator
                        .comparing((Map<String, Object> m) -> String.valueOf(m.getOrDefault("username", "")))
                        .thenComparingInt((Map<String, Object> m) -> parseStudentNoOrder(m.get("studentNo")))
                        .thenComparing(m -> String.valueOf(m.getOrDefault("realName", ""))))
                .collect(Collectors.toList());
    }

    private List<SysUser> searchStudents(String keyword) {
        String needle = keyword.toLowerCase();
        return userRepository.findByRole("STUDENT").stream()
                .filter(u -> studentMatches(u, needle))
                .collect(Collectors.toList());
    }

    private boolean studentMatches(SysUser u, String keyword) {
        if (u == null || !StringUtils.hasText(keyword)) {
            return true;
        }
        String needle = keyword.toLowerCase();
        String name = u.getRealName() == null ? "" : u.getRealName();
        String username = u.getUsername() == null ? "" : u.getUsername().toLowerCase();
        String no = PretestG4Service.parseStudentNo(u.getUsername());
        return name.contains(keyword) || name.toLowerCase().contains(needle)
                || username.contains(needle)
                || (StringUtils.hasText(no) && (no.contains(keyword) || no.contains(needle)));
    }

    public List<SysUser> loadStudentsOfClass(Long classId) {
        LinkedHashSet<Long> ids = new LinkedHashSet<Long>();
        for (SysUser u : userRepository.findByClassId(classId)) {
            if ("STUDENT".equals(u.getRole())) {
                ids.add(u.getId());
            }
        }
        try {
            List<Number> extra = userRepository.findStudentIdsInClass(classId);
            if (extra != null) {
                for (Number n : extra) {
                    if (n != null) {
                        ids.add(n.longValue());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        List<SysUser> users = userRepository.findAllById(ids);
        for (SysUser u : users) {
            if (!"STUDENT".equals(u.getRole())) {
                continue;
            }
            if (u.getClassId() == null || !classId.equals(u.getClassId())) {
                u.setClassId(classId);
                userRepository.save(u);
            }
        }
        return users.stream()
                .filter(u -> "STUDENT".equals(u.getRole()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createStudent(StudentSaveRequest req) {
        requireText(req.getRealName(), "姓名不能为空");
        if (req.getClassId() == null) {
            throw new BusinessException("请选择班级");
        }
        SysClass clazz = classRepository.findById(req.getClassId())
                .orElseThrow(() -> new BusinessException("班级不存在"));
        String realName = req.getRealName().trim();
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        Integer studentNo = parseOptionalStudentNo(req.getStudentNo());
        if (username.matches("\\d{1,2}") && studentNo == null) {
            studentNo = Integer.parseInt(username);
            username = "";
        }
        if (!StringUtils.hasText(username)) {
            if (studentNo == null) {
                throw new BusinessException("请填写学号");
            }
            username = buildStudentUsername(realName, clazz.getName(), studentNo);
        }

        SysUser existing = findExistingStudent(req.getClassId(), username, studentNo, realName);
        if (existing != null) {
            existing.setRealName(realName);
            existing.setRole("STUDENT");
            existing.setStatus(1);
            existing.setClassId(req.getClassId());
            existing = userRepository.saveAndFlush(existing);
            classMembershipService.replace(existing.getId(), req.getClassId());
            Map<String, Object> out = toUserMap(existing);
            out.put("studentNo", studentNo != null ? String.valueOf(studentNo)
                    : PretestG4Service.parseStudentNo(existing.getUsername()));
            out.put("updatedExisting", true);
            return out;
        }

        ensureClassCapacity(req.getClassId(), 1);
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("账号已存在：" + username);
        }
        boolean auto = !StringUtils.hasText(req.getPassword());
        String raw = auto ? generatePassword(6) : req.getPassword().trim();
        if (!auto) {
            passwordPolicy.validateNewPassword("STUDENT", raw);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        assignPassword(user, raw, "STUDENT", auto);
        user.setRealName(realName);
        user.setRole("STUDENT");
        user.setClassId(req.getClassId());
        user.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        user = userRepository.saveAndFlush(user);
        try {
            classMembershipService.replace(user.getId(), user.getClassId());
        } catch (Exception e) {
            // 班级对照表失败时仍保留学生账号，避免整笔回滚后“提示成功但名单没有”
        }

        Map<String, Object> out = toUserMap(user);
        out.put("studentNo", PretestG4Service.parseStudentNo(user.getUsername()));
        if (auto) {
            out.put("initialPassword", raw);
        }
        return out;
    }

    private SysUser findExistingStudent(Long classId, String username, Integer studentNo, String realName) {
        if (StringUtils.hasText(username)) {
            SysUser byName = userRepository.findByUsername(username).orElse(null);
            if (byName != null && "STUDENT".equals(byName.getRole())) {
                return byName;
            }
        }
        if (studentNo != null) {
            String want = String.valueOf(studentNo);
            for (SysUser u : loadStudentsOfClass(classId)) {
                if (want.equals(PretestG4Service.parseStudentNo(u.getUsername()))) {
                    return u;
                }
            }
        }
        if (StringUtils.hasText(realName)) {
            for (SysUser u : userRepository.findByRole("STUDENT")) {
                if (!realName.equals(u.getRealName())) {
                    continue;
                }
                if (u.getClassId() == null || classId.equals(u.getClassId())) {
                    return u;
                }
            }
        }
        return null;
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
            String raw = req.getPassword().trim();
            passwordPolicy.validateNewPassword("STUDENT", raw);
            assignPassword(user, raw, "STUDENT", false);
        }
        user = userRepository.save(user);
        classMembershipService.replace(user.getId(), user.getClassId());
        return toUserMap(user);
    }

    @Transactional
    public void deleteStudent(Long id) {
        SysUser user = requireStudent(id);
        deleteStudentRelatedRows(id);
        userRepository.delete(user);
        userRepository.flush();
    }

    @Transactional
    public Map<String, Object> resetStudentPassword(Long id) {
        SysUser user = requireStudent(id);
        String raw = generatePassword(6);
        assignPassword(user, raw, "STUDENT", true);
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
            assignPassword(user, raw, "STUDENT", true);
            user.setRealName(realName);
            user.setRole("STUDENT");
            user.setClassId(classId);
            user.setStatus(1);
            user = userRepository.save(user);
            classMembershipService.replace(user.getId(), classId);
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

    private void deleteStudentRelatedRows(Long studentId) {
        deleteFromTableIfExists("learn_mall_order", "student_id", studentId);
        deleteFromTableIfExists("learn_pretest_g4", "student_id", studentId);
        deleteFromTableIfExists("learn_pretest_g6", "student_id", studentId);
        deleteFromTableIfExists("learn_hint_log", "student_id", studentId);
        deleteFromTableIfExists("learn_park_access", "student_id", studentId);
        deleteFromTableIfExists("learn_notification", "target_student_id", studentId);
        deleteFromTableIfExists("learn_submission_log", "student_id", studentId);
        deleteFromTableIfExists("learn_submission", "student_id", studentId);
        deleteFromTableIfExists("learn_progress", "student_id", studentId);
        deleteFromTableIfExists("learn_visit_log", "student_id", studentId);
        deleteFromTableIfExists("learn_points", "student_id", studentId);
        deleteFromTableIfExists("student_grade_report", "student_id", studentId);
        deleteFromTableIfExists("student_school_grade_record", "student_id", studentId);
        deleteFromTableIfExists("sys_class_student", "student_id", studentId);
    }

    private void deleteFromTableIfExists(String table, String column, Long studentId) {
        if (studentId == null || !tableExists(table)) {
            return;
        }
        entityManager.createNativeQuery("DELETE FROM " + table + " WHERE " + column + " = ?1")
                .setParameter(1, studentId)
                .executeUpdate();
    }

    private boolean tableExists(String table) {
        Number count = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?1")
                .setParameter(1, table)
                .getSingleResult();
        return count != null && count.intValue() > 0;
    }

    /**
     * 把本班学生账号整理为「年+班+学号+拼音」新格式，便于排序。
     * 只改 username，不改密码、积分、学习记录。
     */
    @Transactional
    public Map<String, Object> normalizeClassStudentUsernames(Long classId) {
        if (classId == null) {
            throw new BusinessException("请选择班级");
        }
        SysClass clazz = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        String className = clazz.getName() == null ? "" : clazz.getName();
        List<SysUser> students = loadStudentsOfClass(classId);
        int changed = 0;
        int skipped = 0;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (SysUser u : students) {
            String oldName = u.getUsername() == null ? "" : u.getUsername();
            String noRaw = PretestG4Service.parseStudentNo(oldName);
            int studentNo;
            try {
                studentNo = Integer.parseInt(noRaw);
            } catch (Exception e) {
                skipped++;
                continue;
            }
            String next = rebuildStudentUsername(oldName, className, studentNo);
            if (next.equals(oldName)) {
                skipped++;
                continue;
            }
            SysUser clash = userRepository.findByUsername(next).orElse(null);
            if (clash != null && !clash.getId().equals(u.getId())) {
                skipped++;
                Map<String, Object> fail = new LinkedHashMap<String, Object>();
                fail.put("oldUsername", oldName);
                fail.put("newUsername", next);
                fail.put("reason", "目标账号已被占用");
                items.add(fail);
                continue;
            }
            u.setUsername(next);
            userRepository.save(u);
            changed++;
            Map<String, Object> ok = new LinkedHashMap<String, Object>();
            ok.put("oldUsername", oldName);
            ok.put("newUsername", next);
            ok.put("realName", u.getRealName());
            items.add(ok);
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("classId", classId);
        out.put("className", className);
        out.put("changedCount", Integer.valueOf(changed));
        out.put("skippedCount", Integer.valueOf(skipped));
        out.put("items", items);
        return out;
    }

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

    private void clearChosenPassword(SysUser user) {
        user.setPasswordNote(null);
        user.setPasswordChangedAt(null);
    }

    /** temporary=true：系统生成的临时口令，清空改密记录；false：人工设定，记为已改密。 */
    private void assignPassword(SysUser user, String raw, String role, boolean temporary) {
        if (!temporary) {
            passwordPolicy.validateNewPassword(role, raw);
        } else if (passwordPolicy.isWeakPlaintext(raw)) {
            throw new BusinessException("临时密码不合格，请重试");
        }
        user.setPassword(passwordEncoder.encode(raw));
        if (temporary) {
            clearChosenPassword(user);
        } else {
            user.setPasswordChangedAt(LocalDateTime.now());
            user.setPasswordNote(null);
        }
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
        String derivedGrade = gradeLabelFromClassName(c.getName());
        m.put("gradeName", StringUtils.hasText(derivedGrade) ? derivedGrade : c.getGradeName());
        m.put("gradeLabel", derivedGrade);
        m.put("classLabel", classLabelFromClassName(c.getName()));
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

    private static Integer parseOptionalStudentNo(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            int n = Integer.parseInt(raw.trim().replaceFirst("^0+(?=\\d)", ""));
            if (n < 1 || n > MAX_CLASS_SIZE) {
                throw new BusinessException("学号须在 1～" + MAX_CLASS_SIZE + " 之间");
            }
            return n;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("学号必须是数字");
        }
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

    /**
     * 学生账号：入学年 + 班号 + 学号 + 姓名拼音字母，便于按账号排序。
     * 例：2021级1班、学号1、baiyuxuan → 20210101baiyuxuan
     * 兼容旧账号（拼音在前）的解析见 PretestG4Service.parseStudentNo。
     */
    static String buildStudentUsername(String realName, String className, int studentNo) {
        String letters = realName == null ? "" : realName.replaceAll("[^A-Za-z]", "").toLowerCase();
        if (!StringUtils.hasText(letters)) {
            letters = "s";
        }
        String[] parts = parseClassNameParts(className);
        String year = parts[0];
        String classNo = parts[1];
        return year + classNo + String.format("%02d", studentNo) + letters;
    }

    /** 从旧账号中抽出拼音字母，再按新规则重排：年+班+学号+拼音 */
    static String rebuildStudentUsername(String oldUsername, String className, int studentNo) {
        String letters = oldUsername == null ? "" : oldUsername.replaceAll("[^A-Za-z]", "").toLowerCase();
        if (!StringUtils.hasText(letters)) {
            letters = "s";
        }
        String[] parts = parseClassNameParts(className);
        return parts[0] + parts[1] + String.format("%02d", studentNo) + letters;
    }

    /** @return [year, classNo2] 如 ["2021","01"] */
    static String[] parseClassNameParts(String className) {
        String year = "0000";
        String classNo = "00";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20\\d{2})级0*(\\d+)班")
                .matcher(className == null ? "" : className);
        if (m.find()) {
            year = m.group(1);
            classNo = String.format("%02d", Integer.parseInt(m.group(2)));
        }
        return new String[] { year, classNo };
    }

    /** 2021级 → 六年级（按当前届别） */
    static String gradeLabelFromClassName(String className) {
        String[] parts = parseClassNameParts(className);
        String year = parts[0];
        if ("2021".equals(year)) {
            return "六年级";
        }
        if ("2022".equals(year)) {
            return "五年级";
        }
        if ("2023".equals(year)) {
            return "四年级";
        }
        if ("2024".equals(year)) {
            return "三年级";
        }
        if ("0000".equals(year)) {
            return "";
        }
        return year + "级";
    }

    static String classLabelFromClassName(String className) {
        String[] parts = parseClassNameParts(className);
        if ("00".equals(parts[1]) && (className == null || !className.contains("班"))) {
            return className == null ? "" : className;
        }
        return Integer.parseInt(parts[1]) + "班";
    }
}
