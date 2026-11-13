package com.baidu.disconf.web.service.config.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baidu.disconf.core.common.constants.DisConfigTypeEnum;
import com.baidu.disconf.web.common.Constants;
import com.baidu.disconf.web.config.ApplicationPropertyConfig;
import com.baidu.disconf.web.service.app.bo.App;
import com.baidu.disconf.web.service.app.service.AppMgr;
import com.baidu.disconf.web.service.config.bo.Config;
import com.baidu.disconf.web.service.config.dao.ConfigDao;
import com.baidu.disconf.web.service.config.form.ConfPublishTargetForm;
import com.baidu.disconf.web.service.config.service.ConfigHistoryMgr;
import com.baidu.disconf.web.service.config.service.ConfigMgr;
import com.baidu.disconf.web.service.config.service.ConfigPublishMgr;
import com.baidu.disconf.web.service.config.vo.ConfigPublishResultVo;
import com.baidu.disconf.web.service.env.bo.Env;
import com.baidu.disconf.web.service.env.service.EnvMgr;
import com.baidu.dsp.common.constant.DataFormatConstants;
import com.baidu.dsp.common.exception.FieldException;
import com.baidu.dsp.common.utils.email.LogMailBean;
import com.github.knightliao.apollo.utils.time.DateUtils;
import com.google.gson.Gson;

/**
 * 公共发布实现
 *
 * @author pi
 */
@Service
public class ConfigPublishMgrImpl implements ConfigPublishMgr {

    protected static final Logger LOG = LoggerFactory.getLogger(ConfigPublishMgrImpl.class);

    private static final Gson GSON = new Gson();

    @Autowired
    private ConfigDao configDao;

    @Autowired
    private ConfigMgr configMgr;

    @Autowired
    private ConfigHistoryMgr configHistoryMgr;

    @Autowired
    private AppMgr appMgr;

    @Autowired
    private EnvMgr envMgr;

    @Autowired
    private LogMailBean logMailBean;

    @Autowired
    private ApplicationPropertyConfig applicationPropertyConfig;

    /**
     * 解析公共发布目标列表
     */
    @Override
    public List<ConfPublishTargetForm> parseTargets(String targetsJson) {

        List<ConfPublishTargetForm> targets = new ArrayList<ConfPublishTargetForm>();

        if (StringUtils.isEmpty(targetsJson)) {
            return targets;
        }

        try {
            ConfPublishTargetForm[] arr = GSON.fromJson(targetsJson, ConfPublishTargetForm[].class);
            if (arr != null) {
                targets.addAll(Arrays.asList(arr));
            }
        } catch (Exception e) {
            LOG.error("parse common publish targets error: " + targetsJson, e);
            throw new FieldException("targets", "publish.targets.parse.error", e);
        }

        return targets;
    }

    /**
     * 公共发布
     */
    @Override
    public List<ConfigPublishResultVo> publishToTargets(Long sourceConfigId, List<ConfPublishTargetForm> targets,
                                                        boolean createIfAbsent) {

        Config source = configMgr.getConfigById(sourceConfigId);

        DisConfigTypeEnum typeEnum = DisConfigTypeEnum.FILE;
        if (source.getType().equals(DisConfigTypeEnum.ITEM.getType())) {
            typeEnum = DisConfigTypeEnum.ITEM;
        }

        // 数据库里存的是 unicode 编码后的值, 直接同步, 避免二次编解码
        String encodedValue = source.getValue();

        List<ConfigPublishResultVo> results = new ArrayList<ConfigPublishResultVo>();

        for (ConfPublishTargetForm target : targets) {

            ConfigPublishResultVo vo;
            try {
                vo = publishOne(source, typeEnum, encodedValue, target, createIfAbsent);
            } catch (Exception e) {
                LOG.error("common publish error, source configId=" + sourceConfigId + ", target=" + target, e);
                vo = buildResultVo(source.getName(), target, ConfigPublishResultVo.RESULT_FAIL,
                        "发布失败: " + e.getMessage());
            }
            results.add(vo);
        }

        // 汇总邮件通知(一封)
        sendSummaryEmail(source, results);

        return results;
    }

    /**
     * 向单个目标发布
     */
    private ConfigPublishResultVo publishOne(Config source, DisConfigTypeEnum typeEnum, String encodedValue,
                                             ConfPublishTargetForm target, boolean createIfAbsent) {

        //
        // 目标已存在同名配置则覆盖更新
        //
        Config targetConfig = configDao.getByParameter(target.getAppId(), target.getEnvId(), target.getVersion(),
                source.getName(), typeEnum);

        if (targetConfig != null) {

            // 目标即源配置本身, 跳过
            if (targetConfig.getId().equals(source.getId())) {
                return buildResultVo(source.getName(), target, ConfigPublishResultVo.RESULT_SKIP,
                        "目标即源配置本身, 已跳过");
            }

            String oldValue = targetConfig.getValue();

            //
            // 更新值 + 写历史 + 通知ZK
            //
            configDao.updateValue(targetConfig.getId(), encodedValue);
            configHistoryMgr.createOne(targetConfig.getId(), oldValue, encodedValue);
            configMgr.notifyZookeeper(targetConfig.getId());

            ConfigPublishResultVo vo = buildResultVo(source.getName(), target, ConfigPublishResultVo.RESULT_SUCCESS,
                    "发布成功(覆盖更新)");
            vo.setTargetConfigId(targetConfig.getId());
            return vo;
        }

        //
        // 目标不存在
        //
        if (!createIfAbsent) {
            return buildResultVo(source.getName(), target, ConfigPublishResultVo.RESULT_SKIP,
                    "目标配置不存在, 且未开启自动创建, 已跳过");
        }

        //
        // 自动创建 + 写历史 + 通知ZK
        //
        Config config = new Config();
        config.setAppId(target.getAppId());
        config.setEnvId(target.getEnvId());
        config.setVersion(target.getVersion());
        config.setName(source.getName());
        config.setType(typeEnum.getType());
        config.setValue(encodedValue);
        config.setStatus(Constants.STATUS_NORMAL);

        String curTime = DateUtils.format(new Date(), DataFormatConstants.COMMON_TIME_FORMAT);
        config.setCreateTime(curTime);
        config.setUpdateTime(curTime);

        configDao.create(config);
        configHistoryMgr.createOne(config.getId(), "", encodedValue);
        configMgr.notifyZookeeper(config.getId());

        ConfigPublishResultVo vo = buildResultVo(source.getName(), target, ConfigPublishResultVo.RESULT_SUCCESS,
                "发布成功(自动新建)");
        vo.setTargetConfigId(config.getId());
        return vo;
    }

    /**
     * 组装单个目标的结果VO
     */
    private ConfigPublishResultVo buildResultVo(String key, ConfPublishTargetForm target, String result,
                                                String message) {

        App app = appMgr.getById(target.getAppId());
        Env env = envMgr.getById(target.getEnvId());

        ConfigPublishResultVo vo = ConfigPublishResultVo.build(target.getAppId(),
                app == null ? String.valueOf(target.getAppId()) : app.getName(),
                target.getEnvId(),
                env == null ? String.valueOf(target.getEnvId()) : env.getName(),
                target.getVersion(),
                key,
                result,
                message);
        return vo;
    }

    /**
     * 汇总发送一封邮件通知(源app + 各目标app 的邮箱)
     */
    private void sendSummaryEmail(Config source, List<ConfigPublishResultVo> results) {

        try {

            if (!applicationPropertyConfig.isEmailMonitorOn() || results.isEmpty()) {
                return;
            }

            // 收集邮箱并去重
            Set<String> emailSet = new HashSet<String>();
            addEmails(emailSet, appMgr.getEmails(source.getAppId()));
            for (ConfigPublishResultVo vo : results) {
                addEmails(emailSet, appMgr.getEmails(vo.getAppId()));
            }
            if (emailSet.isEmpty()) {
                return;
            }

            StringBuilder content = new StringBuilder();
            content.append("<h3>公共发布通知</h3>");
            content.append("<p>源配置: ").append(source.getName())
                    .append(" (configId=").append(source.getId()).append(")</p>");
            content.append("<table border='1' cellspacing='0' cellpadding='4'>");
            content.append("<tr><th>App</th><th>Env</th><th>Version</th><th>结果</th><th>说明</th></tr>");
            for (ConfigPublishResultVo vo : results) {
                content.append("<tr><td>").append(vo.getAppName())
                        .append("</td><td>").append(vo.getEnvName())
                        .append("</td><td>").append(vo.getVersion())
                        .append("</td><td>").append(vo.getResult())
                        .append("</td><td>").append(vo.getMessage())
                        .append("</td></tr>");
            }
            content.append("</table>");

            logMailBean.sendHtmlEmail(StringUtils.join(emailSet, ","), " config common publish", content.toString());

        } catch (Exception e) {
            LOG.warn("common publish summary email send error: " + e.toString());
        }
    }

    private void addEmails(Set<String> emailSet, String emails) {
        if (StringUtils.isNotEmpty(emails)) {
            emailSet.addAll(Arrays.asList(emails.split(",")));
        }
    }
}
