package com.itech.learnspace.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/** 教师和班级的任课匹配。一张表里一行表示这位老师可以查看这个班。 */
@Component
public class TeacherClassSchema {

    private static final Logger log = LoggerFactory.getLogger(TeacherClassSchema.class);
    private final JdbcTemplate jdbcTemplate;

    public TeacherClassSchema(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureTable() {
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS sys_teacher_class ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT,"
                            + "teacher_id BIGINT NOT NULL,"
                            + "class_id BIGINT NOT NULL,"
                            + "PRIMARY KEY (id),"
                            + "UNIQUE KEY uk_teacher_class (teacher_id, class_id)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch (Exception e) {
            log.warn("任课班级表检查未完成：{}", e.getMessage());
        }
    }
}
