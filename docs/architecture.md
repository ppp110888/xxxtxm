# 系统架构设计文档

> **项目名称：** AI 伴学编程平台
> **版本：** v1.0.0
> **最后更新：** 2026-06-08

---

## 一、 系统分层架构

```
┌──────────────────────────────────────────────────────┐
│                    前端展示层 (Vue 3)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  │
│  │  管理端 SPA  │  │  学生端 SPA  │  │  Monaco IDE  │  │
│  │ (动态路由)   │  │  (动态路由)  │  │  编辑器组件   │  │
│  └──────┬───────┘  └──────┬──────┘  └──────┬───────┘  │
└─────────┼─────────────────┼────────────────┼──────────┘
          │    HTTP/HTTPS   │   WebSocket   │   SSE
          ▼                 ▼       ▲       ▲
┌──────────────────────────────────────────────────────┐
│                    网关层 (Nginx)                      │
│         路由分发 / SSL 终结 / 负载均衡                   │
└─────────────────────┬────────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
┌──────────────────┐   ┌──────────────────────┐
│  Spring Boot     │   │   Spring Boot         │
│  主业务服务       │   │   判题消费者服务        │
│  (Port 8080)     │   │   (Port 8081)         │
│                  │   │                       │
│  · 用户认证      │   │   · 拉取判题任务        │
│  · 知识树 CRUD   │   │   · Go-Judge 通信      │
│  · 题目管理      │   │   · 结果比对           │
│  · 用例管理      │   │   · AI 诊断触发        │
│  · 提交记录      │   │   · WebSocket 推送     │
│  · SSE AI 接口   │   │                       │
└───┬──────┬───────┘   └───────┬───────────────┘
    │      │                   │
    ▼      ▼                   ▼
┌────────┐ ┌──────────┐ ┌──────────────┐
│ MySQL  │ │  Redis   │ │  RabbitMQ    │
│ (主库) │ │ (缓存)   │ │  (判题队列)   │
└────────┘ └──────────┘ └──────┬───────┘
                               │
                               ▼
                       ┌────────────────┐
                       │   Go-Judge     │
                       │   判题沙箱集群  │
                       │  (Port 5050)   │
                       │                │
                       │  cgroup 隔离   │
                       │  namespace 隔离 │
                       │  CPU/内存 熔断  │
                       └────────────────┘
```

---

## 二、 核心流程设计

### 2.1 判题流程（核心链路）

```
用户提交代码
    │
    ▼
┌────────────────────┐
│ POST /api/v1       │
│ /student/submit    │
│                    │
│ body: {            │
│   questionId,      │
│   language,        │
│   code             │
│ }                  │
└────────┬───────────┘
         │ 1. 保存提交记录 (status=PENDING)
         │ 2. 投递消息到 RabbitMQ
         │ 3. 返回 { submissionId, status: "PENDING" }
         ▼
    ┌──────────────┐
    │  RabbitMQ    │──────► 判题消费集群 自动拉取
    │  Queue       │         (根据自身算力平滑消费)
    └──────────────┘
              │
              ▼
    ┌──────────────────┐
    │  判题消费者服务    │
    │                   │
    │ 1. 从 MQ 拿到任务  │
    │ 2. 从 DB 查隐藏用例│
    │ 3. 组装请求 →     │
    │    POST Go-Judge  │
    │ 4. 获取运行结果    │
    │ 5. 比对 stdout vs │
    │    预期输出        │
    │ 6. 更新提交记录    │
    │ 7. 如果是 RE →   │
    │    触发 AI 诊断    │
    │ 8. 通过 WebSocket │
    │    推送最终结果    │
    └──────────────────┘
              │
              ▼
    ┌──────────────────┐
    │  WebSocket 推送   │
    │  到学生浏览器      │
    │                   │
    │  { submissionId,  │
    │    status: "AC",  │
    │    timeUsed: 45,  │
    │    memoryUsed: 12 │
    │  }                │
    └──────────────────┘
```

### 2.2 AI 诊断流程

```
判题消费者检测到 RE (Runtime Error)
    │
    ▼
┌─────────────────────────────┐
│ 组装 Prompt:                 │
│   - 题目背景描述             │
│   - 用户提交的源码           │
│   - 沙箱返回的 stderr        │
│   - 系统提示："你是编程导师"  │
└──────────┬──────────────────┘
           │
           ▼
┌──────────────────────────┐
│ POST 第三方大模型 API      │
│ (DeepSeek / 通义千问)     │
│ stream: true              │
└──────────┬───────────────┘
           │ SSE 流式响应
           ▼
┌──────────────────────────┐
│ 后端 SSE 中转:             │
│ GET /api/v1/student/      │
│     ai-diagnosis/{subId}  │
│                           │
│ Content-Type: text/       │
│   event-stream            │
└──────────┬───────────────┘
           │ SSE
           ▼
┌──────────────────────────┐
│ 前端 AiDiagnosisPanel     │
│ - EventSource 接收        │
│ - 打字机效果渲染          │
│ - 流式完成后展示完整内容   │
└──────────────────────────┘
```

---

## 三、 模块划分

### 3.1 后端模块

```
backend/
├── src/main/java/com/xxxtxm/
│   ├── common/              # 公共模块
│   │   ├── config/          # 配置类（Sa-Token, MyBatis, CORS）
│   │   ├── exception/       # 全局异常处理
│   │   ├── result/          # 统一返回体 R<T>
│   │   └── utils/           # 工具类
│   │
│   ├── modules/
│   │   ├── auth/            # 认证模块
│   │   │   ├── controller/  # AuthController
│   │   │   ├── service/     # AuthService
│   │   │   └── mapper/      # UserMapper
│   │   │
│   │   ├── knowledge/       # 知识树模块
│   │   │   ├── controller/  # KnowledgeController
│   │   │   ├── service/     # KnowledgeService
│   │   │   ├── mapper/      # KnowledgeNodeMapper
│   │   │   └── entity/      # KnowledgeNode
│   │   │
│   │   ├── question/        # 题库模块
│   │   │   ├── controller/  # QuestionController
│   │   │   ├── service/     # QuestionService
│   │   │   ├── mapper/      # QuestionMapper
│   │   │   └── entity/      # Question, TestCase
│   │   │
│   │   ├── submission/      # 提交与判题模块
│   │   │   ├── controller/  # SubmissionController
│   │   │   ├── service/     # SubmissionService
│   │   │   ├── mapper/      # SubmissionMapper
│   │   │   ├── entity/      # Submission
│   │   │   └── judge/       # 判题客户端（Go-Judge HTTP Client）
│   │   │
│   │   └── progress/        # 用户进度模块
│   │       ├── controller/  # ProgressController
│   │       ├── service/     # ProgressService
│   │       └── mapper/      # UserProgressMapper
│   │
│   └── consumer/            # 判题消费者（独立启动 Profile）
│       ├── JudgeConsumer.java      # RabbitMQ 监听器
│       ├── GoJudgeClient.java      # Go-Judge HTTP 客户端
│       ├── ResultComparator.java   # 输出比对器
│       └── AiDiagnosisService.java # AI 诊断服务
```

### 3.2 前端模块

```
frontend/
├── src/
│   ├── api/                 # 接口请求封装
│   │   ├── auth.js
│   │   ├── knowledge.js
│   │   ├── question.js
│   │   ├── submission.js
│   │   └── progress.js
│   │
│   ├── stores/              # Pinia 状态管理
│   │   ├── user.js          # 用户信息 + Token + 角色
│   │   ├── permission.js    # 动态路由权限
│   │   └── websocket.js     # WebSocket 连接管理
│   │
│   ├── router/              # 路由配置
│   │   └── index.js         # 静态路由 + 动态路由 + 守卫
│   │
│   ├── views/               # 页面组件
│   │   ├── admin/           # 管理端页面
│   │   │   ├── KnowledgeTree.vue
│   │   │   ├── QuestionList.vue
│   │   │   └── CaseUpload.vue
│   │   └── student/         # 学生端页面
│   │       ├── SkillTree.vue
│   │       ├── LearningCabin.vue
│   │       ├── IdeWorkspace.vue
│   │       └── GrowthCenter.vue
│   │
│   ├── components/          # 公共组件
│   │   ├── MonacoEditor.vue
│   │   ├── MarkdownViewer.vue
│   │   ├── MarkdownEditor.vue
│   │   ├── SkillTreeNode.vue
│   │   ├── AiDiagnosisPanel.vue
│   │   └── StatusBadge.vue
│   │
│   └── utils/               # 工具函数
│       ├── request.js       # Axios 封装（拦截器 + Token 注入）
│       └── constants.js     # 判题状态枚举等常量
```

---

## 四、 数据库核心表设计

### 4.1 ER 关系图（核心表）

```
t_user (用户)
  │
  ├──< t_submission (提交记录)
  │     │
  │     └── t_question (题目)
  │           │
  │           ├──< t_test_case (测试用例)
  │           └──< t_question_knowledge >── t_knowledge_node (知识节点)
  │                                              │
  │                                              └── (自引用 parent_id)
  └──< t_user_progress (用户进度)
        │
        └── t_knowledge_node
```

### 4.2 核心建表语句（概要）

```sql
-- 用户表
CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT 'admin/student',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
);

-- 知识节点表
CREATE TABLE t_knowledge_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0 COMMENT '父节点ID，0=根节点',
    name VARCHAR(100) NOT NULL,
    markdown_content LONGTEXT COMMENT 'Markdown讲义内容',
    sort_order INT DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
);

-- 题目表
CREATE TABLE t_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    difficulty ENUM('easy','medium','hard') NOT NULL,
    language_limit VARCHAR(20) COMMENT 'python/java/all',
    time_limit_ms INT NOT NULL DEFAULT 1000,
    memory_limit_mb INT NOT NULL DEFAULT 256,
    description LONGTEXT,
    input_format TEXT,
    output_format TEXT,
    data_range TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
);

-- 测试用例表
CREATE TABLE t_test_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    input_file_path VARCHAR(500) NOT NULL,
    output_file_path VARCHAR(500) NOT NULL,
    is_visible TINYINT NOT NULL DEFAULT 0 COMMENT '0=隐藏 1=基础可见',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
);

-- 题—知识关联表
CREATE TABLE t_question_knowledge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    knowledge_id BIGINT NOT NULL,
    type ENUM('example','practice') NOT NULL COMMENT '经典例题/实战练习',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 提交记录表
CREATE TABLE t_submission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    language VARCHAR(20) NOT NULL,
    code LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/COMPILING/...',
    time_used INT COMMENT '运行耗时(ms)',
    memory_used INT COMMENT '内存使用(MB)',
    result_detail JSON COMMENT '判题详情(JSON)',
    error_message TEXT COMMENT 'stderr/错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
);

-- 用户进度表
CREATE TABLE t_user_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    knowledge_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'locked' COMMENT 'locked/in_progress/cleared',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_knowledge (user_id, knowledge_id)
);
```

---

## 五、 部署拓扑（生产环境参考）

```
                   ┌─────────────┐
                   │   CDN / DNS │
                   └──────┬──────┘
                          │
                          ▼
                  ┌───────────────┐
                  │  Nginx (443)  │
                  │  SSL + 反向代理│
                  └───┬───────┬───┘
                      │       │
          ┌───────────┘       └───────────┐
          ▼                               ▼
┌──────────────────┐           ┌──────────────────┐
│  Spring Boot ×2  │           │  Vue 3 静态资源   │
│  (主业务服务集群)  │           │  (Nginx 直接serve │
│  Port: 8080      │           │   或 OSS/CDN)     │
└────────┬─────────┘           └──────────────────┘
         │
    ┌────┴─────────────┐
    ▼                  ▼
┌────────┐    ┌──────────────┐
│ MySQL  │    │ Redis Cluster│
│ (主从) │    │ (哨兵模式)    │
└────────┘    └──────────────┘

┌──────────────────────────────────────────────┐
│              判题消费集群 (内网)                │
│  ┌─────────────────┐  ┌─────────────────┐    │
│  │ Consumer ×3     │  │ Consumer ×3     │    │
│  │ (Spring Boot)   │  │ (Spring Boot)   │    │
│  │ Port: 8081      │  │ Port: 8081      │    │
│  └────────┬────────┘  └────────┬────────┘    │
│           │                    │              │
│           └────────┬───────────┘              │
│                    ▼                          │
│           ┌──────────────┐                    │
│           │  RabbitMQ    │                    │
│           │  (集群/镜像)  │                    │
│           └──────────────┘                    │
└──────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────┐
│           Go-Judge 集群           │
│  ┌─────────┐ ┌─────────┐       │
│  │ Sandbox │ │ Sandbox │  ...  │
│  │ (cgroup)│ │ (cgroup)│       │
│  └─────────┘ └─────────┘       │
└──────────────────────────────────┘
```
