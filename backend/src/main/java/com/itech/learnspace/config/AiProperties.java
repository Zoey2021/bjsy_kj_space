package com.itech.learnspace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    /** 兼容模式 chat/completions，脚手架已改为走工作流，可留空 */
    private String baseUrl = "";
    private String apiKey = "";
    private String model = "Qwen/Qwen2.5-7B-Instruct";
    /** 百炼控制台应用卡片上的 APP_ID */
    private String appId = "";
    /** 百炼应用 API 前缀，默认 https://dashscope.aliyuncs.com/api/v1 */
    private String appBaseUrl = "https://dashscope.aliyuncs.com/api/v1";
    /**
     * 是否把课时变量放进 input.biz_params。
     * 开始节点有自定义变量时保持 true；若报未知参数，设为 false，只走 prompt。
     */
    private boolean passBizParams = true;
}
