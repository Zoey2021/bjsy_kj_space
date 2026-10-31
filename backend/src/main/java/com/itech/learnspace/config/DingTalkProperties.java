package com.itech.learnspace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 钉钉企业内部应用。只接受本企业（本校）成员登录。
 * AppKey、AppSecret、CorpId 只放环境变量，不写进代码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "dingtalk")
public class DingTalkProperties {
    private String clientId = "";
    private String clientSecret = "";
    private String corpId = "";
    /** 钉钉回调地址，必须与开放平台里登记的完全一致 */
    private String redirectUri = "https://bjsysmartedu.cn/api/auth/dingtalk/callback";
    /** 登录成功后回到的网站地址，不要带末尾斜杠 */
    private String frontendOrigin = "https://bjsysmartedu.cn";
}
