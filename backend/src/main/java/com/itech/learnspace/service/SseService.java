package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE 实时推送服务（核心亮点）
 *
 * 工作原理（通俗版）：
 * 1. 教师打开学情看板时，浏览器与后端建立一条"长连接"（SSE）
 * 2. 连接保存在本类的 emitters 列表中，按班级ID分组
 * 3. 学生提交任务后，LearnService 调用 broadcast() 方法
 * 4. broadcast 把最新学情数据推送给该班级所有已连接的教师
 * 5. 教师页面收到推送后自动刷新数字，无需手动 F5
 */
@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    // key=班级ID, value=该班级所有教师的 SSE 连接列表
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> classEmitters = new ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 教师打开看板时调用，注册一个 SSE 连接 */
    public SseEmitter subscribe(Long classId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        classEmitters.computeIfAbsent(classId, k -> new CopyOnWriteArrayList<SseEmitter>()).add(emitter);

        // 连接断开或超时时，从列表中移除
        Runnable remove = () -> removeEmitter(classId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("SSE连接成功"));
        } catch (IOException e) {
            removeEmitter(classId, emitter);
        }

        log.info("SSE 连接建立，班级ID={}", classId);
        return emitter;
    }

    /**
     * 广播学情更新（学生提交后触发）
     * @param classId 班级ID
     * @param data    最新看板数据（JSON对象）
     */
    public void broadcast(Long classId, Object data) {
        CopyOnWriteArrayList<SseEmitter> list = classEmitters.get(classId);
        if (list == null || list.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("SSE 数据序列化失败", e);
            return;
        }

        log.info("SSE 广播，班级ID={}，连接数={}", classId, list.size());

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("dashboard").data(json));
            } catch (IOException e) {
                removeEmitter(classId, emitter);
            }
        }
    }

    private void removeEmitter(Long classId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = classEmitters.get(classId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                classEmitters.remove(classId);
            }
        }
    }
}
