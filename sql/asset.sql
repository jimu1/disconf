-- =====================================================
-- 资产(asset)表：存储上传到服务器的本地资产文件元信息
-- 文件实体保存在服务器本地磁盘（disconf.web.asset.dir 配置目录）
-- =====================================================

CREATE TABLE `asset` (
    `asset_id`           BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '资产ID（主键，自增长）',
    `name`               VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '展示名',
    `original_file_name` VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '用户上传时的原始文件名',
    `stored_name`        VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '服务器存储文件名（UUID生成）',
    `file_size`          BIGINT(20)   NOT NULL DEFAULT 0      COMMENT '文件大小（字节）',
    `file_type`          VARCHAR(32)  NOT NULL DEFAULT ''     COMMENT '文件后缀（如 .zip）',
    `content_type`       VARCHAR(128) NOT NULL DEFAULT ''     COMMENT 'MIME 类型',
    `asset_app`          VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '归属的APP名',
    `asset_desc`         VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '描述',
    `asset_owner`        VARCHAR(128) NOT NULL DEFAULT ''     COMMENT '上传者',
    `create_time`        VARCHAR(14)  NOT NULL DEFAULT '99991231235959' COMMENT '创建时间',
    `update_time`        VARCHAR(14)  NOT NULL DEFAULT '99991231235959' COMMENT '更新时间',
    PRIMARY KEY (`asset_id`),
    UNIQUE KEY `uk_stored_name` (`stored_name`),
    KEY `idx_asset_app` (`asset_app`),
    KEY `idx_asset_owner` (`asset_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='资产表';
