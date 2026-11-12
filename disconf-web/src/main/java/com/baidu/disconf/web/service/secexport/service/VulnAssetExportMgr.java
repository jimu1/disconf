package com.baidu.disconf.web.service.secexport.service;

import java.util.List;

import com.baidu.disconf.web.service.secexport.bo.Asset;
import com.baidu.disconf.web.service.secexport.bo.Vulnerability;
import com.baidu.disconf.web.service.secexport.form.AssetExportForm;
import com.baidu.disconf.web.service.secexport.form.VulnExportForm;

/**
 * 漏洞与资产导出服务
 *
 * @author secexport
 */
public interface VulnAssetExportMgr {

    /**
     * 查询当前用户（按 appIds 授权范围内）的漏洞列表
     *
     * @param appIds 当前会话用户被授权的应用 ID 集合（水平越权过滤）
     * @param form   过滤条件（severity/status，可为空）
     */
    List<Vulnerability> listVulnerabilities(Long[] appIds, VulnExportForm form);

    /**
     * 查询当前用户（按 appIds 授权范围内）的资产列表
     *
     * @param appIds 当前会话用户被授权的应用 ID 集合（水平越权过滤）
     * @param form   过滤条件（assetType/riskLevel/env，可为空）
     */
    List<Asset> listAssets(Long[] appIds, AssetExportForm form);
}
