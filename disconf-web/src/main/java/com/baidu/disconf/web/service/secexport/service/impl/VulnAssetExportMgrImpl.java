package com.baidu.disconf.web.service.secexport.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baidu.disconf.web.service.secexport.bo.Asset;
import com.baidu.disconf.web.service.secexport.bo.Vulnerability;
import com.baidu.disconf.web.service.secexport.form.AssetExportForm;
import com.baidu.disconf.web.service.secexport.form.VulnExportForm;
import com.baidu.disconf.web.service.secexport.service.VulnAssetExportMgr;

/**
 * 漏洞与资产导出服务实现（演示数据源，可替换为真实 DAO）
 *
 * @author secexport
 */
@Service
public class VulnAssetExportMgrImpl implements VulnAssetExportMgr {

    protected static final Logger LOG = LoggerFactory.getLogger(VulnAssetExportMgrImpl.class);

    /** 过滤参数白名单 */
    public static final Set<String> SEVERITY_SET =
            new HashSet<String>(Arrays.asList("critical", "high", "medium", "low"));
    public static final Set<String> VULN_STATUS_SET =
            new HashSet<String>(Arrays.asList("open", "fixed", "ignored"));
    public static final Set<String> ASSET_TYPE_SET =
            new HashSet<String>(Arrays.asList("web", "api", "db", "middleware", "host"));
    public static final Set<String> RISK_LEVEL_SET =
            new HashSet<String>(Arrays.asList("high", "medium", "low"));
    public static final Set<String> ENV_SET =
            new HashSet<String>(Arrays.asList("test", "pre", "online"));

    /**
     * 演示数据（生产环境应替换为 DAO 查询）
     */
    private static final List<Vulnerability> VULN_DEMO_DATA = new ArrayList<Vulnerability>();
    private static final List<Asset> ASSET_DEMO_DATA = new ArrayList<Asset>();

    static {
        VULN_DEMO_DATA.add(new Vulnerability(1L, 1L, "demoApp", "SQL注入-订单查询", "CVE-2026-1001",
                "critical", "open", "2026-11-01 10:00:00", "zhangsan"));
        VULN_DEMO_DATA.add(new Vulnerability(2L, 1L, "demoApp", "存储型XSS-评论", "CVE-2026-1002",
                "high", "fixed", "2026-11-03 14:30:00", "lisi"));
        VULN_DEMO_DATA.add(new Vulnerability(3L, 2L, "sampleApp", "敏感信息泄露", "SVA-2026-0033",
                "medium", "open", "2026-11-05 09:15:00", "wangwu"));
        VULN_DEMO_DATA.add(new Vulnerability(4L, 2L, "sampleApp", "弱口令", "SVA-2026-0044",
                "low", "ignored", "2026-11-08 16:45:00", "zhangsan"));

        ASSET_DEMO_DATA.add(new Asset(1L, 1L, "demoApp", "10.10.1.11", 8080,
                "web", "high", "online", "zhangsan"));
        ASSET_DEMO_DATA.add(new Asset(2L, 1L, "demoApp", "10.10.1.12", 3306,
                "db", "medium", "online", "lisi"));
        ASSET_DEMO_DATA.add(new Asset(3L, 2L, "sampleApp", "10.20.1.21", 9090,
                "api", "low", "pre", "wangwu"));
        ASSET_DEMO_DATA.add(new Asset(4L, 2L, "sampleApp", "10.20.1.22", 6379,
                "middleware", "high", "test", "zhangsan"));
    }

    @Override
    public List<Vulnerability> listVulnerabilities(Long[] appIds, VulnExportForm form) {

        List<Vulnerability> result = new ArrayList<Vulnerability>();

        String severity = normalize(form == null ? null : form.getSeverity(), SEVERITY_SET);
        String status = normalize(form == null ? null : form.getStatus(), VULN_STATUS_SET);

        for (Vulnerability vuln : VULN_DEMO_DATA) {

            // 水平越权过滤：仅返回授权应用的数据
            if (!containsApp(appIds, vuln.getAppId())) {
                continue;
            }
            if (severity != null && !severity.equals(vuln.getSeverity())) {
                continue;
            }
            if (status != null && !status.equals(vuln.getStatus())) {
                continue;
            }
            result.add(vuln);
        }

        return result;
    }

    @Override
    public List<Asset> listAssets(Long[] appIds, AssetExportForm form) {

        List<Asset> result = new ArrayList<Asset>();

        String assetType = normalize(form == null ? null : form.getAssetType(), ASSET_TYPE_SET);
        String riskLevel = normalize(form == null ? null : form.getRiskLevel(), RISK_LEVEL_SET);
        String env = normalize(form == null ? null : form.getEnv(), ENV_SET);

        for (Asset asset : ASSET_DEMO_DATA) {

            // 水平越权过滤：仅返回授权应用的数据
            if (!containsApp(appIds, asset.getAppId())) {
                continue;
            }
            if (assetType != null && !assetType.equals(asset.getAssetType())) {
                continue;
            }
            if (riskLevel != null && !riskLevel.equals(asset.getRiskLevel())) {
                continue;
            }
            if (env != null && !env.equals(asset.getEnv())) {
                continue;
            }
            result.add(asset);
        }

        return result;
    }

    /**
     * 过滤参数白名单归一化：不在白名单内的值一律忽略（按未过滤处理），
     * 避免非法参数导致异常或被拼入输出
     */
    private String normalize(String value, Set<String> whitelist) {

        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase();
        if (v.length() == 0) {
            return null;
        }
        if (whitelist.contains(v)) {
            return v;
        }
        LOG.warn("export filter param not in whitelist, ignored: {}", v);
        return null;
    }

    private boolean containsApp(Long[] appIds, Long appId) {

        if (appIds == null || appId == null) {
            return false;
        }
        for (Long id : appIds) {
            if (id != null && id.equals(appId)) {
                return true;
            }
        }
        return false;
    }
}
