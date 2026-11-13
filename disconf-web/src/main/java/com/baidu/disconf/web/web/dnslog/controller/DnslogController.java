package com.baidu.disconf.web.web.dnslog.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.baidu.disconf.web.service.dnslog.form.DnslogSaveForm;
import com.baidu.disconf.web.service.dnslog.service.DnslogMgr;
import com.baidu.disconf.web.service.dnslog.vo.DnslogConfigVo;
import com.baidu.disconf.web.service.dnslog.vo.DnslogRecordVo;
import com.baidu.disconf.web.service.dnslog.vo.DnslogTokenVo;
import com.baidu.disconf.web.service.role.bo.RoleEnum;
import com.baidu.disconf.web.service.user.dto.Visitor;
import com.baidu.dsp.common.constant.WebConstants;
import com.baidu.dsp.common.controller.BaseController;
import com.baidu.dsp.common.exception.AccessDeniedException;
import com.baidu.dsp.common.vo.JsonObjectBase;
import com.baidu.ub.common.commons.ThreadContext;

/**
 * DNSLog 配置管理 Controller：安全测试带外（OOB）检测辅助
 * <p/>
 * 接口列表：
 * - GET  /api/web/dnslog/list    配置列表（apiToken 脱敏）
 * - GET  /api/web/dnslog/get     配置详情（apiToken 脱敏）
 * - POST /api/web/dnslog/save    新建/更新配置（仅管理员）
 * - POST /api/web/dnslog/delete  删除配置（仅管理员）
 * - GET  /api/web/dnslog/token   生成随机 payload 子域名
 * - GET  /api/web/dnslog/check   查询 token 的 DNS 解析记录
 * <p/>
 * 安全设计说明：
 * 1. 认证：/api/web/dnslog/** 默认被 LoginInterceptor 拦截，未登录请求直接拒绝；
 * 2. 授权：配置写操作（save/delete）仅管理员（RoleEnum.ADMIN）可用；
 * 3. SSRF 防护：check 出站请求仅允许 http/https 公网地址、逐 IP 校验、
 *    禁重定向、固定超时、限制响应大小（见 DnslogMgrImpl）；
 * 4. 敏感信息：apiToken 出参一律脱敏，不回传明文；
 * 5. 审计：写操作与出站查询记录专用 SECURITY_AUDIT 审计日志（CRLF 已剥离）。
 *
 * @author pi
 * @version 2024-11-13
 */
@Controller
@RequestMapping(WebConstants.API_PREFIX + "/web/dnslog")
public class DnslogController extends BaseController {

    protected static final Logger LOG = LoggerFactory.getLogger(DnslogController.class);

    /** 专用安全审计日志（独立 category，便于采集与检索） */
    protected static final Logger SEC_AUDIT_LOG = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Autowired
    private DnslogMgr dnslogMgr;

    /**
     * 配置列表
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ResponseBody
    public JsonObjectBase list() {

        getCurrentVisitor();

        List<DnslogConfigVo> list = dnslogMgr.getConfigList();
        return buildListSuccess(list, list.size());
    }

    /**
     * 配置详情
     */
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    @ResponseBody
    public JsonObjectBase get(@RequestParam(value = "id") Long id) {

        getCurrentVisitor();

        DnslogConfigVo vo = dnslogMgr.getConfig(id);
        return buildSuccess("dnslog", vo);
    }

    /**
     * 新建/更新配置（仅管理员）
     *
     * curl 示例:
     * curl -X POST http://host:8085/api/web/dnslog/save \
     *      -b "XID=xxx" \
     *      -d "name=dnslog-cn" \
     *      -d "domain=xxx.dnslog.cn" \
     *      -d "apiUrl=http://www.dnslog.cn/api/sites/{token}" \
     *      -d "enabled=1" -d "description=dnslog.cn 公共平台"
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ResponseBody
    public JsonObjectBase save(DnslogSaveForm form) {

        Visitor visitor = getCurrentVisitor();
        checkAdmin(visitor);

        DnslogConfigVo vo = dnslogMgr.save(form);

        auditLog(visitor, "/api/web/dnslog/save", "dnslog_save",
                "id=" + vo.getId() + ",name=" + vo.getName() + ",domain=" + vo.getDomain());

        return buildSuccess("dnslog", vo);
    }

    /**
     * 删除配置（仅管理员）
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public JsonObjectBase delete(@RequestParam(value = "id") Long id) {

        Visitor visitor = getCurrentVisitor();
        checkAdmin(visitor);

        dnslogMgr.delete(id);

        auditLog(visitor, "/api/web/dnslog/delete", "dnslog_delete", "id=" + id);

        return buildSuccess("删除成功");
    }

    /**
     * 生成随机 payload 子域名（token.domain），
     * 用于嵌入 payload 后通过 check 接口回查是否触发解析
     */
    @RequestMapping(value = "/token", method = RequestMethod.GET)
    @ResponseBody
    public JsonObjectBase token(@RequestParam(value = "name") String name) {

        Visitor visitor = getCurrentVisitor();

        DnslogTokenVo vo = dnslogMgr.genToken(name);

        auditLog(visitor, "/api/web/dnslog/token", "dnslog_gen_token",
                "name=" + name + ",fullDomain=" + vo.getFullDomain());

        return buildSuccess("token", vo);
    }

    /**
     * 查询 token 的 DNS 解析记录：服务端按配置的 API 模板
     * 发起出站请求（带 SSRF 防护），返回平台原始响应
     */
    @RequestMapping(value = "/check", method = RequestMethod.GET)
    @ResponseBody
    public JsonObjectBase check(@RequestParam(value = "name") String name,
                                @RequestParam(value = "token") String token) {

        Visitor visitor = getCurrentVisitor();

        DnslogRecordVo vo = dnslogMgr.checkRecords(name, token);

        auditLog(visitor, "/api/web/dnslog/check", "dnslog_check_records",
                "name=" + name + ",token=" + token + ",status=" + vo.getStatus());

        return buildSuccess("record", vo);
    }

    // ------------------- 私有方法 -------------------

    /**
     * 获取当前会话用户。LoginInterceptor 已保证到达此处必然已登录，
     * 此处判空仅作为纵深防御（defense in depth）。
     */
    private Visitor getCurrentVisitor() {

        Visitor visitor = ThreadContext.getSessionVisitor();
        if (visitor == null) {
            throw new SecurityException("session visitor not found, please login first");
        }
        return visitor;
    }

    /**
     * 管理员校验：配置写操作仅 RoleEnum.ADMIN 可用
     */
    private void checkAdmin(Visitor visitor) {

        if (visitor.getRoleId() != RoleEnum.ADMIN.getValue()) {
            throw new AccessDeniedException("dnslog config write requires admin role, user="
                    + visitor.getLoginUserName());
        }
    }

    /**
     * 安全审计日志：记录操作人、接口、操作类型、关键参数。
     * 不记录 apiToken 明文与平台响应内容。
     * CRLF 注入防护：内容剥离换行符（CWE-117）
     */
    private void auditLog(Visitor visitor, String api, String operateType, String detail) {

        SEC_AUDIT_LOG.info("operateUserid={}, operateFunction={}, operateType={}, detail={}, " +
                        "sensitiveDataType=dnslog",
                stripCrlf(visitor.getLoginUserName()), stripCrlf(api), stripCrlf(operateType),
                stripCrlf(detail));
    }

    /**
     * 剥离回车换行符，防日志伪造
     */
    private String stripCrlf(String value) {

        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\r\\n]", "");
    }
}
