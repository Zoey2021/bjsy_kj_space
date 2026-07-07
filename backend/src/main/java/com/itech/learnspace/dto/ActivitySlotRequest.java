package com.itech.learnspace.dto;

import lombok.Data;

@Data
public class ActivitySlotRequest {
    private String title;
    /** platform | dify | feixiang */
    private String source;
    private String content;
    private String uploadedPath;
    private Boolean enabled;
}
