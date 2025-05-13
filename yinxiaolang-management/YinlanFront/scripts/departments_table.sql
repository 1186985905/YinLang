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

-- 向部门表添加初始数据
INSERT INTO `departments` (`name`, `description`) VALUES 
('研发部-前端组', '负责前端开发和维护'),
('研发部-后端组', '负责后端开发和API设计'),
('产品部', '负责产品规划和设计'),
('市场部', '负责市场营销和推广'),
('管理部', '负责公司行政和管理');

-- 在Users表中添加department_id字段（如果需要）
ALTER TABLE `users` ADD COLUMN `department_id` bigint(20) DEFAULT NULL COMMENT '部门ID' AFTER `role`;
ALTER TABLE `users` ADD CONSTRAINT `fk_users_department_id` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`); 