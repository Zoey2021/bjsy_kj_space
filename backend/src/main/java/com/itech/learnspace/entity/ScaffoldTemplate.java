package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "scaffold_templates")
public class ScaffoldTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "tier_a_json", nullable = false, columnDefinition = "TEXT")
    private String tierAJson;

    @Column(name = "tier_b_json", nullable = false, columnDefinition = "TEXT")
    private String tierBJson;

    @Column(name = "tier_c_json", nullable = false, columnDefinition = "TEXT")
    private String tierCJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
