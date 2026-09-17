# AI 伴学编程平台 — CLAUDE.md 项目开发指引

> **项目定位：** 面向零基础学习者的自由学习式编程实训平台（Java/Python），单体前端 + 动态权限菜单 + 高并发异步判题引擎 + AI 伴学。
> **当前状态：** ✅ 全部开发完成

---

## 一、 标准文件索引

| 文档 | 路径 | 说明 |
|------|------|------|
| **项目计划书** | [./计划书.md](./计划书.md) | 系统定位、核心业务流、技术亮点、迭代路线图、API 一览 |
| **架构设计文档** | [./docs/architecture.md](./docs/architecture.md) | 系统分层架构、模块划分、数据流图、部署拓扑 |
| **需求规格文档** | [./docs/requirements.md](./docs/requirements.md) | 完整功能需求清单、非功能需求、用户故事与验收标准 |
| **技术规范文档** | [./docs/tech-spec.md](./docs/tech-spec.md) | 技术栈版本、代码规范、API 设计规范、数据库规范、安全规范 |
| **设计规范文档** | [./docs/design-spec.md](./docs/design-spec.md) | UI/UX 设计原则、组件库使用规范、配色与布局、响应式策略 |
| **执行步骤文档** | [./docs/execution-plan.md](./docs/execution-plan.md) | 分阶段任务拆解、Milestone 节点、验收标准、风险预案 |
| **开发日志** | [./dev-diary/](./dev-diary/) | 每日开发记录（按 `YYYY-MM-DD.md` 命名） |

---

## 二、 核心模块速查

### 2.1 后端模块（6 个）

| 模块 | 包路径 | 核心职责 |
|------|--------|----------|
| **认证模块** | `modules.auth` | 注册/登录/登出、Sa-Token 鉴权、用户管理 |
| **知识树模块** | `modules.knowledge` | 无限层级知识图谱 CRUD、Markdown 讲义存储 |
| **题库模块** | `modules.question` | 题目 CRUD、测试用例上传/管理、题目-知识节点关联 |
| **判题模块** | `modules.submission` | 代码提交、沙箱执行（本地/Go-Judge）、WebSocket 推送、RabbitMQ 消费 |
| **进度模块** | `modules.progress` | 学习进度追踪、成长中心数据聚合、排行榜 |
| **AI 模块** | `modules.ai` | DeepSeek API 客户端、SSE 流式 AI 讲解/诊断/报告 |

### 2.2 前端页面（9 个）

| 页面 | 路由 | 说明 |
|------|------|------|
| **Login** | `/login` | 登录/注册入口 |
| **KnowledgeTree** | `/admin/knowledge` | 管理端：知识树 CRUD |
| **QuestionList** | `/admin/questions` | 管理端：题库管理 |
| **CaseUpload** | `/admin/cases` | 管理端：测试用例上传 |
| **UserList** | `/admin/users` | 管理端：用户管理 |
| **SkillTree** | `/student/skill-tree` | 学生端：技能树导航 |
| **LearningCabin** | `/student/learning/:nodeId` | 学生端：三阶学习舱 |
| **IdeWorkspace** | `/student/ide/:questionId` | 学生端：在线 IDE |
| **GrowthCenter** | `/student/growth` | 学生端：个人成长中心 |

---

## 三、 技术决策原则

- **后端优先 Spring Boot 生态**，避免引入过多第三方框架
- **前端优先 Vue 3 组合式 API**，统一使用 `<script setup>` 语法
- **判题安全第一**：用户代码不得在宿主机直接执行，生产环境必须通过 Go-Judge 沙箱
- **AI 交互必须走 SSE 流式**，严禁前端轮询大模型结果
- **自由学习模式**：所有知识节点和题目始终可访问，进度追踪不设关卡限制
- **多 Profile 自适应**：dev 模式使用 H2 + 本地判题 + 同步执行；prod 模式使用 MySQL + Go-Judge + RabbitMQ

---

## 四、 项目目录结构

```
xxxtxm/
├── CLAUDE.md                       # ← 本文件
├── 计划书.md                        # 项目总览计划书
├── docker-compose.yml              # 生产中间件编排
├── docs/                            # 标准规范文档
│   ├── architecture.md
│   ├── requirements.md
│   ├── tech-spec.md
│   ├── design-spec.md
│   └── execution-plan.md
├── dev-diary/                       # 开发日志
├── backend/                         # Spring Boot 后端
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/xxxtxm/
│       │   ├── CodeMateApplication.java
│       │   ├── common/              # result/, config/, exception/
│       │   └── modules/             # auth/, knowledge/, question/, submission/, progress/, ai/
│       ├── main/resources/          # application.yml, curriculum/
│       └── test/                    # 测试代码
├── frontend/                        # Vue 3 前端
│   └── src/
│       ├── router/                  # 路由 + 权限守卫
│       ├── layouts/                 # AdminLayout, StudentLayout
│       ├── views/                   # admin/, student/ 视图
│       ├── components/              # MonacoEditor
│       ├── composables/             # useAsyncData, useJudgeSocket, useAiStream
│       ├── stores/                  # Pinia user store
│       ├── api/                     # 6 个 API 模块
│       └── utils/                   # request, constants, markdown
└── sandbox/                         # Go-Judge 沙箱配置
```

---

## 五、 快速启动命令

### 5.1 零 Docker 开发模式（推荐开发时使用）

```bash
# 后端开发模式 (H2 文件数据库，无需 MySQL/Redis/RabbitMQ)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 前端开发模式
cd frontend && pnpm dev
```

### 5.2 Docker 全栈模式（验证中间件集成）

```bash
# 1. 启动所有中间件服务 (MySQL + Redis + RabbitMQ + Go-Judge)
docker compose up -d

# 2. 等待服务就绪 (约 30 秒)
docker compose ps

# 3. 启动后端 (默认连接 Docker 中的 MySQL/Redis/RabbitMQ)
cd backend && mvn spring-boot:run

# 4. 启动前端
cd frontend && pnpm dev

# 5. 停止中间件
docker compose down
```

### 5.3 服务端口速查

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端 API | http://localhost:8080 | Spring Boot REST API |
| Swagger 文档 | http://localhost:8080/doc.html | Knife4j 接口文档 |
| 前端 Dev | http://localhost:5173 | Vite 开发服务器 |
| H2 Console | http://localhost:8080/h2-console | 仅 dev 模式 |
| MySQL | localhost:3306 | root/root123 |
| Redis | localhost:6379 | 无密码 |
| RabbitMQ 管理 | http://localhost:15672 | guest/guest |
| Go-Judge REST | http://localhost:5050 | 判题沙箱 |

### 5.4 测试命令

```bash
# 后端测试
cd backend && mvn test

# 前端单元测试
cd frontend && pnpm test

# 前端 E2E 测试
cd frontend && pnpm test:e2e

# 前端生产构建
cd frontend && pnpm build
```

---

## 六、 数据库核心表

| 表名 | 实体类 | 说明 |
|------|--------|------|
| `t_user` | `User` | 用户（admin/student 角色，BCrypt 密码加密） |
| `t_knowledge_node` | `KnowledgeNode` | 知识树节点（无限层级，Markdown 讲义） |
| `t_question` | `Question` | 编程题目（难度/语言限制/时空限制） |
| `t_test_case` | `TestCase` | 测试用例（样本/隐藏两级可见性） |
| `t_question_knowledge` | `QuestionKnowledge` | 题目-知识节点关联（example/practice 类型） |
| `t_submission` | `Submission` | 代码提交记录（PENDING/AC/WA/TLE/MLE/RE/CE 状态） |
| `t_user_progress` | `UserProgress` | 学习进度追踪（in_progress/cleared 状态） |

---

> **最后更新：** 2026-06-10
> **当前阶段：** ✅ 三阶段全部完成 — 项目可正常运行
