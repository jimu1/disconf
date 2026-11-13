package com.baidu.disconf.web.web.config.validator;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baidu.disconf.core.common.constants.DisConfigTypeEnum;
import com.baidu.disconf.web.service.app.bo.App;
import com.baidu.disconf.web.service.app.service.AppMgr;
import com.baidu.disconf.web.service.config.bo.Config;
import com.baidu.disconf.web.service.config.form.ConfNewForm;
import com.baidu.disconf.web.service.config.form.ConfNewItemForm;
import com.baidu.disconf.web.service.config.form.ConfPublishTargetForm;
import com.baidu.disconf.web.service.config.service.ConfigFetchMgr;
import com.baidu.disconf.web.service.config.service.ConfigMgr;
import com.baidu.disconf.web.service.env.bo.Env;
import com.baidu.disconf.web.service.env.service.EnvMgr;
import com.baidu.disconf.web.service.user.service.AuthMgr;
import com.baidu.dsp.common.exception.FieldException;

/**
 * @author liaoqiqi
 * @version 2014-6-16
 */
@Service
public class ConfigValidator {

    @Autowired
    private AppMgr appMgr;

    @Autowired
    private EnvMgr envMgr;

    @Autowired
    private ConfigMgr configMgr;

    @Autowired
    private ConfigFetchMgr configFetchMgr;

    @Autowired
    private AuthMgr authMgr;

    /**
     * 校验
     *
     * @param id
     *
     * @return
     */
    public Config valideConfigExist(Long id) {

        //
        // config
        //
        Config config = configMgr.getConfigById(id);
        if (config == null) {
            throw new FieldException("configId", "config.id.not.exist", null);
        }

        //
        // validate app
        //
        validateAppAuth(config.getAppId());

        return config;
    }

    /**
     * 校验更新 配置值
     *
     * @param configId
     * @param value
     */
    public void validateUpdateItem(Long configId, String value) {

        //
        // config
        //
        valideConfigExist(configId);

        //
        // value
        //
        try {

            if (StringUtils.isEmpty(value)) {
                throw new Exception();
            }

        } catch (Exception e) {

            throw new FieldException("value", "conf.item.value.null", e);
        }

    }

    /**
     * 校验更新 配置文件
     *
     * @param configId
     * @param fileName
     */
    public void validateUpdateFile(Long configId, String fileName) {

        //
        // config
        //
        Config config = valideConfigExist(configId);

        //
        // value
        //
        try {

            if (!config.getName().equals(fileName)) {
                throw new Exception();
            }

        } catch (Exception e) {

            throw new FieldException("value", "conf.file.name.not.equal", e);
        }

    }

    /**
     * 判断配置是否更新
     *
     * @return
     */
    public boolean isValueUpdate(Long configId, String newValue) {

        //
        // 判断值有没有更新
        //
        String oldValue = configMgr.getValue(configId);

        if (newValue.equals(oldValue)) {
            return false;
        }
        return true;
    }

    /**
     * @param appId
     */
    public void validateAppAuth(Long appId) {

        boolean ret = authMgr.verifyApp4CurrentUser(appId);
        if (ret == false) {
            throw new FieldException(ConfNewForm.APPID, "app.auth.noright", null);
        }

    }

    /**
     * 校验新建 配置
     *
     * @param confNewForm
     * @param disConfigTypeEnum
     */
    public void validateNew(ConfNewItemForm confNewForm, DisConfigTypeEnum disConfigTypeEnum) {

        //
        // app
        //
        App app = appMgr.getById(confNewForm.getAppId());
        if (app == null) {
            throw new FieldException(ConfNewForm.APPID, "app.not.exist", null);
        }

        //
        validateAppAuth(app.getId());

        //
        // env
        //
        Env env = envMgr.getById(confNewForm.getEnvId());
        if (env == null) {
            throw new FieldException(ConfNewForm.ENVID, "env.not.exist", null);
        }

        //
        // key
        //
        Config config = configFetchMgr.getConfByParameter(app.getId(), env.getId(), confNewForm.getVersion(),
                confNewForm.getKey(), disConfigTypeEnum);
        if (config != null) {
            throw new FieldException(ConfNewItemForm.KEY, "key.exist", null);
        }

    }

    /**
     * 验证删除
     */
    public void validateDelete(Long configId) {

        Config config = configMgr.getConfigById(configId);
        if (config == null) {
            throw new FieldException("configId", "config.not.exist", null);
        }

        //
        validateAppAuth(config.getAppId());
    }

    /**
     * 校验公共发布
     *
     * @param configId 源配置ID
     * @param targets  目标列表
     */
    public void validateCommonPublish(Long configId, List<ConfPublishTargetForm> targets) {

        //
        // 源配置必须存在
        //
        Config config = configMgr.getConfigById(configId);
        if (config == null) {
            throw new FieldException("configId", "config.not.exist", null);
        }

        //
        // 源app权限
        //
        validateAppAuth(config.getAppId());

        //
        // 目标列表不能为空
        //
        if (targets == null || targets.isEmpty()) {
            throw new FieldException("targets", "publish.targets.empty", null);
        }

        //
        // 逐个校验目标
        //
        for (ConfPublishTargetForm target : targets) {

            if (target == null || !target.isComplete()) {
                throw new FieldException("targets", "publish.targets.invalid", null);
            }

            //
            // 目标app + 权限
            //
            App app = appMgr.getById(target.getAppId());
            if (app == null) {
                throw new FieldException("targets", "app.not.exist", null);
            }
            validateAppAuth(app.getId());

            //
            // 目标env
            //
            Env env = envMgr.getById(target.getEnvId());
            if (env == null) {
                throw new FieldException("targets", "env.not.exist", null);
            }

            //
            // 目标version
            //
            if (StringUtils.isEmpty(target.getVersion())) {
                throw new FieldException("targets", "version.not.exist", null);
            }
        }
    }

}
