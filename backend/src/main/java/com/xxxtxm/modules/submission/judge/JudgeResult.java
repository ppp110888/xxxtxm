package com.xxxtxm.modules.submission.judge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Go-Judge 沙箱执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResult {

    /** stdout 输出内容 */
    private String stdout;

    /** stderr 错误输出 */
    private String stderr;

    /** 进程退出码 (0=正常) */
    private Integer exitCode;

    /** 运行耗时 (ms) */
    private Integer timeUsed;

    /** 内存使用 (KB) */
    private Integer memoryUsed;

    /** 是否超时 */
    private boolean timeout;

    /** 是否超内存 */
    private boolean memoryOverflow;

    // ── 工厂方法 ──

    public static JudgeResult success(String stdout, int timeMs, int memKb) {
        return JudgeResult.builder()
                .stdout(stdout)
                .stderr("")
                .exitCode(0)
                .timeUsed(timeMs)
                .memoryUsed(memKb)
                .timeout(false)
                .memoryOverflow(false)
                .build();
    }

    public static JudgeResult compileError(String stderr) {
        return JudgeResult.builder()
                .stdout("")
                .stderr(stderr)
                .exitCode(1)
                .timeUsed(0)
                .memoryUsed(0)
                .timeout(false)
                .memoryOverflow(false)
                .build();
    }

    public static JudgeResult runtimeError(String stderr, int timeMs, int memKb) {
        return JudgeResult.builder()
                .stdout("")
                .stderr(stderr)
                .exitCode(1)
                .timeUsed(timeMs)
                .memoryUsed(memKb)
                .timeout(false)
                .memoryOverflow(false)
                .build();
    }

    public static JudgeResult timeout(int timeMs, int memKb) {
        return JudgeResult.builder()
                .stdout("")
                .stderr("Time Limit Exceeded")
                .exitCode(124)
                .timeUsed(timeMs)
                .memoryUsed(memKb)
                .timeout(true)
                .memoryOverflow(false)
                .build();
    }
}
