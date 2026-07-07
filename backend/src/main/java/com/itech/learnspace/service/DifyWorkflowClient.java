package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.config.DifyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DifyWorkflowClient {

    private static final Logger log = LoggerFactory.getLogger(DifyWorkflowClient.class);

    private final DifyProperties difyProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DifyWorkflowClient(DifyProperties difyProperties) {
        this.difyProperties = difyProperties;
    }

    public String runWorkflow(String apiKey, String appMode, Map<String, Object> inputs, String user) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        String mode = StringUtils.hasText(appMode) ? appMode : "workflow";
        String baseUrl = trimTrailingSlash(difyProperties.getBaseUrl());
        String url = "workflow".equals(mode)
                ? baseUrl + "/workflows/run"
                : baseUrl + "/completion-messages";

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", StringUtils.hasText(user) ? user : "learn-space");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<String>(objectMapper.writeValueAsString(body), headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Dify request failed: {}", response.getStatusCode());
                return null;
            }
            return extractText(response.getBody());
        } catch (Exception e) {
            log.warn("Dify call error: {}", e.getMessage());
            return null;
        }
    }

    private String extractText(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode outputs = root.path("data").path("outputs");
            if (outputs.has("html")) {
                return outputs.get("html").asText("");
            }
            if (outputs.has("text")) {
                return outputs.get("text").asText("");
            }
            if (root.path("answer").isTextual()) {
                return root.get("answer").asText("");
            }
        } catch (Exception e) {
            log.warn("Dify response parse error: {}", e.getMessage());
        }
        return null;
    }

    private String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
