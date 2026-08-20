package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class ParkReviewRequest {
    private Long studentId;
    /** approve / reject / revoke */
    private String action;
    private String note;
}
