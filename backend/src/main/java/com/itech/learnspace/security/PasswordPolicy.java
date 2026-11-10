package com.itech.learnspace.security;

import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 口令策略：识别常见弱口令，并校验改密强度。
 */
@Component
public class PasswordPolicy {

    /** 公开报告与演示环境常见的弱口令 */
    private static final String[] WEAK_PLAINTEXTS = {
            "123456", "12345678", "111111", "000000", "password", "Password",
            "admin", "admin123", "Admin123", "qwerty", "abc123", "654321"
    };

    private final PasswordEncoder passwordEncoder;

    public PasswordPolicy(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public boolean matchesWeakPassword(SysUser user) {
        if (user == null || !StringUtils.hasText(user.getPassword())) {
            return false;
        }
        String hash = user.getPassword();
        for (String weak : WEAK_PLAINTEXTS) {
            try {
                if (passwordEncoder.matches(weak, hash)) {
                    return true;
                }
            } catch (Exception ignored) {
                // 非法 hash 不当作弱口令，交给登录失败处理
            }
        }
        return false;
    }

    public boolean isWeakPlaintext(String raw) {
        if (!StringUtils.hasText(raw)) {
            return true;
        }
        String next = raw.trim();
        for (String weak : WEAK_PLAINTEXTS) {
            if (weak.equalsIgnoreCase(next) || weak.equals(next)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 学生：6～20 位且不能是公开弱口令。
     * 教师/管理员：8～32 位，至少含字母和数字，且不能是弱口令。
     */
    public void validateNewPassword(String role, String raw) {
        String next = raw == null ? "" : raw.trim();
        boolean staff = "ADMIN".equals(role) || "TEACHER".equals(role);
        int min = staff ? 8 : 6;
        int max = staff ? 32 : 20;
        if (next.length() < min || next.length() > max) {
            throw new BusinessException(staff
                    ? "新密码请设置 8 到 32 位"
                    : "新密码请设置 6 到 20 位");
        }
        if (isWeakPlaintext(next)) {
            throw new BusinessException("新密码过于简单，请勿使用常见弱口令");
        }
        if (staff) {
            boolean hasLetter = next.matches(".*[A-Za-z].*");
            boolean hasDigit = next.matches(".*\\d.*");
            if (!hasLetter || !hasDigit) {
                throw new BusinessException("教师/管理员密码需同时包含字母和数字");
            }
        }
    }
}
