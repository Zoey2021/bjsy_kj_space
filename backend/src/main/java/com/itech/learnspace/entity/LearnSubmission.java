package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "learn_submission")
public class LearnSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "content_json", nullable = false, columnDefinition = "TEXT")
    private String contentJson;

    private Integer score;

    @Column(length = 20)
    private String status = "SUBMITTED";

    @Column(name = "study_seconds")
    private Integer studySeconds = 0;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        submittedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** 本次提交新获得的积分。不入库，只随本次提交结果返回。 */
    @Transient
    private Integer pointsAwarded;

    /** 提交后的排名积分。不入库。 */
    @Transient
    private Integer earnedPoints;

    /** 提交后的可兑奖积分。不入库。 */
    @Transient
    private Integer redeemablePoints;
}
