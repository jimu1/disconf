package com.baidu.disconf.web.service.dnslog.form;

import com.baidu.dsp.common.form.RequestFormBase;

import lombok.Data;

/**
 * DNSLog 配置保存表单（新建与更新共用，id 为空表示新建）
 *
 * @author pi
 * @version 2024-11-13
 */
@Data
public class DnslogSaveForm extends RequestFormBase {

    private static final long serialVersionUID = 3628174651029384756L;

    /**
     * 配置 ID，更新时必填
     */
    private Long id;

    /**
     * 配置名称（唯一），必填
     */
    private String name;

    /**
     * DNSLog 域名，必填，如 xxx.dnslog.cn
     */
    private String domain;

    /**
     * 记录查询 API 模板，可选，
     * 支持 {token}（payload 子域名）与 {api_token}（认证 Token）占位符
     */
    private String apiUrl;

    /**
     * 平台认证 Token，可选
     */
    private String apiToken;

    /**
     * 是否启用：1 启用 / 0 停用，默认启用
     */
    private Integer enabled = 1;

    /**
     * 描述，可选
     */
    private String description;
}
