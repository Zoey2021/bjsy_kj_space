package com.itech.learnspace.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ScaffoldPublishRequest {
    private Long templateId;
    private List<Map<String, Object>> items;
}
