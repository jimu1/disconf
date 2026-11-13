package com.baidu.disconf.web.service.dnslog.dao.impl;

import org.springframework.stereotype.Service;

import com.baidu.disconf.web.service.dnslog.bo.DnslogConfig;
import com.baidu.disconf.web.service.dnslog.dao.DnslogConfigDao;
import com.baidu.dsp.common.dao.AbstractDao;
import com.baidu.dsp.common.dao.Columns;
import com.baidu.unbiz.common.genericdao.operator.Match;

/**
 * @author pi
 * @version 2024-11-13
 */
@Service
public class DnslogConfigDaoImpl extends AbstractDao<Long, DnslogConfig> implements DnslogConfigDao {

    @Override
    public DnslogConfig getByName(String name) {

        return findOne(new Match(Columns.NAME, name));
    }
}
