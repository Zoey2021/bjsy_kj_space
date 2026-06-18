package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "course_task")
public class CourseTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "task_type", length = 20)
    private String taskType = "FORM";

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "max_score")
    private Integer maxScore = 10;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
