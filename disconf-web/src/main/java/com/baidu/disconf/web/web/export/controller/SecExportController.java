package com.baidu.disconf.web.web.export.controller;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.baidu.disconf.web.service.secexport.bo.Asset;
import com.baidu.disconf.web.service.secexport.bo.Vulnerability;
import com.baidu.disconf.web.service.secexport.form.AssetExportForm;
import com.baidu.disconf.web.service.secexport.form.VulnExportForm;
import com.baidu.disconf.web.service.secexport.service.VulnAssetExportMgr;
import com.baidu.disconf.web.service.secexport.utils.CsvUtils;
import com.baidu.disconf.web.service.user.dto.Visitor;
import com.baidu.dsp.common.constant.WebConstants;
import com.baidu.dsp.common.controller.BaseController;
import com.baidu.dsp.common.vo.JsonObjectBase;
import com.baidu.ub.common.commons.ThreadContext;

/**
 * 漏洞与资产导出接口
 * <p/>
 * 安全设计说明：
 * 1. 认证：/api/web/export/** 默认被 LoginInterceptor 拦截，未登录请求直接拒绝；
 * 2. 授权：导出数据按会话用户的授权应用（Visitor.appIds）过滤，防水平越权；
 * 3. 上限量：单次导出上限 {@link #MAX_EXPORT_ROWS} 行，防资源耗尽；
 * 4. 文件名：服务端生成（类型+时间戳+随机串），不使用任何用户输入；
 * 5. 响应头：Content-Disposition: attachment + 显式 Content-Type + nosniff，防浏览器嗅探解析；
 * 6. 内容安全：CSV 单元格做公式注入转义（见 CsvUtils）；
 * 7. 审计：导出操作记录专用审计日志（操作人/API/类型/条件/行数），不记录导出内容明文。
 *
 * @author secexport
 */
@Controller
@RequestMapping(WebConstants.API_PREFIX + "/web/export")
public class SecExportController extends BaseController {

    protected static final Logger LOG = LoggerFactory.getLogger(SecExportController.class);

    /** 专用安全审计日志（独立 category，便于采集与检索） */
    protected static final Logger SEC_AUDIT_LOG = LoggerFactory.getLogger("SECURITY_AUDIT");

    /** 单次导出最大行数 */
    private static final int MAX_EXPORT_ROWS = 5000;

    @Autowired
    private VulnAssetExportMgr vulnAssetExportMgr;

    /**
     * 漏洞导出概览（导出前预览条数，JSON）
     */
    @RequestMapping(value = "/vuln/overview", method = RequestMethod.GET)
    @ResponseBody
    public JsonObjectBase vulnOverview(VulnExportForm form) {

        Visitor visitor = getCurrentVisitor();
        List<Vulnerability> list = vulnAssetExportMgr.listVulnerabilities(getAppIds(visitor), form);

        auditLog(visitor, "/api/web/export/vuln/overview", "vuln_overview", form.toString(), list.size());

        return buildSuccess("count", list.size());
    }

    /**
     * 资产导出概览（导出前预览条数，JSON）
     */
    @RequestMapping(value = "/asset/overview", method = RequestMethod.GET)
    @ResponseBody
    public JsonObjectBase assetOverview(AssetExportForm form) {

        Visitor visitor = getCurrentVisitor();
        List<Asset> list = vulnAssetExportMgr.listAssets(getAppIds(visitor), form);

        auditLog(visitor, "/api/web/export/asset/overview", "asset_overview", form.toString(), list.size());

        return buildSuccess("count", list.size());
    }

    /**
     * 漏洞列表 CSV 导出
     */
    @RequestMapping(value = "/vuln", method = RequestMethod.GET)
    public HttpEntity<byte[]> exportVuln(VulnExportForm form) {

        Visitor visitor = getCurrentVisitor();

        List<Vulnerability> list = vulnAssetExportMgr.listVulnerabilities(getAppIds(visitor), form);

        if (list.size() > MAX_EXPORT_ROWS) {
            list = list.subList(0, MAX_EXPORT_ROWS);
        }

        String[] headers = new String[]{
                "ID", "应用", "漏洞标题", "漏洞编号", "严重级别", "状态", "发现时间", "责任人"};

        List<String[]> rows = new ArrayList<String[]>(list.size());
        for (Vulnerability v : list) {
            rows.add(new String[]{
                    String.valueOf(v.getId()),
                    safe(v.getAppName()),
                    safe(v.getTitle()),
                    safe(v.getVulnId()),
                    safe(v.getSeverity()),
                    safe(v.getStatus()),
                    safe(v.getDiscoverTime()),
                    safe(v.getOwner())});
        }

        auditLog(visitor, "/api/web/export/vuln", "vuln_export", form.toString(), list.size());

        return buildCsvResponse("vuln_export", headers, rows);
    }

    /**
     * 资产列表 CSV 导出
     */
    @RequestMapping(value = "/asset", method = RequestMethod.GET)
    public HttpEntity<byte[]> exportAsset(AssetExportForm form) {

        Visitor visitor = getCurrentVisitor();

        List<Asset> list = vulnAssetExportMgr.listAssets(getAppIds(visitor), form);

        if (list.size() > MAX_EXPORT_ROWS) {
            list = list.subList(0, MAX_EXPORT_ROWS);
        }

        String[] headers = new String[]{
                "ID", "应用", "主机", "端口", "资产类型", "风险级别", "环境", "责任人"};

        List<String[]> rows = new ArrayList<String[]>(list.size());
        for (Asset a : list) {
            rows.add(new String[]{
                    String.valueOf(a.getId()),
                    safe(a.getAppName()),
                    safe(a.getHost()),
                    a.getPort() == null ? "" : String.valueOf(a.getPort()),
                    safe(a.getAssetType()),
                    safe(a.getRiskLevel()),
                    safe(a.getEnv()),
                    safe(a.getOwner())});
        }

        auditLog(visitor, "/api/web/export/asset", "asset_export", form.toString(), list.size());

        return buildCsvResponse("asset_export", headers, rows);
    }

    /**
     * 构建 CSV 下载响应。
     * 文件名完全由服务端生成：类型_yyyyMMddHHmmss_随机8位.csv，不包含任何用户输入。
     */
    private HttpEntity<byte[]> buildCsvResponse(String type, String[] headers, List<String[]> rows) {

        String csv = CsvUtils.buildCsv(headers, rows);

        byte[] bom = CsvUtils.UTF8_BOM;
        byte[] body = csv.getBytes(java.nio.charset.Charset.forName("UTF-8"));
        byte[] res = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, res, 0, bom.length);
        System.arraycopy(body, 0, res, bom.length, body.length);

        String filename = type + "_" +
                new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "_" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ".csv";

        HttpHeaders header = new HttpHeaders();
        header.set("Content-Type", "text/csv;charset=UTF-8");
        header.set("Content-Disposition", "attachment; filename=" + filename);
        header.set("X-Content-Type-Options", "nosniff");
        header.setContentLength(res.length);

        return new HttpEntity<byte[]>(res, header);
    }

    /**
     * 获取当前会话用户。LoginInterceptor 已保证到达此处必然已登录，
     * 此处判空仅作为纵深防御（defense in depth）。
     */
    private Visitor getCurrentVisitor() {

        Visitor visitor = ThreadContext.getSessionVisitor();
        if (visitor == null) {
            throw new SecurityException("session visitor not found, please login first");
        }
        return visitor;
    }

    private Long[] getAppIds(Visitor visitor) {

        Set<Long> appIds = visitor.getAppIds();
        if (appIds == null || appIds.isEmpty()) {
            return new Long[0];
        }
        return appIds.toArray(new Long[appIds.size()]);
    }

    /**
     * 安全审计日志：记录操作人、接口、操作类型、过滤条件、数据量。
     * 不记录导出内容明文。
     */
    private void auditLog(Visitor visitor, String api, String operateType, String condition, int count) {

        // CRLF注入防护：审计日志内容剥离换行符，防止伪造日志条目（CWE-117）
        SEC_AUDIT_LOG.info("operateUserid={}, operateFunction={}, operateType={}, condition={}, " +
                "sensitiveDataType=secexport, sensitiveDataCount={}",
                stripCrlf(visitor.getLoginUserName()), stripCrlf(api), stripCrlf(operateType),
                stripCrlf(condition), count);
    }

    /**
     * 剥离回车换行符，防日志伪造
     */
    private String stripCrlf(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\r\\n]", "");
    }

    /**
     * 兜底清洗：数据源字段理论上不含换行/引号，仍统一走 CSV 转义，双保险。
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }
}
