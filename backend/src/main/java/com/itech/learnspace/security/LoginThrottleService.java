package com.itech.learnspace.security;

import com.itech.learnspace.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账号密码登录简易限流：同一账号短时间内失败次数过多则暂时锁定。
 */
@Service
public class LoginThrottleService {

    private static final int MAX_FAILURES = 8;
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private static final long CLEAN_EVERY_MS = 60 * 1000L;

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<String, Attempt>();
    private volatile long lastCleanAt = 0L;

    public void assertAllowed(String username) {
        cleanupIfNeeded();
        String key = normalize(username);
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - attempt.firstFailAt > WINDOW_MS) {
            attempts.remove(key);
            return;
        }
        if (attempt.failures >= MAX_FAILURES) {
            long leftMin = Math.max(1L, (WINDOW_MS - (now - attempt.firstFailAt) + 59_999L) / 60_000L);
            throw new BusinessException("登录失败次数过多，请 " + leftMin + " 分钟后再试");
        }
    }

    public void onSuccess(String username) {
        attempts.remove(normalize(username));
    }

    public void onFailure(String username) {
        String key = normalize(username);
        long now = System.currentTimeMillis();
        Attempt attempt = attempts.get(key);
        if (attempt == null || now - attempt.firstFailAt > WINDOW_MS) {
            attempts.put(key, new Attempt(1, now));
            return;
        }
        attempt.failures++;
    }

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanAt < CLEAN_EVERY_MS) {
            return;
        }
        lastCleanAt = now;
        Iterator<Map.Entry<String, Attempt>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Attempt> e = it.next();
            if (now - e.getValue().firstFailAt > WINDOW_MS) {
                it.remove();
            }
        }
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private static final class Attempt {
        private int failures;
        private final long firstFailAt;

        private Attempt(int failures, long firstFailAt) {
            this.failures = failures;
            this.firstFailAt = firstFailAt;
        }
    }
}
