package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "learn_mall_class")
public class PointsMallClassOpen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false, unique = true)
    private Long classId;

    @Column(name = "apply_open", nullable = false)
    private Integer applyOpen = 0;

    @Column(name = "opened_by")
    private Long openedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }
}
