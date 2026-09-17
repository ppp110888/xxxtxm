package com.xxxtxm.modules.submission.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 判题结果 WebSocket 推送处理器
 *
 * <p>连接路径: /ws/judge?token=xxx</p>
 * <p>推送消息格式: { "type": "judge_complete", "submissionId": 1, "status": "AC", ... }</p>
 */
@Slf4j
@Component
public class JudgeWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** userId → WebSocket session */
    private static final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            try { session.close(CloseStatus.POLICY_VIOLATION); } catch (Exception ignored) {}
            log.warn("WebSocket connection rejected: no auth");
            return;
        }

        // 关闭旧连接（同一用户只能有一个 WS 连接）
        WebSocketSession old = sessions.put(userId, session);
        if (old != null && old.isOpen()) {
            try { old.close(CloseStatus.NORMAL.withReason("新的连接已建立")); } catch (Exception ignored) {}
        }

        log.info("WebSocket connected: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.values().removeIf(s -> s.getId().equals(session.getId()));
        log.info("WebSocket disconnected: sessionId={}, reason={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端发来的心跳，回复 pong
        if ("ping".equals(message.getPayload().trim())) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable ex) {
        log.warn("WebSocket transport error: sessionId={}, error={}",
                session.getId(), ex.getMessage());
    }

    // ── 推送方法 ──

    /**
     * 向指定用户推送判题结果
     */
    public void pushJudgeResult(Long userId, Long submissionId, String status,
                                 Integer timeUsed, Integer memoryUsed, String detail) {
        WebSocketSession session = sessions.get(userId);
        if (session == null || !session.isOpen()) return;

        try {
            Map<String, Object> payload = Map.of(
                    "type", "judge_result",
                    "submissionId", submissionId,
                    "status", status,
                    "timeUsed", timeUsed != null ? timeUsed : 0,
                    "memoryUsed", memoryUsed != null ? memoryUsed : 0,
                    "detail", detail != null ? detail : ""
            );
            String json = objectMapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.warn("Failed to push judge result to userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * 广播系统通知（暂未使用）
     */
    public void broadcast(String type, String message) {
        sessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    String json = objectMapper.writeValueAsString(Map.of("type", type, "message", message));
                    session.sendMessage(new TextMessage(json));
                } catch (IOException ignored) {}
            }
        });
    }
}
