package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.config.AiProperties;
import com.itech.learnspace.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiCompatibleClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public OpenAiCompatibleClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(120000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return StringUtils.hasText(aiProperties.getApiKey()) && StringUtils.hasText(aiProperties.getBaseUrl());
    }

    public String chatJson(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(aiProperties.getApiKey()) || !StringUtils.hasText(aiProperties.getBaseUrl())) {
            throw new BusinessException("未配置 AI 接口，请在后端设置 AI_BASE_URL 与 AI_API_KEY");
        }
        String content = callOnce(systemPrompt, userPrompt);
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("AI 生成失败，请手动填写或重试");
        }
        return content;
    }

    private String callOnce(String systemPrompt, String userPrompt) {
        String url = trimSlash(aiProperties.getBaseUrl()) + "/chat/completions";
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("model", StringUtils.hasText(aiProperties.getModel()) ? aiProperties.getModel() : "Qwen/Qwen2.5-7B-Instruct");
        body.put("temperature", 0.3);
        body.put("max_tokens", 1800);
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        messages.add(msg("system", systemPrompt));
        messages.add(msg("user", userPrompt));
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<String>(objectMapper.writeValueAsString(body), headers),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("AI HTTP {}", response.getStatusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.warn("AI call error: {}", e.getMessage());
            return null;
        }
    }

    private static Map<String, String> msg(String role, String content) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
