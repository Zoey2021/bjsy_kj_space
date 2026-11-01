package com.itech.learnspace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 人工智能实验室 · 扣子 /stream_run 对话代理。
 * 生产地址与 Token 只放环境变量，勿写进前端。
 */
@Data
@Component
@ConfigurationProperties(prefix = "coze.chat")
public class CozeChatProperties {
    /** 上游完整地址，例如 https://xxx.coze.site/stream_run */
    private String apiUrl = "";
    /** 上游 access token（Bearer），sat_ 或平台下发的 JWT */
    private String token = "";
    /** 图片对外公网根，如 https://bjsysmartedu.cn */
    private String publicOrigin = "https://bjsysmartedu.cn";
    /** 连接超时毫秒 */
    private int connectTimeoutMs = 15000;
    /** 读超时毫秒（流式对话宜较长） */
    private int readTimeoutMs = 300000;
}
