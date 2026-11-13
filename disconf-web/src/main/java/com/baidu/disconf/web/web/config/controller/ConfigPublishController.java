package com.baidu.disconf.web.web.config.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.baidu.disconf.web.service.config.form.ConfPublishTargetForm;
import com.baidu.disconf.web.service.config.service.ConfigPublishMgr;
import com.baidu.disconf.web.service.config.vo.ConfigPublishResultVo;
import com.baidu.disconf.web.web.config.validator.ConfigValidator;
import com.baidu.dsp.common.constant.WebConstants;
import com.baidu.dsp.common.controller.BaseController;
import com.baidu.dsp.common.vo.JsonObjectBase;

/**
 * 公共发布: 将一个配置(配置项/配置文件)批量发布到多个目标(app + env + version)
 *
 * @author pi
 */
@Controller
@RequestMapping(WebConstants.API_PREFIX + "/web/config")
public class ConfigPublishController extends BaseController {

    protected static final Logger LOG = LoggerFactory.getLogger(ConfigPublishController.class);

    @Autowired
    private ConfigPublishMgr configPublishMgr;

    @Autowired
    private ConfigValidator configValidator;

    /**
     * 公共发布
     *
     * @param configId       源配置ID
     * @param targets        目标列表, JSON数组字符串, 如: [{"appId":1,"envId":1,"version":"1.0.0"}]
     * @param createIfAbsent 目标配置不存在时是否自动创建
     *
     * @return 每个目标的发布结果
     */
    @RequestMapping(value = "/publish/common", method = RequestMethod.POST)
    @ResponseBody
    public JsonObjectBase commonPublish(@RequestParam long configId, String targets,
                                        @RequestParam(required = false, defaultValue = "false") boolean
                                                createIfAbsent) {

        LOG.info("common publish, configId=" + configId + ", targets=" + targets + ", createIfAbsent=" +
                     createIfAbsent);

        //
        // 解析目标
        //
        List<ConfPublishTargetForm> targetList = configPublishMgr.parseTargets(targets);

        //
        // 业务校验(源配置、权限、目标app/env/version)
        //
        configValidator.validateCommonPublish(configId, targetList);

        //
        // 发布
        //
        List<ConfigPublishResultVo> resultVos =
                configPublishMgr.publishToTargets(configId, targetList, createIfAbsent);

        return buildListSuccess(resultVos, resultVos.size());
    }
}
