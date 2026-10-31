package com.itech.learnspace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 张老师、贾斌、倪婕妤是信息老师。身份写在数据库里，登录后进入现有教师工作台。
 * 张老师同时保留最高管理员权限：角色仍由 role 字段决定，这里不改密码、不改角色。
 */
@Component
@Order(30)
public class IdentitySeeder implements ApplicationRunner {

    public static final String INFO_TEACHER = "INFO_TEACHER";

    private static final Logger log = LoggerFactory.getLogger(IdentitySeeder.class);
    private final JdbcTemplate jdbcTemplate;

    public IdentitySeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        int n = jdbcTemplate.update(
                "UPDATE sys_user SET identity_code = ? WHERE real_name IN ('张老师', '贾斌', '倪婕妤')",
                INFO_TEACHER);
        log.info("已确认信息老师身份 {} 人", n);
    }
}
