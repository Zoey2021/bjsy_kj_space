package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class ParkReviewRequest {
    private Long studentId;
    /** 这一条授权属于哪一课 */
    private Long lessonId;
    /** approve / reject / revoke */
    private String action;
    private String note;
}
