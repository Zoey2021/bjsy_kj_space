package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "learn_mall_order")
public class PointsMallOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_id")
    private Long classId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", nullable = false, length = 80)
    private String itemName;

    @Column(name = "item_icon", length = 20)
    private String itemIcon;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false, length = 20)
    private String status = "APPLIED";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
