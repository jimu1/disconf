package com.baidu.disconf.web.service.dnslog.vo;

import java.io.Serializable;

import lombok.Data;

/**
 * DNSLog 配置返回对象（apiToken 脱敏，不回传明文）
 *
 * @author pi
 * @version 2024-11-13
 */
@Data
public class DnslogConfigVo implements Serializable {

    private static final long serialVersionUID = -7918273645018293756L;

    private Long id;

    private String name;

    private String domain;

    private String apiUrl;

    /**
     * 脱敏后的认证 Token（仅保留首尾各 4 位）
     */
    private String maskedApiToken;

    private Boolean enabled;

    private String description;

    private String owner;

    private String createTime;

    private String updateTime;
}
