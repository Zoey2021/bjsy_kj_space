package com.itech.learnspace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "learn-space.activity")
public class ActivityProperties {
    private String htmlOutputDir = "./lesson-generated";
}
