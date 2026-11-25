package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "unit_quiz_submission",
        uniqueConstraints = @UniqueConstraint(name = "uk_unit_quiz_lesson_student", columnNames = {"lesson_id", "student_id"}))
public class UnitQuizSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "quiz_key", length = 64)
    private String quizKey;

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

    @Column(name = "answers_json", nullable = false, columnDefinition = "LONGTEXT")
    private String answersJson;

    /** SUBMITTED / GRADED */
    @Column(nullable = false, length = 20)
    private String status = "SUBMITTED";

    @Column
    private Integer score;

    @Column(name = "grade_level", length = 4)
    private String gradeLevel;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "graded_by")
    private Long gradedBy;

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
