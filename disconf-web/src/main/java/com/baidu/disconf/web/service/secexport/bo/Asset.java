package com.baidu.disconf.web.service.secexport.bo;

import java.io.Serializable;

/**
 * 资产记录（演示数据模型，可替换为真实 DAO 数据源）
 *
 * @author secexport
 */
public class Asset implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 归属应用 ID，用于水平越权过滤 */
    private Long appId;

    private String appName;

    /** 资产地址：主机名或 IP */
    private String host;

    /** 端口 */
    private Integer port;

    /** 资产类型: web / api / db / middleware / host */
    private String assetType;

    /** 风险级别: high / medium / low */
    private String riskLevel;

    /** 运行环境：test / pre / online */
    private String env;

    /** 责任人 */
    private String owner;

    public Asset() {
    }

    public Asset(Long id, Long appId, String appName, String host, Integer port, String assetType,
                 String riskLevel, String env, String owner) {
        this.id = id;
        this.appId = appId;
        this.appName = appName;
        this.host = host;
        this.port = port;
        this.assetType = assetType;
        this.riskLevel = riskLevel;
        this.env = env;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "Asset [id=" + id + ", appName=" + appName + ", host=" + host + ":" + port +
                   ", assetType=" + assetType + ", riskLevel=" + riskLevel + "]";
    }
}
