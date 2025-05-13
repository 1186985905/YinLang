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