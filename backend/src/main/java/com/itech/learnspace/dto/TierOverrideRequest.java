package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class TierOverrideRequest {
    private Long studentId;
    private String tier;
}
