package com.itech.learnspace.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerateActivitiesRequest {
    private String objectives;
    private String studentObjectives;
    private List<ActivitySlotRequest> activitySlots;
}
