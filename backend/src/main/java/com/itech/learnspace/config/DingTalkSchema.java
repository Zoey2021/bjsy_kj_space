package com.itech.learnspace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/** 已有数据库补上钉钉绑定字段，避免手工跑迁移。 */
@Component
public class DingTalkSchema {

    private static final Logger log = LoggerFactory.getLogger(DingTalkSchema.class);
    private final JdbcTemplate jdbcTemplate;

    public DingTalkSchema(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureColumn() {
        tryAlter("ALTER TABLE sys_user ADD COLUMN dingtalk_union_id VARCHAR(64) NULL");
        tryAlter("CREATE UNIQUE INDEX uk_sys_user_dingtalk_union ON sys_user (dingtalk_union_id)");
    }

    private void tryAlter(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (msg.contains("duplicate") || msg.contains("1060") || msg.contains("1061")) {
                return;
            }
            log.warn("钉钉登录字段检查未完成：{}", e.getMessage());
        }
    }
}
