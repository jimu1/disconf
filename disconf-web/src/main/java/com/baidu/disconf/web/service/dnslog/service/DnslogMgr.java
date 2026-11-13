package com.baidu.disconf.web.service.dnslog.service;

import java.util.List;

import com.baidu.disconf.web.service.dnslog.form.DnslogSaveForm;
import com.baidu.disconf.web.service.dnslog.vo.DnslogConfigVo;
import com.baidu.disconf.web.service.dnslog.vo.DnslogRecordVo;
import com.baidu.disconf.web.service.dnslog.vo.DnslogTokenVo;

/**
 * DNSLog 平台配置管理：用于安全测试带外（OOB）检测
 * <p/>
 * 安全设计说明：
 * 1. 认证：/api/web/dnslog/** 默认被 LoginInterceptor 拦截；
 * 2. 授权：配置写操作（save/delete）仅管理员可用（Controller 层校验 RoleEnum.ADMIN）；
 * 3. SSRF 防护：记录查询的出站请求仅允许 http/https 公网地址，
 *    连接时逐 IP 校验（拒绝环回/私网/链路本地/保留地址），禁止重定向，
 *    固定连接/读取超时并限制响应大小；
 * 4. 敏感信息：apiToken 出参一律脱敏，不回传明文；
 * 5. 审计：配置变更与记录查询写专用 SECURITY_AUDIT 审计日志。
 *
 * @author pi
 * @version 2024-11-13
 */
public interface DnslogMgr {

    /**
     * 配置列表（apiToken 脱敏）
     */
    List<DnslogConfigVo> getConfigList();

    /**
     * 获取单个配置（apiToken 脱敏）
     */
    DnslogConfigVo getConfig(Long id);

    /**
     * 新建/更新配置（id 为空表示新建）
     *
     * @return 保存后的配置（apiToken 脱敏）
     */
    DnslogConfigVo save(DnslogSaveForm form);

    /**
     * 删除配置
     */
    void delete(Long id);

    /**
     * 生成随机 payload 子域标签（token.domain）
     *
     * @param name 配置名称
     */
    DnslogTokenVo genToken(String name);

    /**
     * 查询指定 token 的 DNS 解析记录：
     * 服务端按配置的 API 模板发起出站请求（带 SSRF 防护），
     * 返回平台原始响应
     *
     * @param name  配置名称
     * @param token payload 子域标签
     */
    DnslogRecordVo checkRecords(String name, String token);
}
