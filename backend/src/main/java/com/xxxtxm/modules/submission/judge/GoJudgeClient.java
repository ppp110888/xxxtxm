package com.xxxtxm.modules.submission.judge;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Go-Judge 沙箱 HTTP 客户端
 *
 * <p>调用 Go-Judge REST API 执行用户代码。
 * 仅在非 dev 模式下启用（dev 模式使用 DevJudgeService 本地执行）。
 * 生产环境需部署 Go-Judge Docker 容器，暴露 5050 端口。</p>
 *
 * <p>Go-Judge API 参考: https://github.com/criyle/go-judge</p>
 */
@Slf4j
@Component
@Profile("!dev")
public class GoJudgeClient {

    private final RestTemplate restTemplate;

    @Value("${codemate.judge.sandbox-url:http://localhost:5050}")
    private String sandboxUrl;

    public GoJudgeClient() {
        this.restTemplate = new RestTemplate();
        // 使用 SimpleClientHttpRequestFactory 配置超时
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(30000);
        this.restTemplate.setRequestFactory(factory);
    }

    /**
     * 在沙箱中执行代码
     *
     * @param language   编程语言 (python / java)
     * @param source     源代码
     * @param input      标准输入
     * @param timeLimitMs  时间限制 (ms)
     * @param memoryLimitMb 内存限制 (MB)
     * @return 执行结果
     */
    public JudgeResult execute(String language, String source, String input,
                                int timeLimitMs, int memoryLimitMb) {
        GoJudgeRequest request = buildRequest(language, source, input, timeLimitMs, memoryLimitMb);
        log.debug("Go-Judge request: language={}, timeLimit={}ms, memLimit={}MB",
                language, timeLimitMs, memoryLimitMb);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<GoJudgeRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<GoJudgeResponse[]> response = restTemplate.postForEntity(
                    sandboxUrl + "/run", entity, GoJudgeResponse[].class);

            if (response.getBody() == null || response.getBody().length == 0) {
                return JudgeResult.runtimeError("Empty response from sandbox", 0, 0);
            }

            return parseResponse(response.getBody()[0]);
        } catch (Exception e) {
            log.error("Go-Judge execution failed: {}", e.getMessage());
            return JudgeResult.runtimeError("Sandbox error: " + e.getMessage(), 0, 0);
        }
    }

    // ── 构建请求体 ──

    private GoJudgeRequest buildRequest(String language, String source, String input,
                                         int timeLimitMs, int memoryLimitMb) {
        String[] cmd = buildCommand(language);
        String codeFile = language.equals("java") ? "Main.java" : "solution.py";

        // 根据语言调整资源限制
        long procLimit = language.equals("java") ? 200L : 50L;

        GoJudgeRequest.Cmd cmdObj = GoJudgeRequest.Cmd.builder()
                .args(cmd)
                .env(List.of(
                        "PATH=/usr/bin:/bin:/usr/local/bin",
                        "LANG=C.UTF-8",
                        "LC_ALL=C.UTF-8"))
                .cpuLimit((long) timeLimitMs * 1_000_000L)  // ms → ns
                .memoryLimit((long) memoryLimitMb * 1024 * 1024L)  // MB → bytes
                .procLimit(procLimit)
                .copyIn(Map.of(
                        codeFile, new GoJudgeRequest.FileRef(null, source),
                        "input.txt", new GoJudgeRequest.FileRef(null, input != null ? input : ""),
                        "stdout", new GoJudgeRequest.FileRef(null, ""),
                        "stderr", new GoJudgeRequest.FileRef(null, "")))
                .copyOut(List.of("stdout", "stderr"))
                .build();

        return GoJudgeRequest.builder()
                .cmd(Collections.singletonList(cmdObj))
                .build();
    }

    /**
     * 构建执行命令。
     *
     * <p>Go-Judge v1.12+ 兼容格式：
     *   stdout/stderr 通过 copyIn 创建空文件，shell 重定向写入，
     *   copyOut 收集输出。不再使用 cmd.files 字段。</p>
     */
    private String[] buildCommand(String language) {
        if ("python".equals(language)) {
            return new String[]{"/bin/sh", "-c",
                    "python3 solution.py < input.txt > stdout 2> stderr"};
        } else {
            // Java: 编译 → 运行（编译错误写入 stderr，运行输出追加到 stdout）
            return new String[]{"/bin/sh", "-c",
                    "javac Main.java 2> stderr && java -Xmx128m Main < input.txt >> stdout 2>> stderr"};
        }
    }

    // ── 解析响应 ──

    private JudgeResult parseResponse(GoJudgeResponse resp) {
        String stdout = resp.getFiles() != null ? resp.getFiles().getOrDefault("stdout", "") : "";
        String stderr = resp.getFiles() != null ? resp.getFiles().getOrDefault("stderr", "") : "";

        boolean isTimeout = "Time Limit Exceeded".equals(resp.getStatus());
        boolean isMemoryOver = "Memory Limit Exceeded".equals(resp.getStatus());

        return JudgeResult.builder()
                .stdout(stdout)
                .stderr(stderr)
                .exitCode(resp.getExitStatus())
                .timeUsed(resp.getTime() != null ? resp.getTime().intValue() : 0)
                .memoryUsed(resp.getMemory() != null ? resp.getMemory().intValue() : 0)
                .timeout(isTimeout)
                .memoryOverflow(isMemoryOver)
                .build();
    }

    // ──────────────────────────────────────
    // Go-Judge API DTO
    // ──────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GoJudgeRequest {
        private List<Cmd> cmd;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Cmd {
            private String[] args;
            private List<String> env;
            private List<FileRef> files;
            @JsonProperty("cpuLimit")
            private Long cpuLimit;
            @JsonProperty("memoryLimit")
            private Long memoryLimit;
            @JsonProperty("procLimit")
            private Long procLimit;
            @JsonProperty("copyIn")
            private Map<String, FileRef> copyIn;
            @JsonProperty("copyOut")
            private List<String> copyOut;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class FileRef {
            private String name;
            private String content;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoJudgeResponse {
        private String status;
        private Integer exitStatus;
        private Long time;    // ms
        private Long memory;  // KB
        private Map<String, String> files;
    }
}
