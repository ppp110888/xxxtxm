# CodeMate — AI 伴学编程平台 详细项目介绍

> **版本：** v1.0.0 | **完成日期：** 2026-06-10 | **开发周期：** 3 天（敏捷迭代）

---

## 一、项目概述

CodeMate 是一个面向 **零基础学习者** 的在线编程实训平台，专注于 **Java** 和 **Python** 双语言教学。系统提供从知识学习、例题研读、在线编码到 AI 辅导的完整学习闭环，是一个功能完备的全栈 Web 应用。

### 1.1 核心理念

传统编程教学平台通常采用"闯关制"——学完当前节点才能解锁下一节点。CodeMate 采用 **自由学习模式**：所有学习内容始终开放，学习者按自己的节奏自由探索，系统默默追踪进度但不设关卡限制。这降低了入门门槛，让学习者可以跳过已掌握的章节，专注攻克难点。

### 1.2 用户角色

| 角色 | 视角 | 核心操作 |
|------|------|----------|
| **管理员/教师** | 管理端 | 构建知识图谱、录入题目、上传测试用例、管理用户 |
| **学生** | 学生端 | 阅读讲义、研读例题、在线编码、查看数据画像、获取 AI 辅导 |

---

## 二、系统架构

```
                         ┌──────────────────────────────────────────┐
                         │          前端 SPA (Vue 3 + Vite)          │
                         │                                          │
                         │  ┌──────────────────┐  ┌──────────────┐ │
                         │  │   管理端 (Admin)  │  │ 学生端 (Stu)  │ │
                         │  │  ┌────────────┐  │  │ ┌──────────┐ │ │
                         │  │  │ 知识树管理  │  │  │ │ 技能树导航│ │ │
                         │  │  │ 题库管理   │  │  │ │ 三阶学习舱│ │ │
                         │  │  │ 用例上传   │  │  │ │ 在线IDE   │ │ │
                         │  │  │ 用户管理   │  │  │ │ 成长中心  │ │ │
                         │  │  └────────────┘  │  │ └──────────┘ │ │
                         │  └──────────────────┘  └──────────────┘ │
                         └──────────┬───────────────┬───────────────┘
                                    │ HTTP REST     │ SSE / WebSocket
                                    ▼               ▼
                         ┌──────────────────────────────────────────┐
                         │         Spring Boot 3.2 后端 API          │
                         │                                          │
                         │  ┌─────────┐ ┌────────┐ ┌────────────┐  │
                         │  │ Auth    │ │Knowledge│ │Submission  │  │
                         │  │ Sa-Token│ │知识树   │ │判题引擎     │  │
                         │  │ RBAC    │ │题库     │ │WS推送/MQ消费│  │
                         │  └─────────┘ └────────┘ └────────────┘  │
                         │  ┌─────────┐ ┌────────┐ ┌────────────┐  │
                         │  │Progress │ │  AI    │ │  Common    │  │
                         │  │学习进度 │ │DeepSeek│ │ 全局异常    │  │
                         │  │成长中心 │ │SSE流式 │ │ 统一响应    │  │
                         │  └─────────┘ └────────┘ └────────────┘  │
                         └──────┬────────┬──────────┬───────────────┘
                                │        │          │
                                ▼        ▼          ▼
                         ┌──────────────────────────────────────┐
                         │             数据 & 中间件层            │
                         │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ │
                         │  │MySQL │ │Redis │ │Rabbit│ │Go-   │ │
                         │  │持久化│ │缓存  │ │MQ    │ │Judge │ │
                         │  └──────┘ └──────┘ └──────┘ └──────┘ │
                         └──────────────────────────────────────┘
```

### 2.1 分层说明

| 层级 | 技术 | 职责 |
|------|------|------|
| **前端展示层** | Vue 3 + Element Plus + Monaco Editor | 页面渲染、用户交互、SSE/WS 消费 |
| **后端服务层** | Spring Boot 3.2 + MyBatis-Plus | REST API、业务逻辑、判题编排、AI 调用 |
| **权限层** | Sa-Token | Token 签发/校验、角色鉴权（`@SaCheckRole`）、接口拦截 |
| **缓存层** | Redis（prod 模式） | 知识树/题目/进度缓存，降低数据库压力 |
| **消息层** | RabbitMQ（prod 模式） | 判题任务异步化，削峰解耦 |
| **判题层** | Go-Judge / ProcessBuilder | 安全沙箱代码执行，资源限制 + 输出比对 |
| **数据层** | MySQL / H2 | 数据持久化（dev 模式自动切换 H2） |

### 2.2 双模式自适应

系统根据 Spring Profile 自动切换运行模式：

| 维度 | dev 模式 | prod 模式 |
|------|----------|-----------|
| 数据库 | H2 File (./data/h2db) | MySQL 8.0 |
| 缓存 | 无 (直接查库) | Redis 7.0 |
| 消息队列 | 无 (同步判题) | RabbitMQ 3.12 |
| 判题执行 | ProcessBuilder 本地进程 | Go-Judge 沙箱 |
| AI API | 开发 Key | 生产 Key |
| 优势 | 零依赖快速启动 | 高可用生产就绪 |

---

## 三、核心功能详解

### 3.1 认证鉴权体系

- **注册/登录**：用户名 + 密码（BCrypt 加密存储），注册时选择角色
- **Token 认证**：Sa-Token 签发，7 天有效期，30 分钟无操作自动续期
- **角色隔离**：`admin` / `student` 两种角色，`@SaCheckRole` 注解控制接口访问
- **前端路由守卫**：`router.beforeEach` 拦截未登录请求，按角色动态注入路由表
- **WebSocket 鉴权**：握手时通过 `?token=xxx` 查询参数传递 Token

### 3.2 知识树引擎

**管理端操作：**
- 无限层级树形结构（课程 → 章节 → 小节 → …）
- Markdown 格式讲义编辑（支持代码块、表格、图片）
- 拖拽排序，级联删除子节点
- Java + Python 双轨独立课程体系（70+ 篇预备讲义）

**学生端展示：**
- 卡片式章节展示，每个卡片显示进度状态
- 学习中（蓝色图标）/ 已完成（绿色图标）双状态
- 点击进入三阶学习舱

### 3.3 三阶学习舱

每个知识节点提供三段式学习体验：

```
Step 0: 知识吸收        Step 1: 经典例题         Step 2: 实战练习
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│ Markdown 讲义│  →   │ 参考代码 +   │  →   │ 编程练习题   │
│ + AI 讲解    │      │ 逐行解释     │      │ 跳转 IDE     │
│ + 子节点导航  │      │              │      │ + 提交状态   │
└─────────────┘      └─────────────┘      └─────────────┘
```

### 3.4 在线 IDE + 判题引擎

**编辑器特性：**
- Monaco Editor（VS Code 内核），支持 Python/Java 语法高亮
- 22 个代码片段自动补全（def main、class、public static void main 等）
- 深色/浅色主题切换

**双模式执行：**

| 模式 | 接口 | 特点 |
|------|------|------|
| 🔧 自测运行 | POST /api/v1/student/run | 自定义输入 → 查看输出，不记录成绩 |
| 🚀 提交判题 | POST /api/v1/student/submit | 完整测试用例比对 → 记录成绩 + WebSocket 推送 |

**判题流程（同步模式）：**
```
提交代码 → 保存 Submission(PENDING) → 编译代码 → 逐个运行测试用例
→ 比对输出（行尾空格标准化） → 更新状态 → WebSocket 推送结果
→ 若 AC → 自动标记对应知识节点为已完成
```

**判题状态：**

| 状态 | 含义 | 触发条件 |
|------|------|----------|
| PENDING | 等待判题 | 初始状态 |
| JUDGING | 判题中 | 正在执行 |
| AC | 通过 | 所有测试用例输出完全匹配 |
| WA | 答案错误 | 输出与预期不符 |
| TLE | 超时 | 执行时间超过题目限制 |
| MLE | 超内存 | 内存使用超过题目限制 |
| RE | 运行错误 | 代码抛出未捕获异常 |
| CE | 编译错误 | javac/python 语法错误 |
| SE | 系统错误 | 沙箱异常 |

**实时反馈：**
- WebSocket 连接 `/ws/judge?token=xxx`
- judge_result JSON 实时推送
- 前端 useJudgeSocket composable：心跳保活 + 指数退避自动重连

### 3.5 学习进度追踪

- **自动初始化**：用户首次访问时，自动为所有课程轨道创建进度记录
- **根节点标记**：课程根节点初始状态为 `cleared`（已完成）
- **子节点标记**：所有子节点初始状态为 `in_progress`（学习中），自由访问
- **AC 自动完成**：当某知识节点下所有练习题均被 AC 后，自动标记节点为 `cleared`
- **幂等设计**：重复初始化不会产生重复记录

### 3.6 个人成长中心

**数据画像卡片：**
- 总提交次数、AC 通过次数、AC 率
- 最常用编程语言偏好
- 已通关知识节点数 / 学习中节点数

**贡献热力图（CSS Grid）：**
- 最近 20 周每日提交计数可视化
- 类似 GitHub 贡献图的深浅色块
- 周末/工作日自动区分

**三个排行榜：**
1. **错题排行榜**：哪些题目最多人做错（不同用户统计）
2. **通过率排行榜**：哪些题目通过率最低（最难）
3. **通关数量榜**：哪些用户通过的题目最多

**错题本：**
- 所有非 AC/PENDING/JUDGING 提交，按时间倒序
- 支持按 WA/TLE/RE/CE 等状态筛选
- 显示每题耗时、内存、提交时间

**通关列表：**
- 已 AC 题目一览，含最早通关时间
- 点击可查看题目详情

### 3.7 AI 私教（三大 SSE 流式服务）

**AI 知识点讲解：**
- 在学习舱知识吸收阶段，点击「🤖 AI 讲解」按钮
- 系统读取当前知识节点的 Markdown 讲义，构建生活化类比 Prompt
- DeepSeek 大模型流式返回通俗易懂的解释
- 前端以打字机效果逐字渲染 Markdown

**AI 代码纠错诊断：**
- 在 IDE 中，当提交结果为 WA/TLE/RE/CE 时，出现「🤖 AI 诊断」按钮
- 系统加载提交代码 + 错误信息 + 题目要求，构建代码纠错 Prompt
- 流式返回：错误原因分析 → 修复建议 → 相关知识点
- 前端浮层面板展示，Markdown 格式化渲染

**AI 学情报告：**
- 在成长中心，点击「📋 AI 学情报告」按钮
- 系统聚合错题数据（按错误类型、知识点、时间分布），构建分析 Prompt
- 流式返回：薄弱点识别 → 学习建议 → 下一步推荐

**技术实现：**
```
前端 EventSource ─── SSE(stream) ───► Spring Boot SseEmitter
                                          │
                                          ▼
                                    AiService 构建 Prompt
                                          │
                                          ▼
                                    AiModelClient.chatStream()
                                          │
                                          ▼
                                    DeepSeek API (OpenAI 协议)
```

---

## 四、数据库设计

### 4.1 E-R 关系

```
t_user ──┬── t_submission ──── t_question ──┬── t_test_case
   │      │                    │              │
   │      └── (user_id)        ├── t_question_knowledge
   │                           │       │
   └── t_user_progress ────────┼───────┘
              │                │
              └── t_knowledge_node ◄──┘
```

### 4.2 表结构

**t_user — 用户表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR | 用户名，唯一 |
| password | VARCHAR | 密码，BCrypt 加密 |
| role | VARCHAR | admin / student |
| nickname | VARCHAR | 昵称（默认同用户名） |
| create_time | DATETIME | 自动填充 |
| update_time | DATETIME | 自动填充 |
| is_deleted | INT | 逻辑删除 @TableLogic |

**t_knowledge_node — 知识节点表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| parent_id | BIGINT | 父节点 ID，0=根节点 |
| name | VARCHAR | 节点名称 |
| markdown_content | TEXT | Markdown 讲义内容 |
| sort_order | INT | 排序序号 |
| create/update_time | DATETIME | 自动填充 |
| is_deleted | INT | 逻辑删除 |

**t_question — 题目表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| title | VARCHAR | 题目标题 |
| difficulty | VARCHAR | easy / medium / hard |
| language_limit | VARCHAR | python / java / all |
| time_limit_ms | INT | 时间限制（毫秒） |
| memory_limit_mb | INT | 内存限制（MB） |
| description | TEXT | 题目描述 (Markdown) |
| input_format | TEXT | 输入格式说明 |
| output_format | TEXT | 输出格式说明 |
| data_range | TEXT | 数据范围约束 |
| reference_answer | TEXT | 参考解答代码 |
| explanation | TEXT | 题目解析 |
| create/update_time | DATETIME | 自动填充 |
| is_deleted | INT | 逻辑删除 |

**t_test_case — 测试用例表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| question_id | BIGINT | 关联题目 |
| input_file_path | VARCHAR | 输入文件路径 |
| output_file_path | VARCHAR | 期望输出文件路径 |
| is_visible | INT | 0=隐藏用例 / 1=样本用例 |
| create_time | DATETIME | 自动填充 |
| is_deleted | INT | 逻辑删除 |

**t_question_knowledge — 题目-知识关联表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| question_id | BIGINT | 关联题目 |
| knowledge_id | BIGINT | 关联知识节点 |
| type | VARCHAR | example（例题）/ practice（练习题） |
| create_time | DATETIME | 自动填充 |

**t_submission — 提交记录表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 提交用户 |
| question_id | BIGINT | 提交题目 |
| language | VARCHAR | 代码语言 |
| code | TEXT | 源代码 |
| status | VARCHAR | PENDING/AC/WA/TLE/MLE/RE/CE/SE |
| time_used | INT | 执行耗时（ms） |
| memory_used | INT | 内存使用（KB） |
| result_detail | TEXT | 详细结果 JSON |
| error_message | TEXT | 错误信息（stderr/compile error） |
| create_time | DATETIME | 提交时间 |
| is_deleted | INT | 逻辑删除 |

**t_user_progress — 学习进度表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 |
| knowledge_id | BIGINT | 知识节点 |
| status | VARCHAR | in_progress（学习中）/ cleared（已完成） |
| create_time | DATETIME | 自动填充 |
| update_time | DATETIME | 自动填充 |

---

## 五、API 设计规范

### 5.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

响应码体系：
| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 50000 | 服务器内部错误 |

### 5.2 完整接口清单（39 个端点）

#### 认证接口（4 个，无需鉴权）
```
POST /api/v1/auth/register    — 注册 {username, password, role}
POST /api/v1/auth/login       — 登录 {username, password} → token
GET  /api/v1/auth/me          — 获取当前用户信息
POST /api/v1/auth/logout      — 登出
```

#### 管理端接口（16 个，@SaCheckRole("admin")）
```
知识树管理:
  GET    /api/v1/admin/knowledge/tree       — 获取知识树
  POST   /api/v1/admin/knowledge            — 新增节点
  PUT    /api/v1/admin/knowledge/{id}       — 更新节点
  DELETE /api/v1/admin/knowledge/{id}       — 删除节点（级联子节点）

题库管理:
  GET    /api/v1/admin/questions            — 题目分页列表
  GET    /api/v1/admin/questions/{id}       — 题目详情
  POST   /api/v1/admin/questions            — 新增题目
  PUT    /api/v1/admin/questions/{id}       — 更新题目
  DELETE /api/v1/admin/questions/{id}       — 删除题目

测试用例管理:
  GET    /api/v1/admin/cases?questionId=     — 用例列表
  POST   /api/v1/admin/cases/upload          — 上传用例 (multipart)
  DELETE /api/v1/admin/cases/{id}            — 删除用例

用户管理:
  GET    /api/v1/admin/users                — 用户分页列表
  GET    /api/v1/admin/users/{id}           — 用户详情
  PUT    /api/v1/admin/users/{id}           — 编辑用户（昵称/角色）
  DELETE /api/v1/admin/users/{id}           — 删除用户（软删除）
```

#### 学生端接口（19 个，@SaCheckRole("student")）
```
知识学习:
  GET /api/v1/student/knowledge/tree        — 获取知识树（含进度状态）
  GET /api/v1/student/knowledge/{id}        — 获取节点详情（Markdown 讲义）

题库查看:
  GET /api/v1/student/questions             — 题目列表（支持 knowledgeId + type 筛选）
  GET /api/v1/student/questions/{id}        — 题目详情
  GET /api/v1/student/questions/{id}/cases  — 样本用例预览

代码判题:
  POST /api/v1/student/submit               — 提交代码判题
  POST /api/v1/student/run                  — 自测运行（不记录成绩）
  GET  /api/v1/student/submissions          — 我的提交列表
  GET  /api/v1/student/submissions/{id}     — 提交详情
  GET  /api/v1/student/submissions/best-status?questionIds= — 批量查询最佳状态

学习进度:
  GET /api/v1/student/progress              — 我的学习进度

成长中心:
  GET /api/v1/student/growth/stats          — 个人统计数据
  GET /api/v1/student/growth/wrong-answers  — 错题列表
  GET /api/v1/student/growth/heatmap?weeks= — 提交热力图数据
  GET /api/v1/student/growth/leaderboard/wrong-ranking      — 错题排行榜
  GET /api/v1/student/growth/leaderboard/pass-rate-ranking  — 通过率排行榜
  GET /api/v1/student/growth/leaderboard/passed-count-ranking — 通关数排行榜
  GET /api/v1/student/growth/cleared-questions — 我的通关列表

AI 私教（SSE 流式）:
  GET /api/v1/student/ai/explain/knowledge/{id}  — AI 知识点讲解
  GET /api/v1/student/ai/report                   — AI 学情报告
  GET /api/v1/student/ai/diagnose/{submissionId}  — AI 代码诊断
```

---

## 六、前端页面详解

### 6.1 管理端

| 页面 | 路由 | 核心组件 |
|------|------|----------|
| **知识树管理** | `/admin/knowledge` | El-Table 树形表格 + 节点编辑弹窗 + Markdown 编辑器 |
| **题库管理** | `/admin/questions` | 分页表格 + 筛选条件 + 题目创建/编辑弹窗（含所有字段） |
| **用例车间** | `/admin/cases` | 题目选择器 + 用例列表 + 文件上传表单 + 可见性开关 |
| **用户管理** | `/admin/users` | 分页表格 + 用户名/角色筛选 + 编辑弹窗 + 软删除确认 |

### 6.2 学生端

| 页面 | 路由 | 核心组件 |
|------|------|----------|
| **技能树** | `/student/skill-tree` | 章节卡片网格 + 进度状态图标 + 双轨切换 |
| **学习舱** | `/student/learning/:nodeId` | 三阶步骤条 + Markdown 讲义 + AI 讲解面板 + 例题代码 + 子节点导航 |
| **在线 IDE** | `/student/ide/:questionId` | 分栏布局 + Monaco Editor + 提交/自测按钮 + 输出面板 + AI 诊断浮层 |
| **成长中心** | `/student/growth` | 统计卡片 + 热力图 + 3 个榜单 + 错题本 + 通关列表 + AI 报告 |

### 6.3 可组合函数（Composables）

| 文件 | 功能 | 技术要点 |
|------|------|----------|
| `useAsyncData.js` | 通用异步数据加载 | 泛型 fetcher + 自动 loading/error 状态 |
| `useJudgeSocket.js` | WebSocket 判题结果监听 | 心跳 Ping/Pong + 指数退避重连 + 自动清理 |
| `useAiStream.js` | SSE AI 流式接收 | EventSource 封装 + 内容追加 + 连接生命周期管理 |

### 6.4 API 模块（6 个）

| 模块 | 包含函数 |
|------|----------|
| `auth.js` | login, register, getCurrentUser, logout |
| `knowledge.js` | getKnowledgeTree, createNode, updateNode, deleteNode, getStudentTree, getStudentNode |
| `question.js` | listQuestions, getQuestion, createQuestion, updateQuestion, deleteQuestion, getStudentQuestion, listByKnowledge, listCases, uploadCase, deleteCase |
| `submission.js` | submitCode, getMySubmissions, getDetail, getBestStatus |
| `progress.js` | getMyProgress, getGrowthStats, getWrongAnswers, getHeatmap, getWrongRanking, getPassRateRanking, getPassedCountRanking, getClearedQuestions |
| `user.js` | getUserPage, getUserDetail, updateUser, deleteUser |

---

## 七、技术亮点清单

### 7.1 安全

| 亮点 | 实现 |
|------|------|
| **接口级 RBAC** | Sa-Token `@SaCheckRole` 注解 + 全局拦截器，未授权自动返回 403 |
| **密码加密** | BCrypt 哈希，无明文存储 |
| **沙箱隔离** | Go-Judge cgroup namespace，CPU/内存硬限制，网络隔离 |
| **前端路由守卫** | `router.beforeEach` + Token 校验 + 角色动态路由注入 |
| **WebSocket 鉴权** | 握手阶段通过 query param 传递 Token 验证 |

### 7.2 性能

| 亮点 | 实现 |
|------|------|
| **Redis 多级缓存** | 知识树(2h) + 题目(30m) + 进度(5m)，降低 DB 查询 |
| **RabbitMQ 削峰** | 判题任务异步化，3-5 并发消费者，手动 ACK 防丢失 |
| **MyBatis-Plus 分页** | 物理分页 + 自动计数 |
| **前端构建优化** | Vite + Monaco Editor 代码分割 + 按需加载 |

### 7.3 用户体验

| 亮点 | 实现 |
|------|------|
| **WebSocket 实时反馈** | 代码提交后秒级推送判题结果，无需轮询 |
| **SSE 流式 AI** | 打字机效果，首 token < 2s，逐字渲染 Markdown |
| **Monaco Editor** | VS Code 同款编辑器 + 22 个代码片段自动补全 |
| **进度可视化** | 技能树状态图标 + 成长中心热力图 + 数据卡片 |
| **自由学习** | 无枷锁，全部内容始终开放，按自己的节奏学习 |

### 7.4 工程质量

| 亮点 | 实现 |
|------|------|
| **Profile 自适应** | dev(H2+同步) / prod(MySQL+MQ+Redis+沙箱) 一键切换 |
| **全局异常处理** | @RestControllerAdvice 统一拦截，Sa-Token 异常友好提示 |
| **自动填充** | MyBatis-Plus MetaObjectHandler 自动 fill createTime/updateTime |
| **逻辑删除** | @TableLogic 自动追加 is_deleted=0，数据不丢失 |
| **数据初始化** | CommandLineRunner + CurriculumSeeder 自动播种课程数据 |
| **后端测试** | JUnit 5 + @Nested 分组 + @DisplayName 中文标注 |
| **前端测试** | Vitest 单元测试 + Playwright E2E 测试 |

---

## 八、课程内容体系

### 8.1 Java 轨道

种子数据包含 30+ 知识节点，涵盖：
- Java 基础语法、变量与数据类型
- 运算符与表达式、控制流程
- 数组、字符串处理
- 面向对象编程（类与对象、继承、多态）
- 异常处理、集合框架、IO 流

配套 15+ 编程题目（含例题 + 练习题）

### 8.2 Python 轨道

种子数据包含 30+ 知识节点，涵盖：
- Python 基础语法、数据类型
- 字符串操作、列表与元组
- 字典与集合、控制流程
- 函数定义、模块与包
- 面向对象、文件操作、异常处理

配套 15+ 编程题目（含例题 + 练习题）

> 所有讲义以 Markdown 格式存储在 `backend/src/main/resources/curriculum/knowledge-content/` 目录下

---

## 九、部署与运维

### 9.1 开发环境（推荐）

```bash
# 终端 1 — 后端
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# → 自动创建 H2 数据库 + 播种课程数据
# → 启动在 http://localhost:8080

# 终端 2 — 前端
cd frontend
pnpm dev
# → 启动在 http://localhost:5173
```

### 9.2 生产环境

```bash
# 1. 启动中间件
docker compose up -d
# MySQL:3306, Redis:6379, RabbitMQ:5672, Go-Judge:5050

# 2. 配置 AI Key
# 编辑 backend/src/main/resources/application.yml
# codemate.ai.api-key: sk-xxx
# codemate.ai.api-url: https://api.deepseek.com/v1

# 3. 构建并启动
cd backend && mvn clean package -DskipTests
java -jar target/codemate-backend-1.0.0.jar

# 4. 构建前端
cd frontend && pnpm build
# 产出 dist/ 目录，部署到 Nginx
```

### 9.3 服务端口

| 服务 | 地址 | 凭证 |
|------|------|------|
| 后端 API | http://localhost:8080 | — |
| API 文档 | http://localhost:8080/doc.html | Knife4j |
| H2 Console | http://localhost:8080/h2-console | jdbc:h2:file:./data/h2db |
| 前端 | http://localhost:5173 | — |
| MySQL | localhost:3306 | root / root123 |
| Redis | localhost:6379 | 无密码 |
| RabbitMQ | http://localhost:15672 | guest / guest |
| Go-Judge | http://localhost:5050 | — |

---

## 十、默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 学生 | student | student123 |

---

## 十一、项目统计

| 指标 | 数值 |
|------|------|
| 后端 Java 类 | 40+ |
| 前端 Vue 组件 | 15+ |
| 数据库表 | 7 |
| API 端点 | 39 |
| 前端页面 | 9 |
| 知识讲义 | 70+ 篇 (Java + Python) |
| 编程题目 | 30+ 道 |
| AI 服务 | 3 个 SSE 端点 |
| 前端测试 | 40 个用例 (Vitest) |
| 判题模式 | 同步 (dev) / 异步 (prod) |
| 支持语言 | Java, Python |

---

> **文档版本：** v1.0.0
> **最后更新：** 2026-06-10
> **项目代号：** CodeMate — AI 伴学编程平台
