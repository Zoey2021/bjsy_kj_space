package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "course_lesson")
public class CourseLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "duration_min")
    private Integer durationMin = 40;

    private Integer status = 1;
}
