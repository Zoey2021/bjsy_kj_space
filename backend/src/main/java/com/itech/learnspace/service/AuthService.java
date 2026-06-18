package com.itech.learnspace.service;

import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.SysClassRepository;
import com.itech.learnspace.repository.SysUserRepository;
import com.itech.learnspace.security.LoginUser;
import com.itech.learnspace.util.JwtUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final int CLASS_CODE_TTL_HOURS = 8;
    private final Random random = new Random();

    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(SysUserRepository userRepository,
                       SysClassRepository classRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 登录：校验账号密码 → 生成 JWT → 返回用户信息和角色
     * 前端根据 role 字段自动跳转到学生/教师/管理员页面
     */
    public Map<String, Object> login(String username, String password) {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("账号不存在"));

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        return buildLoginResult(user);
    }

    /** 教师：获取或刷新班级登录码 */
    public Map<String, Object> getOrGenerateClassLoginCode(Long classId, Long teacherId) {
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        if (!cls.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "无权操作该班级");
        }
        if (cls.getLoginCode() == null || isCodeExpired(cls)) {
            return generateClassLoginCode(cls);
        }
        return toCodeResponse(cls);
    }

    /** 教师：强制刷新班级登录码 */
    public Map<String, Object> refreshClassLoginCode(Long classId, Long teacherId) {
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        if (!cls.getTeacherId().equals(teacherId)) {
            throw new BusinessException(403, "无权操作该班级");
        }
        return generateClassLoginCode(cls);
    }

    /** 学生：凭班级码获取本班学生名单 */
    public Map<String, Object> getStudentsByClassCode(String code) {
        SysClass cls = findValidClassByCode(code);
        List<Map<String, Object>> students = userRepository.findByClassId(cls.getId()).stream()
                .filter(u -> "STUDENT".equals(u.getRole()) && u.getStatus() == 1)
                .sorted(Comparator.comparing(SysUser::getRealName))
                .map(u -> {
                    Map<String, Object> row = new HashMap<String, Object>();
                    row.put("studentId", u.getId());
                    row.put("realName", u.getRealName());
                    row.put("username", u.getUsername());
                    return row;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("classId", cls.getId());
        result.put("className", cls.getName());
        result.put("students", students);
        return result;
    }

    /** 学生：班级码 + 选名登录 */
    public Map<String, Object> loginByClassCode(String code, Long studentId) {
        SysClass cls = findValidClassByCode(code);
        SysUser user = userRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException("学生不存在"));
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException("仅学生可使用班级码登录");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        if (user.getClassId() == null || !user.getClassId().equals(cls.getId())) {
            throw new BusinessException("该学生不属于此班级");
        }
        return buildLoginResult(user);
    }

    /** 获取当前登录用户（从 Security 上下文中取） */
    public SysUser currentUser() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return loginUser.getUser();
    }

    public Map<String, Object> currentUserProfile() {
        SysUser user = currentUser();
        Map<String, Object> info = new HashMap<String, Object>();
        info.put("userId", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("role", user.getRole());
        info.put("classId", user.getClassId());
        putClassName(info, user);
        return info;
    }

    private Map<String, Object> buildLoginResult(SysUser user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        result.put("classId", user.getClassId());
        putClassName(result, user);
        return result;
    }

    private void putClassName(Map<String, Object> result, SysUser user) {
        if (user.getClassId() == null) {
            return;
        }
        classRepository.findById(user.getClassId())
                .ifPresent(cls -> result.put("className", cls.getName()));
    }

    private Map<String, Object> generateClassLoginCode(SysClass cls) {
        String code = randomUniqueCode();
        cls.setLoginCode(code);
        cls.setLoginCodeExpiresAt(LocalDateTime.now().plusHours(CLASS_CODE_TTL_HOURS));
        classRepository.save(cls);
        return toCodeResponse(cls);
    }

    private Map<String, Object> toCodeResponse(SysClass cls) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("classId", cls.getId());
        result.put("className", cls.getName());
        result.put("loginCode", cls.getLoginCode());
        result.put("expiresAt", cls.getLoginCodeExpiresAt());
        return result;
    }

    private String randomUniqueCode() {
        for (int i = 0; i < 20; i++) {
            String code = String.format("%06d", random.nextInt(1_000_000));
            if (!classRepository.findByLoginCode(code).isPresent()) {
                return code;
            }
        }
        throw new BusinessException("生成班级码失败，请重试");
    }

    private SysClass findValidClassByCode(String code) {
        if (code == null || !code.trim().matches("\\d{6}")) {
            throw new BusinessException("请输入 6 位数字班级码");
        }
        SysClass cls = classRepository.findByLoginCode(code.trim())
                .orElseThrow(() -> new BusinessException("班级码无效，请核对后重试"));
        if (isCodeExpired(cls)) {
            throw new BusinessException("班级码已过期，请向老师索取新码");
        }
        return cls;
    }

    private boolean isCodeExpired(SysClass cls) {
        return cls.getLoginCodeExpiresAt() == null
                || cls.getLoginCodeExpiresAt().isBefore(LocalDateTime.now());
    }
}
