package com.baidu.disconf.web.service.dnslog.vo;

import java.io.Serializable;

import lombok.Data;

/**
 * DNSLog 记录查询结果：返回平台 API 的原始响应（截断），
 * 由调用方按平台自身格式解读
 *
 * @author pi
 * @version 2024-11-13
 */
@Data
public class DnslogRecordVo implements Serializable {

    private static final long serialVersionUID = -5920183746501829374L;

    /**
     * 查询使用的配置名称
     */
    private String configName;

    /**
     * 查询的 payload token
     */
    private String token;

    /**
     * 实际请求的平台 API URL（认证参数脱敏后）
     */
    private String requestUrl;

    /**
     * 平台 API HTTP 状态码，-1 表示请求失败
     */
    private int status;

    /**
     * 平台 API 原始响应内容（UTF-8，最多 64KB，超出截断）
     */
    private String content;

    /**
     * 是否被截断
     */
    private boolean truncated;

    /**
     * 失败原因（请求失败时）
     */
    private String error;
}
