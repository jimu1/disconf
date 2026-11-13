package com.baidu.disconf.web.service.config.vo;

/**
 * 公共发布:单个目标的发布结果
 *
 * @author pi
 */
public class ConfigPublishResultVo {

    public static final String RESULT_SUCCESS = "success";
    public static final String RESULT_SKIP = "skip";
    public static final String RESULT_FAIL = "fail";

    /**
     * 目标 app
     */
    private Long appId;
    private String appName;

    /**
     * 目标 env
     */
    private Long envId;
    private String envName;

    /**
     * 目标 version
     */
    private String version;

    /**
     * 配置名(配置文件名 或 配置项 key)
     */
    private String key;

    /**
     * 目标配置的 configId(自动新建前为空)
     */
    private Long targetConfigId;

    /**
     * success / skip / fail
     */
    private String result;

    private String message;

    public static ConfigPublishResultVo build(Long appId, String appName, Long envId, String envName,
                                              String version, String key, String result, String message) {
        ConfigPublishResultVo vo = new ConfigPublishResultVo();
        vo.setAppId(appId);
        vo.setAppName(appName);
        vo.setEnvId(envId);
        vo.setEnvName(envName);
        vo.setVersion(version);
        vo.setKey(key);
        vo.setResult(result);
        vo.setMessage(message);
        return vo;
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

    public Long getEnvId() {
        return envId;
    }

    public void setEnvId(Long envId) {
        this.envId = envId;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getTargetConfigId() {
        return targetConfigId;
    }

    public void setTargetConfigId(Long targetConfigId) {
        this.targetConfigId = targetConfigId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ConfigPublishResultVo [appId=" + appId + ", appName=" + appName + ", envId=" + envId + ", envName=" +
                   envName + ", version=" + version + ", key=" + key + ", targetConfigId=" + targetConfigId +
                   ", result=" + result + ", message=" + message + "]";
    }
}
