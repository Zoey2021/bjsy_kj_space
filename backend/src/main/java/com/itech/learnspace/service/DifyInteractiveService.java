package com.itech.learnspace.service;

import com.itech.learnspace.config.DifyProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class DifyInteractiveService {

    private final DifyProperties difyProperties;
    private final DifyWorkflowClient workflowClient;

    public DifyInteractiveService(DifyProperties difyProperties, DifyWorkflowClient workflowClient) {
        this.difyProperties = difyProperties;
        this.workflowClient = workflowClient;
    }

    public String generateActivityHtml(String lessonTitle, String studentObjectives,
                                         String activityTitle, String activityContent, String user) {
        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put("lesson_title", nullToEmpty(lessonTitle));
        inputs.put("student_objectives", nullToEmpty(studentObjectives));
        inputs.put("activity_title", nullToEmpty(activityTitle));
        inputs.put("activity_content", nullToEmpty(activityContent));
        return workflowClient.runWorkflow(
                difyProperties.getInteractiveApiKey(),
                difyProperties.getInteractiveAppMode(),
                inputs,
                user);
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
