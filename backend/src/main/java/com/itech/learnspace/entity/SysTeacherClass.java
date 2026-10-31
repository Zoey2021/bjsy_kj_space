package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "sys_teacher_class", uniqueConstraints = {
        @UniqueConstraint(name = "uk_teacher_class", columnNames = {"teacher_id", "class_id"})
})
public class SysTeacherClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "class_id", nullable = false)
    private Long classId;
}
