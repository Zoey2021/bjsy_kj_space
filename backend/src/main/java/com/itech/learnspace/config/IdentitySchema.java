package com.itech.learnspace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/** 给已有用户表补上门户身份字段。 */
@Component
public class IdentitySchema {

    private static final Logger log = LoggerFactory.getLogger(IdentitySchema.class);
    private final JdbcTemplate jdbcTemplate;

    public IdentitySchema(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureColumn() {
        try {
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN identity_code VARCHAR(32) NULL");
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("duplicate") || msg.contains("1060")) {
                return;
            }
            log.warn("门户身份字段检查未完成：{}", e.getMessage());
        }
    }
}
