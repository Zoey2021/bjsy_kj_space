package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "learn_visit_log")
public class LearnVisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "page_url", length = 255)
    private String pageUrl;

    @Column(name = "duration_sec")
    private Integer durationSec = 0;

    @Column(name = "visited_at")
    private LocalDateTime visitedAt;

    @PrePersist
    public void prePersist() {
        visitedAt = LocalDateTime.now();
    }
}
