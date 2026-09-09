package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "learn_pretest_g6")
public class LearnPretestG6 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "student_name", nullable = false, length = 50)
    private String studentName;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "student_no", length = 20)
    private String studentNo;

    @Column(name = "fill_score", nullable = false)
    private Integer fillScore = 0;

    @Column(name = "choice_right", nullable = false)
    private Integer choiceRight = 0;

    @Column(name = "choice_score", nullable = false)
    private Integer choiceScore = 0;

    @Column(name = "op_score", nullable = false)
    private Integer opScore = 0;

    @Column(name = "att_score", nullable = false)
    private Integer attScore = 0;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Column(name = "level_code", length = 4)
    private String levelCode;

    @Column(name = "content_json", nullable = false, columnDefinition = "LONGTEXT")
    private String contentJson;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (submittedAt == null) {
            submittedAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
