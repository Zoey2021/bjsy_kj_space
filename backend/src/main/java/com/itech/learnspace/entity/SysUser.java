package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
public class SysUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(nullable = false, length = 20)
    private String role;

    /** 门户身份。INFO_TEACHER 表示信息老师，登录后进入现有教师工作台。 */
    @Column(name = "identity_code", length = 32)
    private String identityCode;

    @Column(name = "class_id")
    private Long classId;

    /** 教师手动覆盖档位；学生接口不得回传该字段 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "tier_override", length = 4)
    private String tierOverride;

    @Column(nullable = false)
    private Integer status = 1;

    /** 钉钉 unionId。不回传给前端。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "dingtalk_union_id", length = 64)
    private String dingtalkUnionId;

    /** 学生自己改过的密码，只在教师名单里展示，不随登录信息返回。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password_note", length = 64)
    private String passwordNote;

    /** 学生自己修改密码的时间。为空表示还没用账号密码完成首次改密。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
