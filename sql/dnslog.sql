-- =====================================================
-- DNSLog 配置表：存储安全测试（带外 OOB 检测）使用的
-- DNSLog 平台配置（域名 / 记录查询 API / 认证 Token）
-- =====================================================

CREATE TABLE `dnslog_config` (
    `dnslog_id`     BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '配置ID（主键，自增长）',
    `name`          VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '配置名称（唯一）',
    `domain`        VARCHAR(255) NOT NULL DEFAULT ''     COMMENT 'DNSLog 域名（如 xxx.dnslog.cn）',
    `api_url`       VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '记录查询 API 模板，支持 {token} 与 {api_token} 占位符',
    `api_token`     VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '平台认证 Token（查询 API 使用，可选）',
    `enabled`       INT(11)      NOT NULL DEFAULT 1      COMMENT '是否启用：1 启用 / 0 停用',
    `description`   VARCHAR(512) NOT NULL DEFAULT ''     COMMENT '描述',
    `dnslog_owner`  VARCHAR(128) NOT NULL DEFAULT ''     COMMENT '创建/维护者',
    `create_time`   VARCHAR(14)  NOT NULL DEFAULT '99991231235959' COMMENT '创建时间',
    `update_time`   VARCHAR(14)  NOT NULL DEFAULT '99991231235959' COMMENT '更新时间',
    PRIMARY KEY (`dnslog_id`),
    UNIQUE KEY `uk_dnslog_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='DNSLog 平台配置表';
