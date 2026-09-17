# 技术规范文档

> **项目名称：** AI 伴学编程平台
> **版本：** v1.0.0
> **最后更新：** 2026-06-08

---

## 一、 技术栈版本约束

### 1.1 前端

| 依赖 | 版本 | 说明 |
|------|------|------|
| Vue | ^3.4 | 渐进式前端框架 |
| Vite | ^5.0 | 构建工具 |
| Vue Router | ^4.3 | 官方路由管理器 |
| Pinia | ^2.1 | 轻量级状态管理 |
| Element Plus | ^2.5 | 桌面端 UI 组件库 |
| Monaco Editor | ^0.45 | VS Code 内核编辑器 |
| Axios | ^1.6 | HTTP 客户端 |
| dayjs | ^1.11 | 日期处理 |

### 1.2 后端

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 运行环境 |
| Spring Boot | 3.2.x | 主框架 |
| MyBatis-Plus | 3.5.5+ | ORM 增强 |
| Sa-Token | 1.38+ | 轻量级权限认证 |
| MySQL Connector | 8.0+ | 数据库驱动 |
| Druid | 1.2+ | 数据库连接池 |
| Lombok | 1.18+ | 代码简化 |
| Hutool | 5.8+ | Java 工具集 |

### 1.3 中间件

| 中间件 | 版本 | 用途 |
|--------|------|------|
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.2+ | 缓存 + 会话管理 |
| RabbitMQ | 3.12+ | 判题任务消息队列 |
| Go-Judge | latest | 代码执行沙箱 |

---

## 二、 代码规范

### 2.1 Java 后端规范

#### 命名约定
```java
// 包名：全小写，点分隔
com.xxxtxm.controller
com.xxxtxm.service
com.xxxtxm.mapper

// 类名：大驼峰
public class KnowledgeNodeService {}

// 方法名：小驼峰
public List<KnowledgeNode> getTreeByParentId(Long parentId) {}

// 常量：全大写，下划线分隔
public static final int MAX_RETRY_COUNT = 3;
```

#### 接口规范
```java
// 统一返回体
@Data
public class R<T> {
    private Integer code;   // 200=成功, 其他=错误码
    private String message;
    private T data;

    public static <T> R<T> ok(T data) { ... }
    public static <T> R<T> fail(String message) { ... }
}

// Controller 示例
@RestController
@RequestMapping("/api/v1/admin/knowledge")
public class KnowledgeController {

    @SaCheckRole("admin")
    @GetMapping("/tree")
    public R<List<KnowledgeNodeVO>> getTree() { ... }
}
```

#### 分层架构约束
```
Controller → Service → Mapper → DB
   ↓           ↓
  校验      业务逻辑
  参数      事务管理
```
- **Controller 层：** 仅做参数校验与路由转发，不得包含业务逻辑。
- **Service 层：** 承载核心业务，使用 `@Transactional` 管理事务。
- **Mapper 层：** 仅做数据库操作，复杂查询使用 XML 自定义 SQL。

### 2.2 Vue 3 前端规范

#### 文件命名
```
views/           # 页面组件：大驼峰
  admin/
    KnowledgeTree.vue
    QuestionList.vue
  student/
    SkillTree.vue
    IdeWorkspace.vue

components/      # 公共组件：大驼峰
  MonacoEditor.vue
  AiDiagnosisPanel.vue
  StatusBadge.vue

stores/          # 状态管理：小写
  user.js
  question.js

api/             # 接口封装：小写
  knowledge.js
  submission.js
```

#### 组件编写规范
```vue
<script setup lang="ts">
// 1. 导入
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

// 2. Props & Emits
const props = defineProps<{ nodeId: number }>()
const emit = defineEmits<{ select: [id: number] }>()

// 3. 响应式数据
const loading = ref(false)

// 4. 计算属性
const isLocked = computed(() => ...)

// 5. 方法
async function fetchData() { ... }

// 6. 生命周期
onMounted(() => fetchData())
</script>
```

#### 动态路由规范
```typescript
// router/index.ts
// 静态路由（所有人可用）
const constantRoutes = [
  { path: '/login', component: () => import('@/views/Login.vue') },
  { path: '/404', component: () => import('@/views/NotFound.vue') },
]

// 动态路由（按需加载）
const asyncRoutes = {
  admin: [ /* 管理端路由 */ ],
  student: [ /* 学生端路由 */ ],
}

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const store = useUserStore()
  if (!store.token) {
    // 未登录 → 跳转登录
    if (to.path === '/login') next()
    else next('/login')
    return
  }
  // 已登录 → 动态添加角色路由
  if (!store.routesLoaded) {
    const routes = await store.fetchUserRoutes()
    routes.forEach(r => router.addRoute(r))
    store.routesLoaded = true
    next({ ...to, replace: true })
    return
  }
  next()
})
```

---

## 三、 API 设计规范

### 3.1 URL 规范

```
基础路径: /api/v1

管理端:   /api/v1/admin/{resource}
学生端:   /api/v1/student/{resource}
公共:     /api/v1/common/{resource}
```

### 3.2 RESTful 风格

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/api/v1/admin/questions` | 分页查询列表 |
| GET | `/api/v1/admin/questions/{id}` | 查询单条详情 |
| POST | `/api/v1/admin/questions` | 新增 |
| PUT | `/api/v1/admin/questions/{id}` | 全量更新 |
| PATCH | `/api/v1/admin/questions/{id}` | 部分更新 |
| DELETE | `/api/v1/admin/questions/{id}` | 删除 |

### 3.3 统一响应格式

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 分页
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [ ... ],
    "total": 100,
    "page": 1,
    "pageSize": 20
  }
}

// 错误
{ "code": 40001, "message": "参数校验失败", "data": null }
```

### 3.4 错误码规范

| 区间 | 说明 |
|------|------|
| 200 | 成功 |
| 40000-40099 | 参数校验错误 |
| 40100-40199 | 认证/鉴权错误 |
| 40300-40399 | 权限不足 |
| 40400-40499 | 资源不存在 |
| 50000-50099 | 服务器内部错误 |
| 60000-60099 | 判题引擎错误 |

---

## 四、 数据库规范

### 4.1 命名规范

- **表名：** `t_` 前缀 + 小写下划线（如 `t_question`、`t_knowledge_node`）
- **字段名：** 小写下划线（如 `create_time`、`knowledge_id`）
- **索引名：** `idx_表名_字段名`（如 `idx_t_question_difficulty`）
- **唯一约束：** `uk_表名_字段名`（如 `uk_t_user_username`）

### 4.2 必备字段

每个业务表必须包含：
```sql
id          BIGINT PRIMARY KEY AUTO_INCREMENT,
create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
is_deleted  TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记'
```

### 4.3 MyBatis-Plus 配置

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: is_deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 开发环境
```

---

## 五、 安全规范

### 5.1 Sa-Token 配置

```java
// 配置示例
@Configuration
public class SaTokenConfig {
    @Bean
    public StpInterface stpInterface() {
        return new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                // 返回权限码列表
            }
            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                // 返回角色列表（admin/student）
            }
        };
    }
}
```

### 5.2 敏感数据保护

- 密码：BCrypt 加密，不得明文存储或日志打印
- Token：存储在 HttpOnly Cookie 或 Authorization Header
- 隐藏用例：数据库仅存文件路径，API 不得返回给学生端
- 日志脱敏：禁止在日志中输出用户密码、Token 明文

### 5.3 前端安全

- XSS 防护：禁止 `v-html` 直接渲染用户输入内容
- CSRF 防护：使用 Sa-Token 内置的 CSRF 防御机制
- 敏感路由：前端虽做路由过滤，但后端接口鉴权才是安全底线

---

## 六、 判题结果状态枚举

| 状态码 | 含义 | 前端展示 |
|--------|------|---------|
| PENDING | 排队中 | 橙色旋转图标 |
| COMPILING | 编译中 | 蓝色脉冲 |
| RUNNING | 运行中 | 蓝色脉冲 |
| JUDGING | 判题中 | 蓝色脉冲 |
| AC | Accepted / 通过 | 绿色对勾 |
| WA | Wrong Answer / 答案错误 | 红色叉号 |
| TLE | Time Limit Exceeded / 超时 | 黄色时钟 |
| MLE | Memory Limit Exceeded / 内存超限 | 黄色内存图标 |
| RE | Runtime Error / 运行错误 | 红色炸弹（触发 AI 诊断） |
| CE | Compile Error / 编译错误 | 紫色警告 |
| SE | System Error / 系统错误 | 灰色感叹号 |
