package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itech.learnspace.config.DingTalkProperties;
import com.itech.learnspace.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉企业内部登录。授权页带上本校 CorpId，再用企业令牌核对 unionId。
 * 不在本校通讯录里的人，或不是平台教师账号的人，都不能进入。
 */
@Service
public class DingTalkAuthService {

    private static final Logger log = LoggerFactory.getLogger(DingTalkAuthService.class);
    private static final long STATE_TTL_MS = 10 * 60 * 1000L;

    private final DingTalkProperties properties;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final RestTemplate http;
    private final String jwtSecret;
    private final SecureRandom random = new SecureRandom();

    private volatile String corpToken = "";
    private volatile long corpTokenExpireAt = 0L;

    public DingTalkAuthService(DingTalkProperties properties,
                               AuthService authService,
                               ObjectMapper objectMapper,
                               @Value("${jwt.secret}") String jwtSecret) {
        this.properties = properties;
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.jwtSecret = jwtSecret;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(8000);
        this.http = new RestTemplate(factory);
    }

    public boolean enabled() {
        return StringUtils.hasText(properties.getClientId())
                && StringUtils.hasText(properties.getClientSecret())
                && StringUtils.hasText(properties.getCorpId())
                && StringUtils.hasText(properties.getRedirectUri());
    }

    public Map<String, Object> authorizeInfo() {
        Map<String, Object> out = new HashMap<String, Object>();
        out.put("enabled", Boolean.valueOf(enabled()));
        if (!enabled()) {
            out.put("url", "");
            return out;
        }
        out.put("url", buildAuthorizeUrl());
        return out;
    }

    public String callbackRedirect(String authCode, String state, String error) {
        try {
            if (StringUtils.hasText(error)) {
                throw new BusinessException("已取消钉钉登录");
            }
            if (!enabled()) {
                throw new BusinessException("钉钉登录尚未配置");
            }
            verifyState(state);
            if (!StringUtils.hasText(authCode)) {
                throw new BusinessException("钉钉没有返回登录凭证");
            }
            JsonNode me = fetchLoginUser(authCode);
            String unionId = firstText(me, "unionId", "unionid");
            if (!StringUtils.hasText(unionId)) {
                throw new BusinessException("钉钉没有返回用户身份，请在开放平台开通通讯录个人信息读权限");
            }
            String returnedCorp = firstText(me, "corpId", "corp_id");
            if (StringUtils.hasText(returnedCorp) && !properties.getCorpId().equals(returnedCorp)) {
                throw new BusinessException("仅限杭州市滨江实验小学教师登录");
            }
            JsonNode orgUser = requireSchoolMember(unionId);
            String dingUserId = orgUser.path("userid").asText("");
            JsonNode detail = fetchOrgUser(dingUserId);
            String mobile = firstText(me, "mobile");
            if (!StringUtils.hasText(mobile)) {
                mobile = detail.path("mobile").asText("");
            }
            String name = detail.path("name").asText("");
            if (!StringUtils.hasText(name)) {
                name = firstText(me, "nick", "name");
            }
            Map<String, Object> login = authService.loginTeacherFromDingTalk(unionId, mobile, name);
            String token = String.valueOf(login.get("token"));
            return frontendOrigin() + "/login#dingtalkToken=" + url(token);
        } catch (BusinessException e) {
            return frontendOrigin() + "/login?dingtalkError=" + url(e.getMessage());
        } catch (Exception e) {
            log.warn("钉钉登录失败: {}", e.getMessage());
            return frontendOrigin() + "/login?dingtalkError=" + url("钉钉登录失败，请稍后重试");
        }
    }

    private String buildAuthorizeUrl() {
        String state = signState();
        return "https://login.dingtalk.com/oauth2/auth?redirect_uri=" + url(properties.getRedirectUri())
                + "&response_type=code&client_id=" + url(properties.getClientId())
                + "&scope=openid&prompt=consent&corpId=" + url(properties.getCorpId())
                + "&state=" + url(state);
    }

    private JsonNode fetchLoginUser(String authCode) throws Exception {
        Map<String, String> body = new HashMap<String, String>();
        body.put("clientId", properties.getClientId());
        body.put("clientSecret", properties.getClientSecret());
        body.put("code", authCode);
        body.put("grantType", "authorization_code");
        JsonNode token = postJson("https://api.dingtalk.com/v1.0/oauth2/userAccessToken", body, null);
        String accessToken = firstText(token, "accessToken", "access_token");
        if (!StringUtils.hasText(accessToken)) {
            log.warn("钉钉换取用户令牌失败: {}", token.path("message").asText(token.path("code").asText("")));
            throw new BusinessException("钉钉登录未通过，请确认应用已开通登录权限");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-acs-dingtalk-access-token", accessToken);
        JsonNode me = exchange("https://api.dingtalk.com/v1.0/contact/users/me", HttpMethod.GET, new HttpEntity<Object>(headers), null);
        if (me.has("unionId") || me.has("unionid") || me.has("nick") || me.has("openId")) {
            if (token.has("corpId") && me instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) me).put("corpId", token.get("corpId").asText());
            }
            return me;
        }
        log.warn("钉钉个人信息接口未返回身份: {}", me.path("message").asText(""));
        throw new BusinessException("钉钉没有返回用户身份，请在开放平台开通通讯录个人信息读权限");
    }

    /** 用本校企业令牌核对：不在本校内部通讯录，或是外部联系人，都拒绝。 */
    private JsonNode requireSchoolMember(String unionId) throws Exception {
        String token = corpAccessToken();
        Map<String, String> body = new HashMap<String, String>();
        body.put("unionid", unionId);
        JsonNode root = postJson("https://oapi.dingtalk.com/topapi/user/getbyunionid?access_token=" + url(token), body, null);
        int code = root.path("errcode").asInt(-1);
        if (code != 0) {
            log.info("钉钉用户不在本校通讯录: errcode={}", Integer.valueOf(code));
            throw new BusinessException("仅限杭州市滨江实验小学教师登录");
        }
        JsonNode result = root.path("result");
        int contactType = result.path("contact_type").asInt(0);
        if (contactType != 0 || !StringUtils.hasText(result.path("userid").asText(""))) {
            throw new BusinessException("仅限杭州市滨江实验小学在职教师登录");
        }
        return result;
    }

    private JsonNode fetchOrgUser(String dingUserId) {
        if (!StringUtils.hasText(dingUserId)) {
            return objectMapper.createObjectNode();
        }
        try {
            String token = corpAccessToken();
            Map<String, String> body = new HashMap<String, String>();
            body.put("userid", dingUserId);
            body.put("language", "zh_CN");
            JsonNode root = postJson("https://oapi.dingtalk.com/topapi/v2/user/get?access_token=" + url(token), body, null);
            if (root.path("errcode").asInt(-1) != 0) {
                return objectMapper.createObjectNode();
            }
            return root.path("result");
        } catch (Exception e) {
            log.info("读取钉钉成员资料失败: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private synchronized String corpAccessToken() throws Exception {
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(corpToken) && now < corpTokenExpireAt) {
            return corpToken;
        }
        String url = "https://oapi.dingtalk.com/gettoken?appkey=" + url(properties.getClientId())
                + "&appsecret=" + url(properties.getClientSecret());
        JsonNode root = exchange(url, HttpMethod.GET, null, null);
        String token = root.path("access_token").asText("");
        if (!StringUtils.hasText(token)) {
            log.warn("获取钉钉企业令牌失败: errcode={}", Integer.valueOf(root.path("errcode").asInt(-1)));
            throw new BusinessException("钉钉登录尚未配置完成");
        }
        int expires = root.path("expires_in").asInt(7200);
        corpToken = token;
        corpTokenExpireAt = now + Math.max(60, expires - 120) * 1000L;
        return corpToken;
    }

    private JsonNode postJson(String url, Object body, HttpHeaders extra) throws Exception {
        HttpHeaders headers = extra == null ? new HttpHeaders() : extra;
        headers.setContentType(MediaType.APPLICATION_JSON);
        return exchange(url, HttpMethod.POST, new HttpEntity<Object>(body, headers), null);
    }

    private JsonNode exchange(String url, HttpMethod method, HttpEntity<?> entity, HttpHeaders extra) throws Exception {
        HttpEntity<?> request = entity;
        if (request == null) {
            request = new HttpEntity<Object>(extra);
        }
        try {
            ResponseEntity<String> response = http.exchange(url, method, request, String.class);
            String raw = response.getBody() == null ? "{}" : response.getBody();
            return objectMapper.readTree(raw);
        } catch (HttpStatusCodeException e) {
            String raw = e.getResponseBodyAsString();
            log.warn("钉钉接口 HTTP {}: {}", Integer.valueOf(e.getRawStatusCode()), trim(raw));
            if (StringUtils.hasText(raw)) {
                try {
                    return objectMapper.readTree(raw);
                } catch (Exception ignored) {
                    /* 按空结果处理 */
                }
            }
            throw new BusinessException("钉钉登录暂时无法连接，请稍后再试");
        }
    }

    private String signState() {
        long expiry = System.currentTimeMillis() + STATE_TTL_MS;
        byte[] nonce = new byte[8];
        random.nextBytes(nonce);
        String payload = expiry + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
        return payload + "." + hmac(payload);
    }

    private void verifyState(String state) {
        if (!StringUtils.hasText(state)) {
            throw new BusinessException("钉钉登录已过期，请重新点击钉钉登录");
        }
        int split = state.lastIndexOf('.');
        if (split <= 0) {
            throw new BusinessException("钉钉登录已过期，请重新点击钉钉登录");
        }
        String payload = state.substring(0, split);
        String sig = state.substring(split + 1);
        if (!MessageDigest.isEqual(sig.getBytes(StandardCharsets.UTF_8), hmac(payload).getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("钉钉登录校验失败，请重新点击钉钉登录");
        }
        int dot = payload.indexOf('.');
        long expiry;
        try {
            expiry = Long.parseLong(dot < 0 ? payload : payload.substring(0, dot));
        } catch (NumberFormatException e) {
            throw new BusinessException("钉钉登录已过期，请重新点击钉钉登录");
        }
        if (System.currentTimeMillis() > expiry) {
            throw new BusinessException("钉钉登录已过期，请重新点击钉钉登录");
        }
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException("钉钉登录校验失败");
        }
    }

    private String frontendOrigin() {
        String origin = properties.getFrontendOrigin();
        if (!StringUtils.hasText(origin)) {
            return "https://bjsysmartedu.cn";
        }
        while (origin.endsWith("/")) {
            origin = origin.substring(0, origin.length() - 1);
        }
        return origin;
    }

    private static String firstText(JsonNode node, String... names) {
        if (node == null) {
            return "";
        }
        for (String name : names) {
            String value = node.path(name).asText("");
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private static String url(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return "";
        }
    }

    private static String trim(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.length() > 180 ? raw.substring(0, 180) : raw;
    }
}
