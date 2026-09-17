-- =============================================
-- CodeMate AI 伴学编程平台 — 数据库初始化脚本
-- 版本: v1.0.0
-- 日期: 2026-06-08
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS codemate
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
USE codemate;

-- =============================================
-- 1. 用户表
-- =============================================
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username    VARCHAR(50)     NOT NULL COMMENT '用户名',
    password    VARCHAR(255)    NOT NULL COMMENT '密码 (BCrypt)',
    role        VARCHAR(20)     NOT NULL DEFAULT 'student' COMMENT '角色: admin/student',
    nickname    VARCHAR(50)     DEFAULT NULL COMMENT '昵称',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常 1=已删除',
    UNIQUE KEY uk_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 2. 知识节点表（自引用树形结构）
-- =============================================
CREATE TABLE IF NOT EXISTS t_knowledge_node (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    parent_id        BIGINT       NOT NULL DEFAULT 0 COMMENT '父节点ID，0=根节点',
    name             VARCHAR(100) NOT NULL COMMENT '节点名称',
    markdown_content LONGTEXT     DEFAULT NULL COMMENT 'Markdown讲义内容',
    sort_order       INT          NOT NULL DEFAULT 0 COMMENT '排序字段',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识节点表';

-- =============================================
-- 3. 题目表
-- =============================================
CREATE TABLE IF NOT EXISTS t_question (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title          VARCHAR(200) NOT NULL COMMENT '题目标题',
    difficulty     VARCHAR(10)  NOT NULL DEFAULT 'easy' COMMENT '难度: easy/medium/hard',
    language_limit VARCHAR(20)  DEFAULT 'all' COMMENT '语言限制: python/java/all',
    time_limit_ms  INT          NOT NULL DEFAULT 1000 COMMENT '运行时间限制(ms)',
    memory_limit_mb INT         NOT NULL DEFAULT 256 COMMENT '内存限制(MB)',
    description    LONGTEXT     DEFAULT NULL COMMENT '题目描述(Markdown)',
    input_format   TEXT         DEFAULT NULL COMMENT '输入格式说明',
    output_format  TEXT         DEFAULT NULL COMMENT '输出格式说明',
    data_range        TEXT         DEFAULT NULL COMMENT '数据范围提示',
    reference_answer  LONGTEXT     DEFAULT NULL COMMENT '参考代码',
    explanation       LONGTEXT     DEFAULT NULL COMMENT '题目详解(Markdown)',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_difficulty (difficulty),
    INDEX idx_language_limit (language_limit),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- =============================================
-- 4. 测试用例表
-- =============================================
CREATE TABLE IF NOT EXISTS t_test_case (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    question_id      BIGINT       NOT NULL COMMENT '题目ID',
    input_file_path  VARCHAR(500) NOT NULL COMMENT '输入文件路径',
    output_file_path VARCHAR(500) NOT NULL COMMENT '输出文件路径',
    is_visible       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否可见: 0=隐藏判题 1=基础可见(Sample)',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例表';

-- =============================================
-- 5. 题—知识节点关联表
-- =============================================
CREATE TABLE IF NOT EXISTS t_question_knowledge (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    question_id  BIGINT      NOT NULL COMMENT '题目ID',
    knowledge_id BIGINT      NOT NULL COMMENT '知识节点ID',
    type         VARCHAR(20) NOT NULL DEFAULT 'practice' COMMENT '类型: example(经典例题)/practice(实战练习)',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_question_id (question_id),
    INDEX idx_knowledge_id (knowledge_id),
    UNIQUE KEY uk_q_k (question_id, knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题—知识关联表';

-- =============================================
-- 6. 提交记录表
-- =============================================
CREATE TABLE IF NOT EXISTS t_submission (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    question_id   BIGINT       NOT NULL COMMENT '题目ID',
    language      VARCHAR(20)  NOT NULL COMMENT '语言: python/java',
    code          LONGTEXT     NOT NULL COMMENT '源代码',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '判题状态',
    time_used     INT          DEFAULT NULL COMMENT '运行耗时(ms)',
    memory_used   INT          DEFAULT NULL COMMENT '内存使用(KB)',
    result_detail JSON         DEFAULT NULL COMMENT '判题详情(JSON)',
    error_message TEXT         DEFAULT NULL COMMENT 'stderr/错误信息',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_question_id (question_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交记录表';

-- =============================================
-- 7. 用户进度表
-- =============================================
CREATE TABLE IF NOT EXISTS t_user_progress (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id      BIGINT      NOT NULL COMMENT '用户ID',
    knowledge_id BIGINT      NOT NULL COMMENT '知识节点ID',
    status       VARCHAR(20) NOT NULL DEFAULT 'locked' COMMENT '状态: locked/in_progress/cleared',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_knowledge (user_id, knowledge_id),
    INDEX idx_user_id (user_id),
    INDEX idx_knowledge_id (knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户学习进度表';

-- =============================================
-- 种子用户由 DataInitializer.java 程序化创建 (BCrypt)
-- 默认账号: admin/admin123、student/student123
-- =============================================
