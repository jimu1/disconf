package com.baidu.disconf.web.service.secexport.form;

/**
 * 漏洞导出查询条件
 *
 * @author secexport
 */
public class VulnExportForm {

    /** 严重级别（可选）: critical / high / medium / low */
    private String severity;

    /** 状态（可选）: open / fixed / ignored */
    private String status;

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "VulnExportForm [severity=" + severity + ", status=" + status + "]";
    }
}
