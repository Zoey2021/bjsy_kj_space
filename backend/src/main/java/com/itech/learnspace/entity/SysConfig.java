package com.itech.learnspace.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "sys_config")
public class SysConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 50)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 500)
    private String configValue;

    private String description;
}
