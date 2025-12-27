CREATE DATABASE IF NOT EXISTS netdisk_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE netdisk_db;

-- ----------------------------
-- 1. 用户表 sys_user
-- ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username        VARCHAR(64) NOT NULL COMMENT '用户名',
    password        VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
    email           VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    role            VARCHAR(20) DEFAULT 'ROLE_USER' COMMENT '角色: ROLE_USER / ROLE_ADMIN',
    is_deleted      TINYINT DEFAULT 0 COMMENT '0=正常, 1=删除',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 2. 物理存储表 fs_storage (先建这个，因为 fs_node 要引用它)
-- ----------------------------
DROP TABLE IF EXISTS fs_storage;
CREATE TABLE fs_storage (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    real_path       VARCHAR(512) NOT NULL COMMENT '物理路径',
    file_hash       VARCHAR(64) NOT NULL COMMENT '文件Hash(SHA256)',
    file_size       BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_suffix     VARCHAR(16) DEFAULT NULL COMMENT '文件后缀',
    encrypt_key     VARCHAR(128) DEFAULT NULL COMMENT '加密密钥(若针对单文件加密)',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_hash (file_hash) -- 核心：保证同一个文件只存一份
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物理存储表';

-- ----------------------------
-- 3. 文件/目录表 fs_node (核心表)
-- ----------------------------
DROP TABLE IF EXISTS fs_node;
CREATE TABLE fs_node (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT NOT NULL COMMENT '所属用户ID',
    parent_id       BIGINT DEFAULT NULL COMMENT '父文件夹ID(根目录为NULL或0)',
    file_name       VARCHAR(255) NOT NULL COMMENT '文件名/文件夹名',
    is_dir          TINYINT(1) DEFAULT 0 COMMENT '是否目录: 1=是, 0=否',
    storage_id      BIGINT DEFAULT NULL COMMENT '指向物理存储ID(目录则为NULL)',
    file_size       BIGINT DEFAULT 0 COMMENT '展示大小',
    is_deleted      TINYINT DEFAULT 0 COMMENT '0=正常, 1=删除',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_parent (user_id, parent_id) -- 核心索引：加速“打开文件夹”的操作
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件节点表';

-- ----------------------------
-- 4. 审计日志表 fs_audit_log
-- ----------------------------
DROP TABLE IF EXISTS fs_audit_log;
CREATE TABLE fs_audit_log (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    operation       VARCHAR(50) NOT NULL COMMENT '操作类型(UPLOAD/DOWNLOAD/DELETE)',
    file_name       VARCHAR(255) DEFAULT NULL COMMENT '相关文件名',
    detail          VARCHAR(500) DEFAULT NULL COMMENT '详细信息',
    op_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';


-- 1. 插入管理员 (密码: 123456)
INSERT IGNORE INTO `sys_user` (`username`, `password`, `role`)
VALUES ('admin', '$2b$10$UvpcWokPJdlAgHXYfEX2fO8pIxL6VdN8hDuGSzeFXY2J1PbEunTdW', 'ROLE_ADMIN');

INSERT IGNORE INTO `sys_user` (`username`, `password`, `role`)
VALUES ('user', '$2b$10$UvpcWokPJdlAgHXYfEX2fO8pIxL6VdN8hDuGSzeFXY2J1PbEunTdW', 'ROLE_USER');


-- 2. 插入根目录 (注意：有些设计需要显式根目录，有些不需要。建议给用户1初始化一个根目录)
-- 这一步很重要：很多逻辑是基于“ParentID”查找的，顶层文件需要一个 ParentID=0 或 NULL 的逻辑
-- 这里假设 parent_id=0 是根，这条数据代表 "我的网盘" 根文件夹
INSERT INTO fs_node (id, user_id, parent_id, file_name, is_dir, create_time)
VALUES (1, 1, 0, 'root', 1, NOW());

-- 3. 在根目录下创建一个 "我的作业" 文件夹
INSERT INTO fs_node (user_id, parent_id, file_name, is_dir, create_time)
VALUES (1, 1, '我的作业', 1, NOW());