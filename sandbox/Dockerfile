# =============================================
# CodeMate 判题沙箱 — Go-Judge + Python/Java 运行时
# =============================================
#
# 构建镜像:
#   docker build -t codemate-sandbox:latest -f sandbox/Dockerfile sandbox/
#
# 或通过 docker compose 一键构建:
#   docker compose build go-judge
# =============================================

FROM redis:7.2-alpine

# ── 使用阿里云 Alpine 镜像加速 ──
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

# ── 安装 Python 3 和 OpenJDK 17 ──
RUN apk add --no-cache \
    python3 \
    openjdk17-jdk

# ── 从官方镜像复制 Go-Judge 二进制 ──
COPY --from=criyle/go-judge:latest /opt/go-judge /usr/local/bin/go-judge

# ── 创建工作目录 ──
RUN mkdir -p /tmp /code && chmod 777 /tmp

# ── 时区设置 ──
ENV TZ=Asia/Shanghai

EXPOSE 5050 5051

ENTRYPOINT ["/usr/local/bin/go-judge"]
