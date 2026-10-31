package com.itech.learnspace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/** 给学生表补上「自己修改的密码」和修改时间，供 2021 级 6 班首次登录改密使用。 */
@Component
public class PasswordNoteSchema {

    private static final Logger log = LoggerFactory.getLogger(PasswordNoteSchema.class);
    private final JdbcTemplate jdbcTemplate;

    public PasswordNoteSchema(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureColumns() {
        addColumn("ALTER TABLE sys_user ADD COLUMN password_note VARCHAR(64) NULL");
        addColumn("ALTER TABLE sys_user ADD COLUMN password_changed_at DATETIME NULL");
    }

    private void addColumn(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("duplicate") || msg.contains("1060")) {
                return;
            }
            log.warn("学生改密字段检查未完成：{}", e.getMessage());
        }
    }
}
