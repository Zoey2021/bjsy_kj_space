package com.itech.learnspace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {
    private String baseUrl = "http://localhost:8090/v1";
    private String recommendApiKey = "";
    private String recommendAppMode = "workflow";
    private String interactiveApiKey = "";
    private String interactiveAppMode = "workflow";
}
