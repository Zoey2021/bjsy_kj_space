package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "learn_mall_item")
public class PointsMallItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 20)
    private String icon;

    @Column(nullable = false)
    private Integer cost;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Integer enabled = 1;
}
