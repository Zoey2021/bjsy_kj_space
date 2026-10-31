package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "learn_park_play")
public class LearnParkPlay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "game_id", nullable = false, length = 32)
    private String gameId;

    @Column(name = "game_title", nullable = false, length = 50)
    private String gameTitle;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;
}
