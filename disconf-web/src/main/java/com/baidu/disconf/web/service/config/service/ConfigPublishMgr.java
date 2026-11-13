package com.baidu.disconf.web.service.config.service;

import java.util.List;

import com.baidu.disconf.web.service.config.form.ConfPublishTargetForm;
import com.baidu.disconf.web.service.config.vo.ConfigPublishResultVo;

/**
 * 公共发布: 将一个配置(配置项/配置文件)的值批量发布到多个目标(app + env + version)
 *
 * @author pi
 */
public interface ConfigPublishMgr {

    /**
     * 解析公共发布目标列表(JSON数组字符串)
     * <p/>
     * 形如: [{"appId":1,"envId":1,"version":"1.0.0"},{"appId":2,"envId":1,"version":"1.0.1"}]
     *
     * @param targetsJson JSON 数组字符串
     *
     * @return 目标列表
     */
    List<ConfPublishTargetForm> parseTargets(String targetsJson);

    /**
     * 公共发布
     * <p/>
     * 将源配置的值同步到每一个目标:
     * - 目标配置存在: 覆盖更新值 + 写历史 + 通知 ZooKeeper
     * - 目标配置不存在且 createIfAbsent=true: 自动创建 + 写历史 + 通知 ZooKeeper
     * - 目标配置不存在且 createIfAbsent=false: 跳过
     * <p/>
     * 单个目标失败不影响其它目标, 结果逐条返回
     *
     * @param sourceConfigId 源配置ID
     * @param targets        目标列表
     * @param createIfAbsent 目标不存在时是否自动创建
     *
     * @return 每个目标的发布结果
     */
    List<ConfigPublishResultVo> publishToTargets(Long sourceConfigId, List<ConfPublishTargetForm> targets,
                                                 boolean createIfAbsent);
}
