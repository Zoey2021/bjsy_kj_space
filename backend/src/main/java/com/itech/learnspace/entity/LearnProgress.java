package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "learn_progress")
public class LearnProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(nullable = false, length = 20)
    private String status = "NOT_STARTED";

    @Column(name = "progress_percent")
    private Integer progressPercent = 0;

    @Column(name = "study_seconds")
    private Integer studySeconds = 0;

    @Column(name = "last_visit_at")
    private LocalDateTime lastVisitAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
