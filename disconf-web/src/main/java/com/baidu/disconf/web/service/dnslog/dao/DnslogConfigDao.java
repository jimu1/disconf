package com.baidu.disconf.web.service.dnslog.dao;

import com.baidu.disconf.web.service.dnslog.bo.DnslogConfig;
import com.baidu.unbiz.common.genericdao.dao.BaseDao;

/**
 * @author pi
 * @version 2024-11-13
 */
public interface DnslogConfigDao extends BaseDao<Long, DnslogConfig> {

    /**
     * 按配置名称查询
     */
    DnslogConfig getByName(String name);
}
