package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "course_activity_publish")
public class CourseActivityPublish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "html_path", length = 500)
    private String htmlPath;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "published_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date publishedAt;

    @Column(name = "plan_json", columnDefinition = "TEXT")
    private String planJson;
}
