package com.baidu.disconf.web.service.secexport.form;

/**
 * 资产导出查询条件
 *
 * @author secexport
 */
public class AssetExportForm {

    /** 资产类型（可选）: web / api / db / middleware / host */
    private String assetType;

    /** 风险级别（可选）: high / medium / low */
    private String riskLevel;

    /** 环境（可选）: test / pre / online */
    private String env;

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

    @Override
    public String toString() {
        return "AssetExportForm [assetType=" + assetType + ", riskLevel=" + riskLevel + ", env=" + env + "]";
    }
}
