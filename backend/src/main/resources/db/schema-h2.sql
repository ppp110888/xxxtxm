-- =============================================
-- CodeMate H2 开发数据库初始化 (MySQL 兼容模式)
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'student',
    nickname    VARCHAR(50)     DEFAULT NULL,
    create_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted  TINYINT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_username UNIQUE (username)
);

-- 知识节点表
CREATE TABLE IF NOT EXISTS t_knowledge_node (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    parent_id        BIGINT       NOT NULL DEFAULT 0,
    name             VARCHAR(100) NOT NULL,
    markdown_content CLOB         DEFAULT NULL,
    sort_order       INT          NOT NULL DEFAULT 0,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       TINYINT      NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_kn_parent ON t_knowledge_node(parent_id);

-- 题目表
CREATE TABLE IF NOT EXISTS t_question (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    difficulty     VARCHAR(10)  NOT NULL DEFAULT 'easy',
    language_limit VARCHAR(20)  DEFAULT 'all',
    time_limit_ms  INT          NOT NULL DEFAULT 1000,
    memory_limit_mb INT         NOT NULL DEFAULT 256,
    description    CLOB         DEFAULT NULL,
    input_format   TEXT         DEFAULT NULL,
    output_format  TEXT         DEFAULT NULL,
    data_range        TEXT         DEFAULT NULL,
    reference_answer  CLOB         DEFAULT NULL,
    explanation       CLOB         DEFAULT NULL,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted     TINYINT      NOT NULL DEFAULT 0
);

-- 测试用例表
CREATE TABLE IF NOT EXISTS t_test_case (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    question_id      BIGINT       NOT NULL,
    input_file_path  VARCHAR(500) NOT NULL,
    output_file_path VARCHAR(500) NOT NULL,
    is_visible       TINYINT      NOT NULL DEFAULT 0,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted       TINYINT      NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_tc_question ON t_test_case(question_id);

-- 题—知识关联表
CREATE TABLE IF NOT EXISTS t_question_knowledge (
    id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
    question_id  BIGINT      NOT NULL,
    knowledge_id BIGINT      NOT NULL,
    type         VARCHAR(20) NOT NULL DEFAULT 'practice',
    create_time  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_qk UNIQUE (question_id, knowledge_id)
);

-- 提交记录表
CREATE TABLE IF NOT EXISTS t_submission (
    id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    question_id   BIGINT      NOT NULL,
    language      VARCHAR(20) NOT NULL,
    code          CLOB        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    time_used     INT         DEFAULT NULL,
    memory_used   INT         DEFAULT NULL,
    result_detail CLOB        DEFAULT NULL,
    error_message TEXT        DEFAULT NULL,
    create_time   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted    TINYINT     NOT NULL DEFAULT 0
);

-- 用户进度表
CREATE TABLE IF NOT EXISTS t_user_progress (
    id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    knowledge_id BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'locked',
    create_time  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_knowledge UNIQUE (user_id, knowledge_id)
);
