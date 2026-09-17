CREATE TABLE t_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'student',
    nickname VARCHAR(50) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_knowledge_node (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    markdown_content LONGTEXT DEFAULT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_question (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    difficulty VARCHAR(10) NOT NULL DEFAULT 'easy',
    language_limit VARCHAR(20) DEFAULT 'all',
    time_limit_ms INT NOT NULL DEFAULT 1000,
    memory_limit_mb INT NOT NULL DEFAULT 256,
    description LONGTEXT DEFAULT NULL,
    input_format TEXT DEFAULT NULL,
    output_format TEXT DEFAULT NULL,
    data_range TEXT DEFAULT NULL,
    reference_answer LONGTEXT DEFAULT NULL,
    explanation LONGTEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_difficulty (difficulty),
    INDEX idx_language_limit (language_limit),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_test_case (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    input_file_path VARCHAR(500) NOT NULL,
    output_file_path VARCHAR(500) NOT NULL,
    is_visible TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_question_knowledge (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    knowledge_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'practice',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_question_id (question_id),
    INDEX idx_knowledge_id (knowledge_id),
    UNIQUE KEY uk_q_k (question_id, knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_submission (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    language VARCHAR(20) NOT NULL,
    code LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    time_used INT DEFAULT NULL,
    memory_used INT DEFAULT NULL,
    result_detail JSON DEFAULT NULL,
    error_message TEXT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_question_id (question_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_user_progress (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    knowledge_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'locked',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_knowledge (user_id, knowledge_id),
    INDEX idx_user_id (user_id),
    INDEX idx_knowledge_id (knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
