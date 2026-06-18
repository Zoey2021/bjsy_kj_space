package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "course_unit")
public class CourseUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grade_id", nullable = false)
    private Long gradeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    private String description;
}
