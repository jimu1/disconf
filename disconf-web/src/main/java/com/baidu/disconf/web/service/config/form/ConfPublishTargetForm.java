package com.baidu.disconf.web.service.config.form;

import org.apache.commons.lang3.StringUtils;

/**
 * 公共发布的目标(app + env + version)
 *
 * @author pi
 */
public class ConfPublishTargetForm {

    private Long appId;

    private Long envId;

    private String version;

    public ConfPublishTargetForm() {
    }

    public ConfPublishTargetForm(Long appId, Long envId, String version) {
        this.appId = appId;
        this.envId = envId;
        this.version = version;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public Long getEnvId() {
        return envId;
    }

    public void setEnvId(Long envId) {
        this.envId = envId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * 目标是否完整
     */
    public boolean isComplete() {
        return appId != null && envId != null && StringUtils.isNotEmpty(version);
    }

    @Override
    public String toString() {
        return "ConfPublishTargetForm [appId=" + appId + ", envId=" + envId + ", version=" + version + "]";
    }
}
