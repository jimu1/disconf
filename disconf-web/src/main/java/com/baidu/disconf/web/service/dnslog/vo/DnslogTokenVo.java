package com.baidu.disconf.web.service.dnslog.vo;

import java.io.Serializable;

import lombok.Data;

/**
 * DNSLog payload 子域名生成结果
 *
 * @author pi
 * @version 2024-11-13
 */
@Data
public class DnslogTokenVo implements Serializable {

    private static final long serialVersionUID = -4810927365019283746L;

    /**
     * 随机生成的子域标签（如 3f9a2c8d1b7e）
     */
    private String token;

    /**
     * 所属 DNSLog 域名
     */
    private String domain;

    /**
     * 完整 payload 子域名：token.domain
     */
    private String fullDomain;
}
