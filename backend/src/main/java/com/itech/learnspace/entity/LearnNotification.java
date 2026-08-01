package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "learn_notification")
public class LearnNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "target_student_id")
    private Long targetStudentId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "msg_type", nullable = false, length = 32)
    private String msgType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
