package com.baidu.disconf.web.service.dnslog.bo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.dsp.common.dao.Columns;
import com.baidu.dsp.common.dao.DB;
import com.baidu.unbiz.common.genericdao.annotation.Column;
import com.baidu.unbiz.common.genericdao.annotation.Table;
import com.github.knightliao.apollo.db.bo.BaseObject;

/**
 * DNSLog 平台配置实体：安全测试带外（OOB）检测所用的
 * DNSLog 域名与记录查询 API 配置
 *
 * @author pi
 * @version 2024-11-13
 */
@Table(db = DB.DB_NAME, name = "dnslog_config", keyColumn = Columns.DNSLOG_ID)
public class DnslogConfig extends BaseObject<Long> {

    protected static final Logger LOG = LoggerFactory.getLogger(DnslogConfig.class);

    private static final long serialVersionUID = -2894736510283746510L;

    /**
     * 配置名称（唯一）
     */
    @Column(value = Columns.NAME)
    private String name;

    /**
     * DNSLog 域名（如 xxx.dnslog.cn）
     */
    @Column(value = Columns.DNSLOG_DOMAIN)
    private String domain;

    /**
     * 记录查询 API 模板，支持 {token} 与 {api_token} 占位符
     */
    @Column(value = Columns.DNSLOG_API_URL)
    private String apiUrl;

    /**
     * 平台认证 Token（查询 API 使用，可选）
     */
    @Column(value = Columns.DNSLOG_API_TOKEN)
    private String apiToken;

    /**
     * 是否启用：1 启用 / 0 停用
     */
    @Column(value = Columns.DNSLOG_ENABLED)
    private Integer enabled;

    /**
     * 描述
     */
    @Column(value = Columns.DNSLOG_DESCRIPTION)
    private String description;

    /**
     * 创建/维护者
     */
    @Column(value = Columns.DNSLOG_OWNER)
    private String owner;

    /**
     * 创建时间
     */
    @Column(value = Columns.CREATE_TIME)
    private String createTime;

    /**
     * 更新时间
     */
    @Column(value = Columns.UPDATE_TIME)
    private String updateTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "DnslogConfig{id=" + getId() + ", name=" + name + ", domain=" + domain +
                ", apiUrl=" + apiUrl + ", enabled=" + enabled + ", owner=" + owner +
                ", createTime=" + createTime + ", updateTime=" + updateTime + "}";
    }
}
