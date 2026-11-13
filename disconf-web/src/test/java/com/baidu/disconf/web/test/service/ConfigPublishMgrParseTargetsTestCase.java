package com.baidu.disconf.web.test.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.baidu.disconf.web.service.config.form.ConfPublishTargetForm;
import com.baidu.disconf.web.service.config.service.impl.ConfigPublishMgrImpl;
import com.baidu.dsp.common.exception.FieldException;

/**
 * 公共发布: 目标解析测试
 *
 * @author pi
 */
public class ConfigPublishMgrParseTargetsTestCase {

    private ConfigPublishMgrImpl mgr = new ConfigPublishMgrImpl();

    @Test
    public void testParseEmpty() {

        List<ConfPublishTargetForm> targets = mgr.parseTargets(null);
        Assert.assertNotNull(targets);
        Assert.assertTrue(targets.isEmpty());

        targets = mgr.parseTargets("");
        Assert.assertNotNull(targets);
        Assert.assertTrue(targets.isEmpty());

        targets = mgr.parseTargets("[]");
        Assert.assertNotNull(targets);
        Assert.assertTrue(targets.isEmpty());
    }

    @Test
    public void testParseNormal() {

        List<ConfPublishTargetForm> targets =
                mgr.parseTargets("[{\"appId\":1,\"envId\":2,\"version\":\"1.0.0\"},{\"appId\":3,\"envId\":2,"
                        + "\"version\":\"1.0.1\"}]");

        Assert.assertEquals(2, targets.size());

        ConfPublishTargetForm first = targets.get(0);
        Assert.assertEquals(Long.valueOf(1), first.getAppId());
        Assert.assertEquals(Long.valueOf(2), first.getEnvId());
        Assert.assertEquals("1.0.0", first.getVersion());
        Assert.assertTrue(first.isComplete());

        ConfPublishTargetForm second = targets.get(1);
        Assert.assertEquals(Long.valueOf(3), second.getAppId());
        Assert.assertTrue(second.isComplete());
    }

    @Test
    public void testParseIncomplete() {

        // version 缺失, 视为不完整的目标(由校验层拦截)
        List<ConfPublishTargetForm> targets = mgr.parseTargets("[{\"appId\":1,\"envId\":2}]");
        Assert.assertEquals(1, targets.size());
        Assert.assertFalse(targets.get(0).isComplete());
    }

    @Test
    public void testParseIllegalJson() {

        try {
            mgr.parseTargets("not-a-json");
            Assert.fail("should throw FieldException");
        } catch (FieldException e) {
            // expected
        }
    }
}
