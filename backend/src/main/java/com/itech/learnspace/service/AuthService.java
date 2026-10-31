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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private static final String FIRST_PASSWORD_CLASS = "2021级6班";
    private final Random random = new Random();

    private final SysUserRepository userRepository;
    private final SysClassRepository classRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SchoolManageService schoolManageService;
    private final AccountAccessService accountAccessService;

    public AuthService(SysUserRepository userRepository,
                       SysClassRepository classRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       SchoolManageService schoolManageService,
                       AccountAccessService accountAccessService) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.schoolManageService = schoolManageService;
        this.accountAccessService = accountAccessService;
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

        Map<String, Object> result = buildLoginResult(user);
        if (needsFirstPasswordChange(user)) {
            result.put("mustChangePassword", true);
        }
        return result;
    }

    /**
     * 学生第一次用账号密码登录后，把自己的新密码记下来。
     * 只有 2021级6班 会记下明文，方便教师在名单里查看。
     */
    @Transactional
    public Map<String, Object> changeOwnPassword(String oldPassword, String newPassword) {
        SysUser session = currentUser();
        SysUser user = userRepository.findById(session.getId())
                .orElseThrow(() -> new BusinessException("账号不存在"));
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        String next = newPassword == null ? "" : newPassword.trim();
        if (next.length() < 4 || next.length() > 20) {
            throw new BusinessException("新密码请设置 4 到 20 位");
        }
        if (passwordEncoder.matches(next, user.getPassword())) {
            throw new BusinessException("新密码不能和原来的一样");
        }
        user.setPassword(passwordEncoder.encode(next));
        if (inFirstPasswordClass(user)) {
            user.setPasswordNote(next);
            user.setPasswordChangedAt(LocalDateTime.now());
        }
        userRepository.save(user);
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("changed", true);
        return out;
    }

    public boolean needsFirstPasswordChange(SysUser user) {
        if (user == null || !"STUDENT".equals(user.getRole())) {
            return false;
        }
        if (user.getPasswordChangedAt() != null) {
            return false;
        }
        return inFirstPasswordClass(user);
    }

    public boolean inFirstPasswordClass(SysUser user) {
        if (user == null || user.getClassId() == null) {
            return false;
        }
        SysClass cls = classRepository.findById(user.getClassId()).orElse(null);
        if (cls == null || cls.getName() == null) {
            return false;
        }
        return FIRST_PASSWORD_CLASS.equals(cls.getName().trim());
    }

    /**
     * 钉钉登录：调用方已经确认对方属于本校钉钉组织。
     * 这里再核对平台里的教师或管理员账号，学生和其他人不能进入。
     */
    @Transactional
    public Map<String, Object> loginTeacherFromDingTalk(String unionId, String mobile, String name) {
        if (!StringUtils.hasText(unionId)) {
            throw new BusinessException("钉钉没有返回用户身份");
        }
        SysUser bound = userRepository.findByDingtalkUnionId(unionId).orElse(null);
        if (bound != null) {
            assertSchoolTeacher(bound);
            return buildLoginResult(bound);
        }

        String phone = digits(mobile);
        SysUser matched = null;
        if (phone.length() >= 11) {
            String tail = phone.substring(phone.length() - 11);
            matched = userRepository.findByUsername(tail).orElse(null);
            if (matched == null) {
                for (SysUser user : staff()) {
                    if (tail.equals(digits(user.getUsername()))) {
                        matched = user;
                        break;
                    }
                }
            }
        }
        if (matched != null && "STUDENT".equals(matched.getRole())) {
            throw new BusinessException("该手机号对应的是学生账号，不能使用钉钉教师登录");
        }
        if (matched == null && StringUtils.hasText(name)) {
            String trimmed = name.trim();
            java.util.List<SysUser> sameName = new java.util.ArrayList<SysUser>();
            for (SysUser user : staff()) {
                if (trimmed.equals(user.getRealName() == null ? "" : user.getRealName().trim())
                        && user.getStatus() != null && user.getStatus() == 1) {
                    sameName.add(user);
                }
            }
            if (sameName.size() > 1) {
                throw new BusinessException("有多位教师同名，请让管理员把教师账号改成钉钉手机号后再登录");
            }
            if (sameName.size() == 1) {
                matched = sameName.get(0);
            }
        }
        if (matched == null) {
            throw new BusinessException("仅限本校已开通的教师。请确认钉钉姓名或手机号与平台教师账号一致");
        }
        assertSchoolTeacher(matched);
        if (StringUtils.hasText(matched.getDingtalkUnionId()) && !unionId.equals(matched.getDingtalkUnionId())) {
            throw new BusinessException("该教师账号已绑定其他钉钉，请联系管理员");
        }
        matched.setDingtalkUnionId(unionId);
        userRepository.save(matched);
        return buildLoginResult(matched);
    }

    private java.util.List<SysUser> staff() {
        java.util.List<SysUser> all = new java.util.ArrayList<SysUser>();
        all.addAll(userRepository.findByRole("TEACHER"));
        all.addAll(userRepository.findByRole("ADMIN"));
        return all;
    }

    private void assertSchoolTeacher(SysUser user) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        if (!"TEACHER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new BusinessException("仅限本校教师登录");
        }
    }

    private static String digits(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch >= '0' && ch <= '9') {
                out.append(ch);
            }
        }
        return out.toString();
    }

    /** 教师：获取或刷新班级登录码 */
    public Map<String, Object> getOrGenerateClassLoginCode(Long classId, Long teacherId) {
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        if (cls.getLoginCode() == null || isCodeExpired(cls)) {
            return generateClassLoginCode(cls);
        }
        return toCodeResponse(cls);
    }

    /** 教师：强制刷新班级登录码 */
    public Map<String, Object> refreshClassLoginCode(Long classId, Long teacherId) {
        SysClass cls = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("班级不存在"));
        return generateClassLoginCode(cls);
    }

    /** 学生：凭班级码获取本班学生名单 */
    public Map<String, Object> getStudentsByClassCode(String code) {
        SysClass cls = findValidClassByCode(code);
        List<Map<String, Object>> students = schoolManageService.loadStudentsOfClass(cls.getId()).stream()
                .filter(u -> Integer.valueOf(1).equals(u.getStatus()))
                .sorted(Comparator
                        .comparingInt((SysUser u) -> {
                            try {
                                return Integer.parseInt(PretestG4Service.parseStudentNo(u.getUsername()));
                            } catch (Exception e) {
                                return Integer.MAX_VALUE;
                            }
                        })
                        .thenComparing(u -> u.getRealName() == null ? "" : u.getRealName()))
                .map(u -> {
                    Map<String, Object> row = new HashMap<String, Object>();
                    row.put("studentId", u.getId());
                    row.put("realName", u.getRealName());
                    row.put("username", u.getUsername());
                    row.put("studentNo", PretestG4Service.parseStudentNo(u.getUsername()));
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
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        if (user.getClassId() == null || !user.getClassId().equals(cls.getId())) {
            boolean inClass = schoolManageService.loadStudentsOfClass(cls.getId()).stream()
                    .anyMatch(u -> studentId.equals(u.getId()));
            if (!inClass) {
                throw new BusinessException("该学生不属于此班级");
            }
            user.setClassId(cls.getId());
            userRepository.save(user);
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
        info.put("identityCode", user.getIdentityCode());
        info.put("classId", user.getClassId());
        info.put("accountScope", accountAccessService.scope(user));
        putClassName(info, user);
        SysUser fresh = userRepository.findById(user.getId()).orElse(user);
        if (needsFirstPasswordChange(fresh)) {
            info.put("mustChangePassword", true);
        }
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
        result.put("identityCode", user.getIdentityCode());
        result.put("classId", user.getClassId());
        result.put("accountScope", accountAccessService.scope(user));
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
