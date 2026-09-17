package com.xxxtxm.modules.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxxtxm.common.config.AiProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

/**
 * DeepSeek / 通义千问 等 OpenAI 兼容 API 的 HTTP 客户端
 *
 * <p>支持：
 *   <li>普通对话（chat） — 返回完整响应</li>
 *   <li>流式对话（chatStream） — 逐 Token 回调，用于 SSE 推送到前端</li>
 * </p>
 */
@Slf4j
@Component
public class AiModelClient {

    private final AiProperties props;
    private final ObjectMapper mapper;

    public AiModelClient(AiProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
    }

    // ── 请求/响应 DTO（OpenAI 兼容格式） ──────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;    // system / user / assistant
        private String content;
    }

    @Data
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private boolean stream = false;
        private double temperature = 0.7;
        @JsonProperty("max_tokens")
        private int maxTokens = 2048;
    }

    @Data
    public static class Choice {
        private int index;
        private Message message;   // 非流式
        private Message delta;     // 流式（增量）
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }

    @Data
    public static class ChatResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
    }

    // ── 流式响应 DTO ──

    @Data
    public static class StreamChoice {
        private int index;
        private StreamDelta delta;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    public static class StreamDelta {
        private String content;
    }

    @Data
    public static class StreamResponse {
        private List<StreamChoice> choices;
    }

    // ── 公共 API ────────────────────────────────────────

    /** 检查 AI 是否可用 */
    public boolean isAvailable() {
        return props.isEnabled();
    }

    /** 普通对话（非流式） */
    public String chat(List<Message> messages) {
        if (!isAvailable()) {
            return "[AI 服务未配置 — 请在环境变量中设置 AI_API_KEY]";
        }
        try {
            ChatRequest req = buildRequest(messages, false);
            HttpURLConnection conn = createConnection();
            String body = mapper.writeValueAsString(req);
            sendRequest(conn, body);

            int code = conn.getResponseCode();
            if (code != 200) {
                String err = readStream(conn.getErrorStream());
                log.error("AI API 返回 {}: {}", code, err);
                return "[AI 服务返回错误: " + code + "]";
            }

            String respBody = readStream(conn.getInputStream());
            ChatResponse resp = mapper.readValue(respBody, ChatResponse.class);
            return resp.getChoices() != null && !resp.getChoices().isEmpty()
                    ? resp.getChoices().get(0).getMessage().getContent()
                    : "[AI 未返回有效响应]";
        } catch (IOException e) {
            log.error("AI API 调用失败", e);
            return "[AI 服务暂时不可用: " + e.getMessage() + "]";
        }
    }

    /** 流式对话 — 每收到一个 Token 调用一次 onChunk，结束时调用 onComplete */
    public void chatStream(List<Message> messages, Consumer<String> onChunk, Runnable onComplete) {
        chatStream(messages, onChunk, onComplete, props.getTemperature(), props.getMaxTokens());
    }

    /** 流式对话（可覆盖 temperature 和 maxTokens） */
    public void chatStream(List<Message> messages, Consumer<String> onChunk, Runnable onComplete,
                           double temperature, int maxTokens) {
        if (!isAvailable()) {
            onChunk.accept("[AI 服务未配置]");
            onComplete.run();
            return;
        }
        try {
            ChatRequest req = buildRequest(messages, true, temperature, maxTokens);
            HttpURLConnection conn = createConnection();
            String body = mapper.writeValueAsString(req);
            sendRequest(conn, body);

            int code = conn.getResponseCode();
            if (code != 200) {
                String err = readStream(conn.getErrorStream());
                log.error("AI SSE API 返回 {}: {}", code, err);
                onChunk.accept("[AI 服务返回错误: " + code + "]");
                onComplete.run();
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        try {
                            StreamResponse sr = mapper.readValue(data, StreamResponse.class);
                            if (sr.getChoices() != null) {
                                for (StreamChoice c : sr.getChoices()) {
                                    if (c.getDelta() != null && c.getDelta().getContent() != null) {
                                        onChunk.accept(c.getDelta().getContent());
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                            // 跳过无法解析的行（如注释行）
                        }
                    }
                }
            }
            onComplete.run();
        } catch (IOException e) {
            log.error("AI SSE API 调用失败", e);
            onChunk.accept("[AI 连接中断: " + e.getMessage() + "]");
            onComplete.run();
        }
    }

    // ── 内部方法 ────────────────────────────────────────

    private ChatRequest buildRequest(List<Message> messages, boolean stream) {
        return buildRequest(messages, stream, props.getTemperature(), props.getMaxTokens());
    }

    private ChatRequest buildRequest(List<Message> messages, boolean stream, double temperature, int maxTokens) {
        ChatRequest req = new ChatRequest();
        req.setModel(props.getModel());
        req.setMessages(messages);
        req.setStream(stream);
        req.setTemperature(temperature);
        req.setMaxTokens(maxTokens);
        return req;
    }

    private HttpURLConnection createConnection() throws IOException {
        URI uri = URI.create(props.getApiUrl() + "/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + props.getApiKey());
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(60_000);
        return conn;
    }

    private void sendRequest(HttpURLConnection conn, String body) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    private String readStream(java.io.InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
