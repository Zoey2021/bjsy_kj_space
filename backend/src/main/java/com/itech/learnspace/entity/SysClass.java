package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_class")
public class SysClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "grade_name", nullable = false, length = 20)
    private String gradeName;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /** 班级登录码（6 位数字，教师选班后生成） */
    @Column(name = "login_code", length = 6)
    private String loginCode;

    @Column(name = "login_code_expires_at")
    private LocalDateTime loginCodeExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
