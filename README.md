# CodeMate AI 伴学编程平台

CodeMate 是面向 Java/Python 初学者的学习与在线判题平台，包含知识树、题库、在线 IDE、学习进度和 AI 辅导能力。

## 技术栈

- 前端：Vue 3、Vite、Element Plus、Pinia、Monaco Editor
- 后端：Java 17、Spring Boot 3、MyBatis-Plus、Sa-Token
- 基础设施：MySQL、Redis、RabbitMQ、Go-Judge
- 开发数据库：H2 文件数据库

## 环境要求

- Java 17 和 Maven 3.9+
- Node.js 20+ 和 pnpm 9+
- Docker Desktop（仅集成或全栈模式需要）

## 启动方式

### 1. 轻量开发模式

不需要 Docker，后端使用 H2，适合日常开发。

```powershell
.\scripts\start-dev.ps1
```

也可以分别启动：

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

```powershell
cd frontend
pnpm install
pnpm dev
```

访问地址：

- 前端：http://localhost:5173
- API 文档：http://localhost:8080/doc.html
- H2 控制台：http://localhost:8080/h2-console

开发模式默认账号为 `admin/admin123` 和 `student/student123`，禁止在生产环境使用。

### 2. Docker 集成模式

只启动 MySQL、Redis、RabbitMQ 和 Go-Judge，前后端仍在本机运行：

```bash
docker compose up -d
cd backend && mvn spring-boot:run
cd frontend && pnpm dev
```

### 3. Docker 全栈模式

先创建本地环境配置：

```powershell
Copy-Item .env.example .env
```

修改 `.env` 中的全部 `change-me-*` 密码，然后运行：

```bash
docker compose --profile full up -d --build
```

前端位于 http://localhost，后端健康检查位于 http://localhost:8080/actuator/health。

停止服务：

```bash
docker compose --profile full down
```

只有明确需要清空数据库时才使用 `docker compose down -v`。

## 配置

生产配置全部通过环境变量注入，示例见 [.env.example](.env.example)。关键变量包括：

- `AI_API_KEY`
- `ADMIN_USERNAME` / `ADMIN_PASSWORD`
- `MYSQL_PASSWORD`、`REDIS_PASSWORD`、`RABBITMQ_PASSWORD`
- `ALLOWED_ORIGINS`

不要把 `.env`、API Key、密码或运行日志提交到 Git。

## 测试

```bash
cd backend && mvn test
cd frontend && pnpm test
cd frontend && pnpm build
```

数据库结构由 Flyway 管理，生产迁移文件位于 `backend/src/main/resources/db/migration/`。

## 项目结构

```text
backend/       Spring Boot API
frontend/      Vue 单页应用及 Nginx 配置
sandbox/       Go-Judge 运行镜像
docs/          架构与需求文档
dev-diary/     开发记录
scripts/       本地辅助脚本
```
