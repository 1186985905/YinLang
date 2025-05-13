-- 创建用户表
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码(加密)',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `role` varchar(20) DEFAULT 'user' COMMENT '角色(user/admin)',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态(1:启用,0:禁用)',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建聊天会话表
CREATE TABLE `chat_sessions` (
  `id` varchar(36) NOT NULL COMMENT '会话ID',
  `title` varchar(100) NOT NULL COMMENT '会话标题',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `default_model_type` varchar(50) NOT NULL DEFAULT 'openai' COMMENT '默认模型类型',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_chat_sessions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- 创建消息表
CREATE TABLE `messages` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` varchar(36) NOT NULL COMMENT '会话ID',
  `role` varchar(20) NOT NULL COMMENT '角色(user/assistant)',
  `content` text NOT NULL COMMENT '消息内容',
  `model_type` varchar(50) DEFAULT NULL COMMENT '模型类型',
  `model_name` varchar(50) DEFAULT NULL COMMENT '模型名称',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  CONSTRAINT `fk_messages_session_id` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 创建图片表(图片实际存储在阿里云OSS)
CREATE TABLE `images` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `user_id` bigint(20) NOT NULL COMMENT '上传用户ID',
  `message_id` bigint(20) DEFAULT NULL COMMENT '关联消息ID',
  `filename` varchar(255) NOT NULL COMMENT '文件名',
  `oss_url` varchar(500) NOT NULL COMMENT '阿里云OSS URL',
  `content_type` varchar(100) NOT NULL COMMENT '内容类型',
  `size` bigint(20) NOT NULL COMMENT '文件大小(字节)',
  `width` int(11) DEFAULT NULL COMMENT '图片宽度',
  `height` int(11) DEFAULT NULL COMMENT '图片高度',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_message_id` (`message_id`),
  CONSTRAINT `fk_images_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_images_message_id` FOREIGN KEY (`message_id`) REFERENCES `messages` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片表';

-- 创建部门表
CREATE TABLE `departments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `name` varchar(100) NOT NULL COMMENT '部门名称',
  `description` varchar(255) DEFAULT NULL COMMENT '部门描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_department_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 在Users表中添加department_id字段，关联到部门表
ALTER TABLE `users` ADD COLUMN `department_id` bigint(20) DEFAULT NULL COMMENT '部门ID' AFTER `role`;
ALTER TABLE `users` ADD CONSTRAINT `fk_users_department_id` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`);

-- 插入指定的7个部门
INSERT INTO `departments` (`name`, `description`) VALUES 
('新媒体运营', '负责社交媒体、内容创作和在线推广'),
('电商部', '负责电子商务平台运营和销售'),
('行政部', '负责公司日常行政事务和后勤管理'),
('产品研发部', '负责产品设计、开发和迭代'),
('生产部', '负责产品制造和质量控制'),
('市场部', '负责市场调研、营销策略和推广'),
('品牌部', '负责品牌建设、定位和传播');