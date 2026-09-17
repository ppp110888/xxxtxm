# 项目执行步骤拆解

> **项目名称：** AI 伴学编程平台 (CodeMate)
> **版本：** v1.0.0
> **最后更新：** 2026-06-10
> **整体状态：** ✅ 三阶段全部完成

---

## 执行总览

```
阶段一（MVP）      阶段二（抗压进阶）      阶段三（AI 赋能）
  6-8 周             4-6 周                3-4 周
 ─────────●────────────────────●──────────────────────●─────────►
          │                    │                      │
      2026-06-08           2026-06-09             2026-06-10
     RBAC + 数据录入      沙箱 + 高可用改造         AI 私教赋能
         ✅                   ✅                      ✅
```

---

## 阶段一：RBAC 基建与数据录入（MVP 版本）✅ 已完成

> **完成日期：** 2026-06-08
> **产出：** 后端 + 前端工程骨架、认证鉴权体系、管理端知识树/题库 CRUD、学生端技能树

### Step 1.1 — 项目初始化与环境搭建 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| 创建 Spring Boot 工程 | 初始化 Maven 项目，配置依赖 | 后端工程骨架 | ✅ |
| 创建 Vue 3 工程 | Vite 创建项目，配置依赖 | 前端工程骨架 | ✅ |
| Docker 环境准备 | MySQL + Redis + RabbitMQ + Go-Judge 编排 | 开发环境可用 | ✅ |
| 数据库初始化 | DDL 建表 (7 张核心表) | 表结构就绪 | ✅ |

### Step 1.2 — 认证鉴权体系 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| Sa-Token 集成 | 引入依赖，配置 Token 策略 (7d 超时) | 鉴权框架就绪 | ✅ |
| 用户注册接口 | POST /api/v1/auth/register | 注册可用 | ✅ |
| 用户登录接口 | POST /api/v1/auth/login，返回 Token | 登录可用 | ✅ |
| 角色权限配置 | admin/student 角色定义与 @SaCheckRole 校验 | 角色体系就绪 | ✅ |
| 前端登录页 | 登录表单 + 注册弹窗 + Token 存储 | 登录页可用 | ✅ |

### Step 1.3 — 前端动态路由拦截器 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| 路由表设计 | 管理端 4 页面 + 学生端 5 页面 | 路由清单 | ✅ |
| 动态路由守卫 | router.beforeEach 鉴权 + 角色动态路由注入 | 路由拦截生效 | ✅ |
| 管理端布局 | 可折叠侧边栏 + 面包屑顶栏 + 用户菜单 | AdminLayout | ✅ |
| 学生端布局 | 顶栏导航 + Logo + 用户头像下拉 | StudentLayout | ✅ |
| 角色视图切换测试 | 管理/学生双账号隔离验证 | 切换正常 | ✅ |

### Step 1.4 — 管理端：知识树 CRUD ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| 知识节点后端接口 | 树形 CRUD（含级联删除子节点） | RESTful API | ✅ |
| Element Plus TreeTable | 前端树形表格组件 + 节点编辑弹窗 | KnowledgeTree 页面 | ✅ |
| Markdown 内容编辑 | 讲义内容录入 (Markdown 格式) | 讲义编辑可用 | ✅ |
| 课程内容初始化 | Java + Python 双轨 70+ 篇讲义 + JSON 课程结构 | CurriculumSeeder | ✅ |

### Step 1.5 — 管理端：题库与用例管理 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| 题目 CRUD 后端接口 | 含难度/语言限制/时空限制字段 | RESTful API | ✅ |
| 题目管理前端页面 | 分页列表 + 多条件筛选 + 创建/编辑弹窗 | QuestionList 页面 | ✅ |
| 测试用例上传接口 | Multipart 文件上传 + 可见性标记 (sample/hidden) | 用例上传可用 | ✅ |
| 测试用例管理页面 | 按题目查看用例列表 + 文件上传/删除 | CaseUpload 页面 | ✅ |
| 题目-知识节点关联 | 例题/练习题绑定到知识节点 | 关联表 CRUD | ✅ |
| 用户管理功能 | 用户分页 + 角色/昵称编辑 + 软删除 | UserList 页面 | ✅ |

### Step 1.6 — 学生端：技能树可视化 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| 知识树查询接口 | 含用户进度状态标记 (in_progress/cleared) | API 可用 | ✅ |
| 技能树可视化组件 | 章节卡片 + 进度状态图标 + 双轨选择 | SkillTree 页面 | ✅ |
| 学习舱框架 | 三阶步骤切换 (知识→例题→练习) | LearningCabin 页面 | ✅ |

### 阶段一验收结果 ✅

- ✅ 管理员可登录并访问管理端
- ✅ 可创建多层知识树 + 编写 Markdown 讲义
- ✅ 可录入题目并上传测试用例
- ✅ 学生登录后只能看到学生端界面
- ✅ 学生可查看技能树（含进度状态）
- ✅ 直接调用管理端 API 返回权限拒绝

---

## 阶段二：沙箱打通与高可用改造 ✅ 已完成

> **完成日期：** 2026-06-09
> **产出：** 在线 IDE、本地/Go-Judge 双模式判题、WebSocket 实时推送、RabbitMQ 异步化、Redis 缓存、成长中心

### Step 2.1 — Go-Judge 沙箱部署与对接 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| Go-Judge 部署 | Docker 部署 + docker-compose 编排 | 沙箱可用 | ✅ |
| GoJudgeClient 编写 | Java HTTP 客户端，file copyIn/copyOut | prod 模式沙箱通信 | ✅ |
| DevJudgeService | 开发模式使用 ProcessBuilder 本地执行 | dev 模式判题 | ✅ |
| Profile 自适应 | @Profile("dev") vs @Profile("!dev") 自动切换 | 双模式无缝切换 | ✅ |

### Step 2.2 — 判题链路核心 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| 提交代码接口 | POST /api/v1/student/submit (同步/异步自动切换) | 提交可用 | ✅ |
| 自测接口 | POST /api/v1/student/run (不记录成绩) | 自测可用 | ✅ |
| 结果比对逻辑 | stdout vs 预期输出 (行尾空格标准化) | 判题逻辑 | ✅ |
| 判题状态机 | PENDING→JUDGING→AC/WA/TLE/MLE/RE/CE/SE | 状态流转 | ✅ |
| 前端 IDE 页面 | Monaco Editor + 题目描述 + 输出面板 + AI 诊断 | IdeWorkspace 页面 | ✅ |

### Step 2.3 — RabbitMQ 异步化改造 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| RabbitMQ 配置 | DirectExchange + 持久化队列 + JSON 序列化 | MQ 配置 | ✅ |
| 生产者投递 | JudgeService 提交后投递至 MQ | 异步投递 | ✅ |
| 消费者接收 | @RabbitListener + 手动 ACK + 3-5 并发 | 消费链路 | ✅ |
| Profile 适配 | dev/test 模式排除 MQ，同步执行 | 开发友好 | ✅ |

### Step 2.4 — WebSocket 实时推送 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| WebSocket 服务端 | /ws/judge?token=xxx + Sa-Token 鉴权 | WS 服务可用 | ✅ |
| 连接管理 | userId → Session Map + Ping/Pong 心跳 | 连接稳定 | ✅ |
| 判题结果推送 | judge_result JSON 实时广播 | 实时反馈 | ✅ |
| 前端 WS 客户端 | useJudgeSocket composable + 指数退避重连 | 前端集成 | ✅ |

### Step 2.5 — Redis 缓存层 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| 知识树缓存 | 树结构 JSON 缓存 (TTL 2h) | 查询加速 | ✅ |
| 题目缓存 | 题目详情 Redis 缓存 (TTL 30m) | 查询加速 | ✅ |
| 进度缓存 | 用户进度缓存 (TTL 5m) | 查询加速 | ✅ |
| Profile 适配 | dev/test 模式排除 Redis，直接查库 | 开发友好 | ✅ |

### Step 2.6 — 个人成长中心 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| 数据画像接口 | 提交数/AC率/语言偏好/进度统计 | GrowthController.stats | ✅ |
| 提交热力图 | 每日提交计数 + CSS Grid 前端渲染 (20周) | Heatmap 组件 | ✅ |
| 错题本 | 非 AC 提交列表 + 状态筛选 | WrongAnswers 组件 | ✅ |
| 排行榜 | 错题榜/通过率榜/通关数榜 (3 个榜单) | Leaderboard 组件 | ✅ |
| 通关列表 | 已 AC 题目一览 + 最早通关时间 | ClearedQuestions 组件 | ✅ |
| 判题 AC → 自动标记 | JudgeService AC 后自动调用 progressService.markCleared | 学习闭环 | ✅ |

### 阶段二验收结果 ✅

- ✅ 学生在 IDE 中编写代码并成功提交
- ✅ 代码在沙箱中安全执行，超时/超内存自动终止
- ✅ 判题结果准确（输出比对 + 行尾空格标准化）
- ✅ WebSocket 实时推送判题结果
- ✅ 自由学习模式：所有节点可直接访问
- ✅ 热力图正确展示每日提交频次

---

## 阶段三：AI 私教赋能 ✅ 已完成

> **完成日期：** 2026-06-10
> **产出：** 3 个 AI SSE 流式接口、DeepSeek 对接、前端 AI 交互面板

### Step 3.1 — 大模型对接 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| 大模型 API 选型 | DeepSeek (兼容 OpenAI 协议) | 选定方案 | ✅ |
| Prompt 模板设计 | 3 套 System Prompt (诊断/报告/讲解) | Prompt 模板 | ✅ |
| AiModelClient | HTTP 客户端 + 流式 chatStream + 响应解析 | 通信层 | ✅ |
| AiProperties | @ConfigurationProperties 外部化 API Key/URL | 配置管理 | ✅ |

### Step 3.2 — AI 代码纠错诊断 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| diagnoseSubmission | 加载提交 + 题目 → 构建诊断 Prompt → SSE 流式返回 | AiService.diagnoseSubmission | ✅ |
| SSE 接口 | GET /api/v1/student/ai/diagnose/{submissionId} | AiController.diagnose | ✅ |
| 前端 AI 浮层 | useAiStream composable + EventSource + Markdown 渲染 | IdeWorkspace AI 面板 | ✅ |
| 流式异常处理 | 网络断开/用户关闭自动清理 EventSource | 健壮性 | ✅ |

### Step 3.3 — AI 知识点讲解 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| explainKnowledge | 加载知识节点内容 → 构建生活化类比 Prompt → SSE 流式 | AiService.explainKnowledge | ✅ |
| SSE 接口 | GET /api/v1/student/ai/explain/knowledge/{knowledgeId} | AiController.explainKnowledge | ✅ |
| 前端集成 | 学习舱内嵌 "AI 讲解" 按钮，侧边面板展示 | LearningCabin 页面 | ✅ |

### Step 3.4 — AI 学情报告 ✅

| 任务 | 描述 | 产出物 | 状态 |
|------|------|--------|------|
| generateReport | 聚合错题数据 → 分析薄弱点 → SSE 流式报告 | AiService.generateReport | ✅ |
| SSE 接口 | GET /api/v1/student/ai/report | AiController.report | ✅ |
| 前端集成 | 成长中心 "AI 学情报告" 按钮，流式渲染 | GrowthCenter 页面 | ✅ |

### 阶段三验收结果 ✅

- ✅ AI 诊断：分析代码错误原因 + 修复建议 + 关联知识点
- ✅ AI 讲解：生活化类比讲解知识概念
- ✅ AI 报告：基于错题数据生成个性化学习分析
- ✅ AI 回复以 SSE 流式展示 (打字机效果)
- ✅ SSE 连接断开时前端正确清理资源
- ✅ AI Key 可外部配置 (application.yml)

---

## 实际完成里程碑

```
M1 ─ 2026-06-08 ─ 项目骨架 + 登录鉴权 + 管理端 CRUD + 学生端技能树 (阶段一)
M2 ─ 2026-06-09 ─ 在线 IDE + 判题引擎 + WebSocket + 成长中心 (阶段二)
M3 ─ 2026-06-10 ─ AI 诊断/讲解/报告 + 自由学习模式 + Bug 修复 (阶段三 + 收尾)
```

---

## 风险预案回顾

| 风险 | 预案 | 实际情况 |
|------|------|----------|
| Go-Judge 沙箱逃逸 | Docker 安全策略 + 只读文件系统 | 已配置 Go-Judge 镜像 + dev 模式 ProcessBuilder fallback |
| RabbitMQ 消息堆积 | 消息 TTL + 按需扩容 | dev 模式排除 MQ，prod 使用手动 ACK + 3-5 并发消费者 |
| 大模型 API 不可用 | 降级：返回原始 stderr | AiModelClient 含超时 + 异常捕获 |
| WebSocket 连接泄漏 | 心跳 + 空闲超时 | Ping/Pong 30s 心跳 + 指数退避重连 (max 5 次) |
| 数据库慢查询 | 索引 + Redis 缓存 | MyBatis-Plus 分页 + Redis 缓存知识树/题目/进度 |
