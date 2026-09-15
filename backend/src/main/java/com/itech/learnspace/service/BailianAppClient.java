package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.config.AiProperties;
import com.itech.learnspace.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调用阿里云百炼「工作流 / 智能体应用」：POST /api/v1/apps/{APP_ID}/completion
 */
@Service
public class BailianAppClient {

    private static final Logger log = LoggerFactory.getLogger(BailianAppClient.class);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public BailianAppClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(180000);
        this.restTemplate = new RestTemplate(factory);
    }

    public boolean isConfigured() {
        return StringUtils.hasText(aiProperties.getApiKey())
                && StringUtils.hasText(aiProperties.getAppId());
    }

    public String runWorkflow(String prompt, Map<String, Object> bizParams) {
        if (!isConfigured()) {
            throw new BusinessException("未配置百炼工作流，请在服务器设置 AI_APP_ID 与 AI_API_KEY");
        }
        String content = callOnce(prompt, bizParams);
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("百炼工作流没有返回内容，请检查结束节点是否输出文本或 JSON");
        }
        return content;
    }

    private String callOnce(String prompt, Map<String, Object> bizParams) {
        String url = trimSlash(aiProperties.getAppBaseUrl()) + "/apps/"
                + aiProperties.getAppId().trim() + "/completion";

        Map<String, Object> input = new LinkedHashMap<String, Object>();
        input.put("prompt", StringUtils.hasText(prompt) ? prompt : "请生成本环节三档脚手架");
        if (aiProperties.isPassBizParams() && bizParams != null && !bizParams.isEmpty()) {
            input.put("biz_params", bizParams);
        }

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("input", input);
        body.put("parameters", new LinkedHashMap<String, Object>());
        body.put("debug", new LinkedHashMap<String, Object>());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey().trim());
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<String>(objectMapper.writeValueAsString(body), headers),
                    String.class);
            if (response.getBody() == null) {
                log.warn("Bailian empty body, status={}", response.getStatusCode());
                return null;
            }
            return extractText(response.getBody());
        } catch (HttpStatusCodeException e) {
            String msg = extractError(e.getResponseBodyAsString(), e.getStatusCode().value());
            log.warn("Bailian HTTP {}: {}", e.getStatusCode(), msg);
            throw new BusinessException(msg);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Bailian call error: {}", e.getMessage());
            throw new BusinessException("百炼工作流调用失败，请稍后重试");
        }
    }

    private String extractText(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.hasNonNull("code") && StringUtils.hasText(root.path("code").asText())
                    && !"Success".equalsIgnoreCase(root.path("code").asText())) {
                String msg = root.path("message").asText("百炼工作流调用失败");
                throw new BusinessException(msg);
            }
            JsonNode output = root.path("output");
            String text = output.path("text").asText("");
            if (StringUtils.hasText(text)) {
                return text;
            }
            if (output.has("A") || output.has("B") || output.has("C")) {
                return objectMapper.writeValueAsString(output);
            }
            JsonNode result = output.path("result");
            if (result.isTextual() && StringUtils.hasText(result.asText())) {
                return result.asText();
            }
            if (result.isObject()) {
                return objectMapper.writeValueAsString(result);
            }
            log.warn("Bailian response has no text, request_id={}", root.path("request_id").asText(""));
            return null;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Bailian parse error: {}", e.getMessage());
            return null;
        }
    }

    private String extractError(String body, int status) {
        if (StringUtils.hasText(body)) {
            try {
                JsonNode root = objectMapper.readTree(body);
                String msg = root.path("message").asText("");
                if (StringUtils.hasText(msg)) {
                    return "百炼工作流：" + msg;
                }
            } catch (Exception ignored) {
            }
        }
        if (status == 401 || status == 403) {
            return "百炼 API Key 无效或没有该应用权限";
        }
        return "百炼工作流调用失败（HTTP " + status + "）";
    }

    private static String trimSlash(String url) {
        if (!StringUtils.hasText(url)) {
            return "https://dashscope.aliyuncs.com/api/v1";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
