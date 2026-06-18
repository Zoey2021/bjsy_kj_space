package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "course_grade")
public class CourseGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    private String description;

    /** MAIN=配套教材，SCHOOL=校本教材 */
    @Column(name = "textbook_type", nullable = false, length = 20)
    private String textbookType = "MAIN";

    /** 封面图 URL，如 /course-covers/grade3-up.jpg */
    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    /** 电子教材 PDF，如 /textbooks/grade5-down.pdf */
    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;
}
