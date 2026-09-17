# 项目需求规格说明书

> **项目名称：** AI 伴学编程平台
> **版本：** v1.0.0
> **最后更新：** 2026-06-08

---

## 一、 项目概述

### 1.1 项目背景

面向零基础编程学习者，提供 Java 与 Python 双语言的闯关式实训平台。通过知识图谱导航、在线 IDE 实战、自动判题反馈与 AI 伴学诊断，打造「学—练—测—诊」完整学习闭环。

### 1.2 产品愿景

让每一个编程初学者都能在有温度的 AI 陪伴下，从零到一掌握编程技能。

### 1.3 目标用户

| 角色 | 描述 |
|------|------|
| **学生** | 零基础编程学习者，通过闯关式学习掌握 Java/Python |
| **管理员/教师** | 教学内容录入者，管理知识树、题库、测试用例 |
| **系统管理员** | 运维人员，监控判题引擎健康状态与系统性能 |

---

## 二、 功能性需求

### 2.1 用户认证与权限（FR-AUTH）

| ID | 需求描述 | 优先级 |
|----|---------|--------|
| FR-AUTH-01 | 用户可通过用户名+密码注册并登录系统 | P0 |
| FR-AUTH-02 | 登录成功后返回 Sa-Token 令牌，前端存储并携带于每次请求 | P0 |
| FR-AUTH-03 | 后端根据角色（student/admin）进行接口级鉴权，拒绝越权请求 | P0 |
| FR-AUTH-04 | 前端根据登录角色动态加载对应路由（学生视图 vs 管理视图） | P0 |
| FR-AUTH-05 | Token 过期后自动跳转登录页 | P1 |

### 2.2 管理端 — 知识树引擎（FR-KNOWLEDGE）

| ID | 需求描述 | 优先级 |
|----|---------|--------|
| FR-KNOWLEDGE-01 | 支持无限层级的树形知识图谱，含节点的增删改查 | P0 |
| FR-KNOWLEDGE-02 | 每个知识节点绑定一份 Markdown 讲义（支持代码高亮） | P0 |
| FR-KNOWLEDGE-03 | 每个知识节点可关联多道「经典例题」与「实战练习题」 | P0 |
| FR-KNOWLEDGE-04 | 支持树节点的拖拽排序与父子关系调整 | P2 |

### 2.3 管理端 — 题库中心（FR-QUESTIONS）

| ID | 需求描述 | 优先级 |
|----|---------|--------|
| FR-QUESTIONS-01 | 题目 CRUD：名称、难度（简单/中等/困难）、语言限制 | P0 |
| FR-QUESTIONS-02 | 题目内容编辑：描述、输入格式、输出格式、数据范围 | P0 |
| FR-QUESTIONS-03 | 针对每道题配置语言特定的运行时间阈值（ms）与内存上限（MB） | P0 |
| FR-QUESTIONS-04 | 题目列表支持按难度、语言、知识点筛选 | P1 |

### 2.4 管理端 — 测试用例管理（FR-CASES）

| ID | 需求描述 | 优先级 |
|----|---------|--------|
| FR-CASES-01 | 支持为每道题上传多个标准输入文件（`.in`）和标准输出文件（`.out`） | P0 |
| FR-CASES-02 | 测试用例区分为「基础可见」（学生可见 Sample）与「隐藏判题」（盲盒用例） | P0 |
| FR-CASES-03 | 隐藏用例对判题服务可见但对学生端不可见 | P0 |

### 2.5 学生端 — 技能树（FR-SKILLTREE）

| ID | 需求描述 | 优先级 |
|----|---------|--------|
| FR-SKILLTREE-01 | 以可视化图谱展示知识节点，含「学习中/已完成」进度状态 | P0 |
| FR-SKILLTREE-02 | 所有知识节点自由访问，不受前置条件限制 | P0 |
| FR-SKILLTREE-03 | 点击任意节点进入「三阶学习舱」 | P0 |

### 2.6 学生端 — 三阶学习舱（FR-LEARNING）

| ID | 需求描述 | 优先级 |
|----|---------|--------|
| FR-LEARNING-01 | 第一阶：沉浸式阅读节点 Markdown 教程 | P0 |
| FR-LEARNING-02 | 第二阶：展示例题的完整解题思路、复杂度分析与标准代码 | P0 |
| FR-LEARNING-03 | 第三阶：点击"去通关"跳转在线 IDE 进行实战 | P0 |

### 2.7 学生端 — 在线 IDE（FR-IDE）

| ID | 需求描述 | 优先级 |
|----|---------|--------|
| FR-IDE-01 | 左侧任务面板：题目描述、样例输入/输出、历史提交状态 | P0 |
| FR-IDE-02 | 右侧编码面板：Monaco Editor，支持 Java/Python 语言切换 | P0 |
| FR-IDE-03 | 自测模式（Run）：用户输入自定义数据，返回 stdout 输出 | P0 |
| FR-IDE-04 | 评测模式（Submit）：代码提交至后端沙箱，与全量隐藏用例比对 | P0 |
| FR-IDE-05 | 判题结果通过 WebSocket 实时推送至前端 | P0 |
| FR-IDE-06 | 展示判题详情：通过用例数/总用例数、耗时、内存 | P1 |

### 2.8 学生端 — 个人成长中心（FR-GROWTH）

| ID | 需求描述 | 优先级 |
|----|---------|--------|
| FR-GROWTH-01 | 可视化总做题数、AC 通过率、语言使用偏好 | P1 |
| FR-GROWTH-02 | 代码提交热力图（类似 GitHub 风格） | P1 |
| FR-GROWTH-03 | 智能错题本：自动收录非 AC 提交，按错误类型分类 | P1 |
| FR-GROWTH-04 | AI 学情诊断报告：大模型分析错题流水，生成个性化建议 | P2 |

### 2.9 AI 赋能层（FR-AI）

| ID | 需求描述 | 优先级 |
|----|---------|--------|
| FR-AI-01 | 捕获 Runtime Error 时拦截 stderr，组装 Prompt 调用大模型 | P2 |
| FR-AI-02 | 通过 SSE 协议流式返回 AI 纠错辅导内容（打字机效果） | P2 |
| FR-AI-03 | 前端设计「AI 诊断浮层」展示 AI 辅导结果 | P2 |

---

## 三、 非功能性需求

### 3.1 性能要求

| ID | 指标 | 目标值 |
|----|------|--------|
| NFR-PERF-01 | 判题接口响应时间（异步提交） | < 500ms 返回"排队中"状态 |
| NFR-PERF-02 | 单题判题完成时间 | < 10s（含沙箱执行+比对） |
| NFR-PERF-03 | WebSocket 推送延迟 | < 1s |
| NFR-PERF-04 | 页面首屏加载 | < 2s |
| NFR-PERF-05 | 并发判题支持 | ≥ 100 QPS |

### 3.2 安全要求

| ID | 指标 | 说明 |
|----|------|------|
| NFR-SEC-01 | 接口鉴权 | 所有 API 需经过 Sa-Token 角色验证 |
| NFR-SEC-02 | 沙箱隔离 | 用户代码仅在 Go-Judge 沙箱执行，宿主机不可见 |
| NFR-SEC-03 | 密码存储 | 使用 BCrypt 加密存储 |
| NFR-SEC-04 | SQL 注入防护 | 使用 MyBatis-Plus 参数化查询 |
| NFR-SEC-05 | XSS 防护 | 前端输出转义 + 后端输入校验 |

### 3.3 可用性要求

| ID | 指标 | 目标值 |
|----|------|--------|
| NFR-AVAIL-01 | 系统可用性 | ≥ 99.5% |
| NFR-AVAIL-02 | 判题引擎可用性 | ≥ 99.9%（独立部署+健康检查） |

---

## 四、 用户故事（关键场景）

### US-01：管理员录入知识树
> 作为一名管理员，我希望能创建多层级的 Java/Python 知识树，并为每个节点编写 Markdown 讲义、关联配套题目，以便学生能按照知识体系循序渐进地学习。

**验收标准：**
- [ ] 树形表格支持无限层级增删改
- [ ] Markdown 讲义编辑实时预览
- [ ] 已关联的题目在节点详情中可见

### US-02：学生浏览技能树
> 作为一名学生，我希望在技能树页面看到自己的学习进度，已完成的知识点显示绿色对勾，学习中显示蓝色标记，所有节点均可自由点击进入学习。

**验收标准：**
- [ ] 所有知识节点均可自由访问
- [ ] 已完成的节点标注绿色已完成标记
- [ ] 点击任意节点进入学习舱

### US-03：学生提交代码并获取判题结果
> 作为一名学生，我希望在在线 IDE 中编写 Python/Java 代码，点击提交后能实时看到判题结果（通过/错误/超时等），以便快速验证自己的代码是否正确。

**验收标准：**
- [ ] Monaco Editor 支持 Python/Java 语法高亮
- [ ] 提交后立即显示"排队中"
- [ ] WebSocket 实时推送判题状态（Compiling → Running → Judging → Accepted/Wrong Answer/...）
- [ ] 错误类型精确反馈（WA / TLE / RE / MLE / CE）

### US-04：学生查看 AI 纠错辅导
> 作为一名学生，当我提交代码出现 Runtime Error 时，希望能看到 AI 用通俗易懂的语言解释错误原因并给出修复建议，而不是面对晦涩的系统报错堆栈。

**验收标准：**
- [ ] RE 时自动触发 AI 诊断
- [ ] AI 回复以打字机效果流式展示
- [ ] 诊断内容包含错误原因 + 修复建议 + 相关知识点链接

---

## 五、 数据实体关系概览

```
User (用户)
  ├─ id, username, password, role(student/admin), create_time

KnowledgeNode (知识节点)
  ├─ id, parent_id, name, markdown_content, sort_order
  ├─ 自引用 parent_id → 树形结构

Question (题目)
  ├─ id, title, difficulty, language_limit, time_limit_ms, memory_limit_mb
  ├─ description, input_format, output_format, data_range

QuestionKnowledge (题—知识关联)
  ├─ question_id, knowledge_id, type(经典例题/实战练习)

TestCase (测试用例)
  ├─ id, question_id, input_file_path, output_file_path, is_visible(基础可见/隐藏)

Submission (提交记录)
  ├─ id, user_id, question_id, language, code, status, time_used, memory_used
  ├─ result_detail(JSON), create_time

UserProgress (用户进度)
  ├─ id, user_id, knowledge_id, status(in_progress/cleared)
```
