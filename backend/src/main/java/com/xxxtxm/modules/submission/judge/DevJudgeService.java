package com.xxxtxm.modules.submission.judge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/**
 * 开发环境本地判题器
 *
 * <p>使用 ProcessBuilder 在本地执行用户代码（仅 dev 环境启用）。
 * 生产环境必须使用 Go-Judge 沙箱以保证宿主机安全。</p>
 */
@Slf4j
@Service
@Profile("dev")
public class DevJudgeService {

    private static final Path WORK_DIR = Paths.get("./data/judge-work");

    /**
     * 本地编译并执行代码
     *
     * @param language      编程语言
     * @param source        源代码
     * @param input         测试输入
     * @param timeLimitMs   时间限制 (ms)
     * @param memoryLimitMb 内存限制 (MB) — 仅作记录，本地执行无法精确限制内存
     * @return 执行结果
     */
    public JudgeResult execute(String language, String source, String input,
                                int timeLimitMs, int memoryLimitMb) {
        Path workDir = null;
        try {
            Files.createDirectories(WORK_DIR);
            workDir = Files.createTempDirectory(WORK_DIR, "judge-");

            if ("python".equals(language)) {
                return executePython(workDir, source, input, timeLimitMs);
            } else if ("java".equals(language)) {
                return executeJava(workDir, source, input, timeLimitMs);
            } else {
                return JudgeResult.runtimeError("Unsupported language: " + language, 0, 0);
            }
        } catch (Exception e) {
            log.error("Judge execution error: {}", e.getMessage(), e);
            return JudgeResult.runtimeError("System error: " + e.getMessage(), 0, 0);
        } finally {
            if (workDir != null) {
                try { deleteRecursively(workDir); } catch (Exception ignored) {}
            }
        }
    }

    // ── Python 执行 ──

    private JudgeResult executePython(Path workDir, String source, String input,
                                       int timeLimitMs) throws IOException {
        // 写入源文件
        Path scriptFile = workDir.resolve("solution.py");
        Files.writeString(scriptFile, source, StandardCharsets.UTF_8);

        // 写入输入文件
        Path inputFile = workDir.resolve("input.txt");
        Files.writeString(inputFile, input != null ? input : "", StandardCharsets.UTF_8);

        long startTime = System.currentTimeMillis();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python", scriptFile.toAbsolutePath().toString());
            pb.directory(workDir.toFile());
            pb.redirectInput(inputFile.toFile());
            pb.redirectErrorStream(false);
            // 强制 UTF-8 输出，防止中文 Windows GBK 乱码
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            Process process = pb.start();
            boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);

            int timeUsed = (int) (System.currentTimeMillis() - startTime);

            if (!finished) {
                process.destroyForcibly();
                return JudgeResult.timeout(timeUsed, 0);
            }

            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            int exitCode = process.exitValue();

            if (exitCode != 0) {
                return JudgeResult.runtimeError(stderr.isEmpty() ? "Exit code: " + exitCode : stderr,
                        timeUsed, 0);
            }

            return JudgeResult.success(stdout, timeUsed, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            int timeUsed = (int) (System.currentTimeMillis() - startTime);
            return JudgeResult.runtimeError("Interrupted", timeUsed, 0);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("python")) {
                // Python not found, try python3
                return executePythonFallback(workDir, scriptFile, inputFile, timeLimitMs);
            }
            throw e;
        }
    }

    private JudgeResult executePythonFallback(Path workDir, Path scriptFile, Path inputFile,
                                               int timeLimitMs) throws IOException {
        long startTime = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(
                "python3", scriptFile.toAbsolutePath().toString());
        pb.directory(workDir.toFile());
        pb.redirectInput(inputFile.toFile());
        pb.redirectErrorStream(false);
        pb.environment().put("PYTHONIOENCODING", "utf-8");

        Process process = pb.start();
        try {
            boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
            int timeUsed = (int) (System.currentTimeMillis() - startTime);

            if (!finished) {
                process.destroyForcibly();
                return JudgeResult.timeout(timeUsed, 0);
            }

            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            if (process.exitValue() != 0) {
                return JudgeResult.runtimeError(stderr.isEmpty() ? "Runtime Error" : stderr,
                        timeUsed, 0);
            }
            return JudgeResult.success(stdout, timeUsed, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return JudgeResult.runtimeError("Interrupted", 0, 0);
        }
    }

    // ── Java 执行 ──

    private JudgeResult executeJava(Path workDir, String source, String input,
                                     int timeLimitMs) throws IOException {
        // 写入源文件
        Path javaFile = workDir.resolve("Main.java");
        Files.writeString(javaFile, source, StandardCharsets.UTF_8);

        // 写入输入文件
        Path inputFile = workDir.resolve("input.txt");
        Files.writeString(inputFile, input != null ? input : "", StandardCharsets.UTF_8);

        // 编译
        long startTime = System.currentTimeMillis();
        try {
            ProcessBuilder compilePb = new ProcessBuilder(
                    "javac", "-encoding", "UTF-8", "-J-Dfile.encoding=UTF-8",
                    javaFile.toAbsolutePath().toString());
            compilePb.directory(workDir.toFile());
            Process compileProcess = compilePb.start();
            boolean compileDone = compileProcess.waitFor(30, TimeUnit.SECONDS);

            if (!compileDone || compileProcess.exitValue() != 0) {
                String stderr = readStream(compileProcess.getErrorStream());
                return JudgeResult.compileError("Compilation error:\n" + stderr);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return JudgeResult.compileError("Compilation interrupted");
        }

        int compileTime = (int) (System.currentTimeMillis() - startTime);
        int remainingTime = Math.max(100, timeLimitMs - compileTime);

        // 运行
        try {
            ProcessBuilder runPb = new ProcessBuilder(
                    "java", "-Dfile.encoding=UTF-8", "-cp",
                    workDir.toAbsolutePath().toString(), "Main");
            runPb.directory(workDir.toFile());
            runPb.redirectInput(inputFile.toFile());
            runPb.redirectErrorStream(false);

            Process runProcess = runPb.start();
            boolean finished = runProcess.waitFor(remainingTime, TimeUnit.MILLISECONDS);

            int timeUsed = (int) (System.currentTimeMillis() - startTime);

            if (!finished) {
                runProcess.destroyForcibly();
                return JudgeResult.timeout(timeUsed, 0);
            }

            String stdout = readStream(runProcess.getInputStream());
            String stderr = readStream(runProcess.getErrorStream());

            if (runProcess.exitValue() != 0) {
                return JudgeResult.runtimeError(stderr.isEmpty() ? "Runtime Error" : stderr,
                        timeUsed, 0);
            }

            return JudgeResult.success(stdout, timeUsed, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return JudgeResult.runtimeError("Execution interrupted",
                    (int) (System.currentTimeMillis() - startTime), 0);
        }
    }

    // ── 工具方法 ──

    private String readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!sb.isEmpty()) sb.append('\n');
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.delete(p); } catch (Exception ignored) {}
                });
            }
        }
    }
}
