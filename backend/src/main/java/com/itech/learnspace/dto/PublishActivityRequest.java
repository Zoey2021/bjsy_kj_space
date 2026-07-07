package com.itech.learnspace.dto;

import lombok.Data;

import java.util.List;

@Data
public class PublishActivityRequest {
    private Long classId;
    private String activityTitle;
    private String objectives;
    private String teacherObjectives;
    private String studentObjectives;
    private String evaluation;
    private String progress;
    private String literacy;
    /** 兼容旧版：整段活动文本 */
    private String activities;
    private List<ActivitySlotRequest> activitySlots;
}
