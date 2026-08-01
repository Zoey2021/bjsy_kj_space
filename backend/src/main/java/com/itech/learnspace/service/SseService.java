package com.itech.learnspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> classEmitters = new ConcurrentHashMap<>();
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> studentEmitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 教师看板 SSE */
    public SseEmitter subscribe(Long classId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        classEmitters.computeIfAbsent(classId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        bindLifecycle(classEmitters, classId, emitter);
        sendEvent(emitter, "connected", "SSE连接成功");
        log.info("教师 SSE 连接，班级ID={}", classId);
        return emitter;
    }

    /** 学生端 SSE */
    public SseEmitter subscribeStudent(Long studentId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        studentEmitters.computeIfAbsent(studentId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        bindLifecycle(studentEmitters, studentId, emitter);
        sendEvent(emitter, "connected", "SSE连接成功");
        log.info("学生 SSE 连接，studentId={}", studentId);
        return emitter;
    }

    public void broadcast(Long classId, Object data) {
        broadcastToList(classEmitters.get(classId), "dashboard", data);
    }

    /** 向单个学生推送通知 { type, data, timestamp } */
    public void pushToStudent(Long studentId, String type, Map<String, Object> data) {
        Map<String, Object> envelope = envelope(type, data);
        broadcastToList(studentEmitters.get(studentId), "message", envelope);
    }

    /** 向全班在线学生推送 */
    public void pushToStudents(java.util.Collection<Long> studentIds, String type, Map<String, Object> data) {
        if (studentIds == null) {
            return;
        }
        Map<String, Object> envelope = envelope(type, data);
        for (Long sid : studentIds) {
            broadcastToList(studentEmitters.get(sid), "message", envelope);
        }
    }

    @Scheduled(fixedRate = 30000)
    public void heartbeat() {
        Map<String, Object> ping = envelope("ping", new HashMap<>());
        for (CopyOnWriteArrayList<SseEmitter> list : classEmitters.values()) {
            broadcastToList(list, "ping", ping);
        }
        for (CopyOnWriteArrayList<SseEmitter> list : studentEmitters.values()) {
            broadcastToList(list, "ping", ping);
        }
    }

    private Map<String, Object> envelope(String type, Map<String, Object> data) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("type", type);
        envelope.put("data", data != null ? data : new HashMap<>());
        envelope.put("timestamp", System.currentTimeMillis());
        return envelope;
    }

    private void broadcastToList(CopyOnWriteArrayList<SseEmitter> list, String eventName, Object payload) {
        if (list == null || list.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("SSE 序列化失败", e);
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(json));
            } catch (IOException e) {
                list.remove(emitter);
            }
        }
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void bindLifecycle(Map<Long, CopyOnWriteArrayList<SseEmitter>> map, Long key, SseEmitter emitter) {
        Runnable remove = () -> removeEmitter(map, key, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
    }

    private void removeEmitter(Map<Long, CopyOnWriteArrayList<SseEmitter>> map, Long key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = map.get(key);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                map.remove(key);
            }
        }
    }
}
