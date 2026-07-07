package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "learn_submission_log")
public class LearnSubmissionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "content_json", nullable = false, columnDefinition = "TEXT")
    private String contentJson;

    private Integer score;

    @Column(name = "study_seconds")
    private Integer studySeconds = 0;

    @Column(name = "activity_key", length = 50)
    private String activityKey;

    @Column(name = "activity_index")
    private Integer activityIndex;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
