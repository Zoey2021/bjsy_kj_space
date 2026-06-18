package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "course_resource")
public class CourseResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "res_type", nullable = false, length = 20)
    private String resType;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
