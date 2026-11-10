package com.itech.learnspace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 创建科创启航教师账号。已存在则不改密码。
 * 新建账号使用临时弱口令，登录后会被强制改密。
 */
@Component
@Order(24)
public class KechuangTeacherSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KechuangTeacherSeeder.class);
    /** BCrypt of 123456 — 仅作首次建号占位，平台会强制改密 */
    private static final String PASSWORD_PLACEHOLDER =
            "$2b$10$W/PCZrjdz.jTDLRRklWBzumK7tDUmpuk/pseBDLVylqyvSfyb7HOK";

    private final JdbcTemplate jdbcTemplate;

    public KechuangTeacherSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensure("15396254154", "郑超煌");
        ensure("13588326564", "郭伟杰");
    }

    private void ensure(String username, String realName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, role, real_name FROM sys_user WHERE username = ?", username);
        if (rows.isEmpty()) {
            jdbcTemplate.update(
                    "INSERT INTO sys_user (username, password, real_name, role, class_id, status) VALUES (?,?,?,?,NULL,1)",
                    username, PASSWORD_PLACEHOLDER, realName, "TEACHER");
            log.info("已创建科创教师账号：{} / {}（请登录后立即修改密码）", realName, username);
            return;
        }
        Map<String, Object> row = rows.get(0);
        String role = row.get("role") == null ? "" : String.valueOf(row.get("role"));
        if (!"TEACHER".equals(role) && !"ADMIN".equals(role)) {
            log.warn("账号 {} 已存在且不是教师（{}），跳过科创教师创建", username, role);
            return;
        }
        jdbcTemplate.update(
                "UPDATE sys_user SET real_name = ?, status = 1 WHERE username = ?",
                realName, username);
        log.info("科创教师账号已存在：{} / {}", realName, username);
    }
}
