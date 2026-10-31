package com.itech.learnspace.service;

import com.itech.learnspace.entity.SysClass;
import com.itech.learnspace.entity.SysUser;
import com.itech.learnspace.exception.BusinessException;
import com.itech.learnspace.repository.SysClassRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 账号可见范围：ALL 全量，KECHUANG 仅科创启航，DEFAULT 配套课程（教师端科创上锁）。
 * 学生端可以看到配套课程和科创启航。
 */
@Service
public class AccountAccessService {

    public static final String ALL = "ALL";
    public static final String KECHUANG = "KECHUANG";
    public static final String DEFAULT = "DEFAULT";

    private static final Set<String> FULL_NAMES = new HashSet<String>(Arrays.asList("张老师", "贾斌", "倪婕妤"));
    private static final Set<String> FULL_USERNAMES = new HashSet<String>(Arrays.asList(
            "teacher1", "18857121982", "admin"));
    private static final Set<String> KECHUANG_NAMES = new HashSet<String>(Arrays.asList("郑超煌", "郭伟杰"));
    private static final Set<String> KECHUANG_USERNAMES = new HashSet<String>(Arrays.asList(
            "15396254154", "13588326564"));

    private final SysClassRepository classRepository;

    public AccountAccessService(SysClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    public String scope(SysUser user) {
        if (user == null) {
            return DEFAULT;
        }
        if ("ADMIN".equals(user.getRole())) {
            return ALL;
        }
        String name = user.getRealName() == null ? "" : user.getRealName().trim();
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        if (FULL_NAMES.contains(name) || FULL_USERNAMES.contains(username)) {
            return ALL;
        }
        if (KECHUANG_NAMES.contains(name) || KECHUANG_USERNAMES.contains(username)) {
            return KECHUANG;
        }
        return DEFAULT;
    }

    public boolean hidesClassData(SysUser user) {
        return KECHUANG.equals(scope(user));
    }

    public void assertClassData(SysUser user) {
        if (hidesClassData(user)) {
            throw new BusinessException(403, "当前账号仅可查看科创启航");
        }
    }

    /** 2021 届只看六年级上册，2023 届只看四年级上册；其他届不限制到单册。 */
    public String studentOnlyBook(SysUser user) {
        if (user == null || !"STUDENT".equals(user.getRole())) {
            return null;
        }
        String year = enrollmentYear(user);
        if ("2021".equals(year)) {
            return "六年级上册";
        }
        if ("2023".equals(year)) {
            return "四年级上册";
        }
        return null;
    }

    public List<Map<String, Object>> filterTextbooks(SysUser user, List<Map<String, Object>> books) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (books == null) {
            return out;
        }
        for (Map<String, Object> book : books) {
            if (canSeeBook(user, book)) {
                out.add(book);
            }
        }
        return out;
    }

    public void assertBook(SysUser user, String name, String description, String textbookType) {
        if (!canSeeBook(user, name, description, textbookType)) {
            throw new BusinessException(403, "当前账号不能查看这本教材");
        }
    }

    public void assertBook(SysUser user, Map<String, Object> book) {
        if (book == null) {
            return;
        }
        assertBook(user,
                str(book.get("name")),
                str(book.get("description")),
                str(book.get("textbookType")));
    }

    private boolean canSeeBook(SysUser user, Map<String, Object> book) {
        return canSeeBook(user, str(book.get("name")), str(book.get("description")), str(book.get("textbookType")));
    }

    private boolean canSeeBook(SysUser user, String name, String description, String textbookType) {
        boolean kechuang = isKechuang(name, description);
        if (user == null) {
            return !kechuang;
        }
        if ("STUDENT".equals(user.getRole())) {
            return true;
        }
        String scope = scope(user);
        if (ALL.equals(scope)) {
            return true;
        }
        if (KECHUANG.equals(scope)) {
            return kechuang;
        }
        return !kechuang;
    }

    private String enrollmentYear(SysUser user) {
        if (user.getClassId() != null) {
            SysClass cls = classRepository.findById(user.getClassId()).orElse(null);
            if (cls != null && cls.getName() != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20\\d{2})级").matcher(cls.getName());
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        String username = user.getUsername() == null ? "" : user.getUsername();
        java.util.regex.Matcher suffix = java.util.regex.Pattern.compile("(20[12]\\d)\\d{4,}$").matcher(username);
        if (suffix.find()) {
            return suffix.group(1);
        }
        return null;
    }

    static boolean isKechuang(String name, String description) {
        String blob = (name == null ? "" : name) + " " + (description == null ? "" : description);
        return blob.contains("科创启航");
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
