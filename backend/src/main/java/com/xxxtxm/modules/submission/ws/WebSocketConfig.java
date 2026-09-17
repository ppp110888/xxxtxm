package com.xxxtxm.modules.submission.ws;

import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 配置 — 注册判题结果推送端点
 *
 * <p>连接地址: ws://host:port/ws/judge?token=<sa-token-value></p>
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final JudgeWebSocketHandler judgeWebSocketHandler;

    @Value("${codemate.security.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(judgeWebSocketHandler, "/ws/judge")
                .addInterceptors(new AuthHandshakeInterceptor())
                .setAllowedOriginPatterns(allowedOrigins.split(","));
    }

    /**
     * 握手拦截器 — 从 query string 提取 Sa-Token 并认证
     */
    static class AuthHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler wsHandler, Map<String, Object> attributes) {
            URI uri = request.getURI();
            String query = uri.getQuery();
            if (query == null) return false;

            // 解析 token 参数
            String token = null;
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if ("token".equals(kv[0]) && kv.length > 1) {
                    token = kv[1];
                    break;
                }
            }
            if (token == null || token.isEmpty()) return false;

            // 验证 Sa-Token
            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId == null) return false;
                attributes.put("userId", Long.valueOf(loginId.toString()));
                return true;
            } catch (Exception e) {
                log.debug("WebSocket auth failed: {}", e.getMessage());
                return false;
            }
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Exception exception) {
            // no-op
        }
    }
}
