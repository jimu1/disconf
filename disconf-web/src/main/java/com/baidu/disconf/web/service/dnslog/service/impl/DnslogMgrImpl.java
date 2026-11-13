package com.baidu.disconf.web.service.dnslog.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.baidu.disconf.web.service.dnslog.bo.DnslogConfig;
import com.baidu.disconf.web.service.dnslog.dao.DnslogConfigDao;
import com.baidu.disconf.web.service.dnslog.form.DnslogSaveForm;
import com.baidu.disconf.web.service.dnslog.service.DnslogMgr;
import com.baidu.disconf.web.service.dnslog.vo.DnslogConfigVo;
import com.baidu.disconf.web.service.dnslog.vo.DnslogRecordVo;
import com.baidu.disconf.web.service.dnslog.vo.DnslogTokenVo;
import com.baidu.disconf.web.service.user.dto.Visitor;
import com.baidu.dsp.common.constant.DataFormatConstants;
import com.baidu.dsp.common.exception.DocumentNotFoundException;
import com.baidu.dsp.common.exception.FieldException;
import com.baidu.ub.common.commons.ThreadContext;
import com.github.knightliao.apollo.utils.time.DateUtils;

/**
 * DNSLog 平台配置管理实现
 * <p/>
 * 出站请求 SSRF 防护设计（checkRecords）：
 * 1. URL 仅允许 http/https，禁止 userinfo、fragment；
 * 2. 域名解析与建连使用同一份地址结果（自定义 DnsResolver），
 *    每个解析 IP 均校验为公网地址，环回/私网/链路本地/保留段一律拒绝，
 *    降低 DNS rebinding 与重定向绕过风险；
 * 3. 禁止自动重定向、禁止自动重试与自动认证；
 * 4. 连接超时 3s / 读超时 5s，响应体最多读取 64KB，超出截断。
 *
 * @author pi
 * @version 2024-11-13
 */
@Service
@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
public class DnslogMgrImpl implements DnslogMgr {

    protected static final Logger LOG = LoggerFactory.getLogger(DnslogMgrImpl.class);

    @Autowired
    private DnslogConfigDao dnslogConfigDao;

    /** 配置名称：字母数字下划线连字符，1~64 位 */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{1,64}$");

    /** 公网域名格式（非 IP 字面量） */
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("^(?=.{1,253}$)([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$");

    /** payload 子域标签：字母数字连字符点，1~63 位 */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9\\.\\-]{1,63}$");

    /** 禁用的内网/本地域名后缀（小写比较） */
    private static final String[] FORBIDDEN_DOMAIN_SUFFIXES = {
            "localhost", "local", "internal", "intranet", "lan", "corp", "home.arpa"};

    /** 随机 token 字符集 */
    private static final String TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 出站请求：连接超时（毫秒） */
    private static final int HTTP_CONNECT_TIMEOUT_MS = 3000;

    /** 出站请求：读超时（毫秒） */
    private static final int HTTP_SOCKET_TIMEOUT_MS = 5000;

    /** 出站响应体最大读取字节数（64KB） */
    private static final int MAX_RESPONSE_BYTES = 64 * 1024;

    /** 出站请求最终 URL 最大长度 */
    private static final int MAX_URL_LENGTH = 2048;

    // ------------------- 配置 CRUD -------------------

    @Override
    public List<DnslogConfigVo> getConfigList() {

        List<DnslogConfigVo> vos = new ArrayList<DnslogConfigVo>();
        List<DnslogConfig> configs = dnslogConfigDao.findAll();
        for (DnslogConfig config : configs) {
            vos.add(toVo(config));
        }
        return vos;
    }

    @Override
    public DnslogConfigVo getConfig(Long id) {

        return toVo(getConfigOrThrow(id));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public DnslogConfigVo save(DnslogSaveForm form) {

        if (form == null) {
            throw new FieldException("form", "参数不能为空", null);
        }

        // 名称：标识符白名单
        String name = trimToNull(form.getName());
        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new FieldException("name", "配置名称仅允许 1~64 位字母/数字/下划线/连字符", null);
        }

        // 域名：公网域名格式 + 内网后缀黑名单（仅用于拼接 payload，不做服务端请求）
        String domain = trimToNull(form.getDomain());
        if (domain == null || !DOMAIN_PATTERN.matcher(domain).matches()) {
            throw new FieldException("domain", "DNSLog 域名格式不正确（应为公网域名，如 xxx.dnslog.cn）", null);
        }
        String lowerDomain = domain.toLowerCase(Locale.US);
        for (String suffix : FORBIDDEN_DOMAIN_SUFFIXES) {
            if (lowerDomain.equals(suffix) || lowerDomain.endsWith("." + suffix)) {
                throw new FieldException("domain", "不允许配置内网/本地域名", null);
            }
        }

        // 查询 API 模板（可选）：http(s)、含 {token} 占位符、长度受限
        String apiUrl = trimToNull(form.getApiUrl());
        if (apiUrl != null) {
            if (apiUrl.length() > 512) {
                throw new FieldException("apiUrl", "API URL 长度不能超过 512", null);
            }
            if (!apiUrl.contains("{token}")) {
                throw new FieldException("apiUrl", "API URL 模板必须包含 {token} 占位符", null);
            }
            validateApiUrl(apiUrl);
        }

        // 认证 Token（可选）：长度受限、禁止换行（防日志/头注入）
        String apiToken = trimToNull(form.getApiToken());
        if (apiToken != null) {
            if (apiToken.length() > 255) {
                throw new FieldException("apiToken", "API Token 长度不能超过 255", null);
            }
            if (apiToken.matches(".*[\\r\\n].*")) {
                throw new FieldException("apiToken", "API Token 含有非法字符", null);
            }
        }

        // 启用状态归一化
        int enabled = (form.getEnabled() != null && form.getEnabled() == 1) ? 1 : 0;

        // 描述长度受限
        String description = trimToNull(form.getDescription());
        if (description != null && description.length() > 512) {
            throw new FieldException("description", "描述长度不能超过 512", null);
        }

        // 名称唯一性
        DnslogConfig exist = dnslogConfigDao.getByName(name);
        if (form.getId() == null) {
            if (exist != null) {
                throw new FieldException("name", "配置名称已存在: " + name, null);
            }
        } else {
            if (exist != null && !exist.getId().equals(form.getId())) {
                throw new FieldException("name", "配置名称已存在: " + name, null);
            }
        }

        String curTime = DateUtils.format(new Date(), DataFormatConstants.COMMON_TIME_FORMAT);

        DnslogConfig config;
        if (form.getId() == null) {
            config = new DnslogConfig();
            config.setOwner(getCurrentUserName());
            config.setCreateTime(curTime);
        } else {
            config = getConfigOrThrow(form.getId());
        }

        config.setName(name);
        config.setDomain(domain);
        config.setApiUrl(apiUrl == null ? "" : apiUrl);
        config.setApiToken(apiToken == null ? "" : apiToken);
        config.setEnabled(enabled);
        config.setDescription(description == null ? "" : description);
        config.setUpdateTime(curTime);

        if (form.getId() == null) {
            config = dnslogConfigDao.create(config);
        } else {
            dnslogConfigDao.update(config);
        }

        LOG.info("dnslog config saved: " + config.toString());

        return toVo(config);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void delete(Long id) {

        DnslogConfig config = getConfigOrThrow(id);
        dnslogConfigDao.delete(id);
        LOG.info("dnslog config deleted: " + config.toString());
    }

    // ------------------- token 生成与记录查询 -------------------

    @Override
    public DnslogTokenVo genToken(String name) {

        DnslogConfig config = getEnabledConfigOrThrow(name);

        // SecureRandom 生成 12 位小写字母数字子域标签
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(TOKEN_CHARS.charAt(SECURE_RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        String token = sb.toString();

        DnslogTokenVo vo = new DnslogTokenVo();
        vo.setToken(token);
        vo.setDomain(config.getDomain());
        vo.setFullDomain(token + "." + config.getDomain());
        return vo;
    }

    @Override
    public DnslogRecordVo checkRecords(String name, String token) {

        DnslogConfig config = getEnabledConfigOrThrow(name);

        if (token == null || !TOKEN_PATTERN.matcher(token).matches()) {
            throw new FieldException("token", "token 仅允许 1~63 位字母/数字/连字符/点", null);
        }
        if (config.getApiUrl() == null || config.getApiUrl().isEmpty()) {
            throw new FieldException("name", "该配置未设置记录查询 API（apiUrl）", null);
        }

        // 占位符替换：值均做 URL 编码，防注入到 URL
        String apiToken = config.getApiToken() == null ? "" : config.getApiToken();
        String url = config.getApiUrl()
                .replace("{api_token}", urlEncode(apiToken))
                .replace("{token}", urlEncode(token));

        // 最终 URL 再校验一次（格式 + 长度）
        validateApiUrl(url);
        if (url.length() > MAX_URL_LENGTH) {
            throw new FieldException("apiUrl", "最终请求 URL 过长", null);
        }

        DnslogRecordVo vo = new DnslogRecordVo();
        vo.setConfigName(config.getName());
        vo.setToken(token);
        vo.setRequestUrl(maskUrlToken(url, apiToken));
        vo.setStatus(-1);

        // 出站查询（SSRF 防护见类注释）
        CloseableHttpClient client = buildHttpClient();
        try {
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept", "*/*");
            get.addHeader("User-Agent", "disconf-dnslog-checker/1.0");

            HttpResponse response = client.execute(get);
            vo.setStatus(response.getStatusLine().getStatusCode());

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                readEntity(entity, vo);
            }
        } catch (Exception e) {
            LOG.warn("dnslog check failed: " + e.toString());
            vo.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                LOG.warn("close http client error: " + e.toString());
            }
        }

        return vo;
    }

    // ------------------- 私有方法 -------------------

    private DnslogConfig getConfigOrThrow(Long id) {

        DnslogConfig config = dnslogConfigDao.get(id);
        if (config == null) {
            throw new DocumentNotFoundException("dnslog config " + id);
        }
        return config;
    }

    private DnslogConfig getEnabledConfigOrThrow(String name) {

        if (name == null || !NAME_PATTERN.matcher(name).matches()) {
            throw new FieldException("name", "配置名称不合法", null);
        }
        DnslogConfig config = dnslogConfigDao.getByName(name);
        if (config == null) {
            throw new DocumentNotFoundException("dnslog config " + name);
        }
        if (config.getEnabled() == null || config.getEnabled() != 1) {
            throw new FieldException("name", "配置已停用: " + name, null);
        }
        return config;
    }

    /**
     * API URL 校验：仅 http/https，禁 userinfo/fragment，host 非空
     */
    private void validateApiUrl(String apiUrl) {

        try {
            URI uri = new URI(apiUrl);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.US);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new FieldException("apiUrl", "API URL 仅允许 http/https 协议", null);
            }
            if (uri.getUserInfo() != null) {
                throw new FieldException("apiUrl", "API URL 不允许携带 userinfo", null);
            }
            if (uri.getFragment() != null) {
                throw new FieldException("apiUrl", "API URL 不允许携带 fragment", null);
            }
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                throw new FieldException("apiUrl", "API URL 缺少主机名", null);
            }
        } catch (URISyntaxException e) {
            throw new FieldException("apiUrl", "API URL 格式不正确", e);
        }
    }

    /**
     * 构造带 SSRF 校验的 HTTP 客户端：
     * 解析与建连共用同一 DnsResolver 结果，逐 IP 校验为公网地址；
     * 禁重定向、禁自动重试；固定超时。
     */
    private CloseableHttpClient buildHttpClient() {

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();

        BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager(
                registry, null, null, new PublicOnlyDnsResolver());

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS)
                .setSocketTimeout(HTTP_SOCKET_TIMEOUT_MS)
                .setRedirectsEnabled(false)
                .setRelativeRedirectsAllowed(false)
                .setAuthenticationEnabled(false)
                .build();

        return HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries()
                .setMaxConnTotal(1)
                .build();
    }

    /**
     * 读取响应体，最多 MAX_RESPONSE_BYTES，超出置 truncated
     */
    private void readEntity(HttpEntity entity, DnslogRecordVo vo) throws IOException {

        InputStream in = entity.getContent();
        if (in == null) {
            return;
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int total = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                if (total + n > MAX_RESPONSE_BYTES) {
                    int remain = MAX_RESPONSE_BYTES - total;
                    if (remain > 0) {
                        bos.write(buf, 0, remain);
                    }
                    vo.setTruncated(true);
                    break;
                }
                bos.write(buf, 0, n);
                total += n;
            }
            vo.setContent(new String(bos.toByteArray(), "UTF-8"));
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                LOG.warn("close response stream error: " + e.toString());
            }
        }
    }

    /**
     * 回显 URL 中隐藏认证参数值
     */
    private String maskUrlToken(String url, String apiToken) {

        if (apiToken == null || apiToken.isEmpty()) {
            return url;
        }
        return url.replace(urlEncode(apiToken), "****");
    }

    private String urlEncode(String value) {

        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
     * apiToken 出参脱敏：保留首尾各 4 位
     */
    private String maskApiToken(String apiToken) {

        if (apiToken == null || apiToken.isEmpty()) {
            return "";
        }
        if (apiToken.length() <= 8) {
            return "****";
        }
        return apiToken.substring(0, 4) + "****" + apiToken.substring(apiToken.length() - 4);
    }

    private DnslogConfigVo toVo(DnslogConfig config) {

        DnslogConfigVo vo = new DnslogConfigVo();
        vo.setId(config.getId());
        vo.setName(config.getName());
        vo.setDomain(config.getDomain());
        vo.setApiUrl(config.getApiUrl());
        vo.setMaskedApiToken(maskApiToken(config.getApiToken()));
        vo.setEnabled(config.getEnabled() != null && config.getEnabled() == 1);
        vo.setDescription(config.getDescription());
        vo.setOwner(config.getOwner());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }

    private String trimToNull(String s) {

        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String getCurrentUserName() {

        try {
            Visitor visitor = ThreadContext.getSessionVisitor();
            if (visitor != null && visitor.getLoginUserName() != null) {
                return visitor.getLoginUserName();
            }
        } catch (Exception e) {
            LOG.warn("cannot get current user: " + e.toString());
        }
        return "unknown";
    }

    /**
     * 仅放行公网地址的 DNS 解析器：
     * 建连前对全部解析结果做公网校验，环回/私网/链路本地/保留段直接失败，
     * 且校验与建连使用同一份解析结果，缓解 DNS rebinding。
     */
    static class PublicOnlyDnsResolver implements DnsResolver {

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {

            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (!isPublicAddress(address)) {
                    throw new UnknownHostException(
                            "blocked non-public address for host '" + host + "'");
                }
            }
            return addresses;
        }

        /**
         * 是否公网单播地址（拒绝环回/私网/链路本地/组播/保留段）
         */
        static boolean isPublicAddress(InetAddress address) {

            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                return false;
            }

            byte[] b = address.getAddress();
            if (b.length == 4) {
                return isPublicIpv4(b);
            }

            // IPv4-mapped IPv6（::ffff:a.b.c.d）：按内嵌 IPv4 判断
            boolean mapped = true;
            for (int i = 0; i < 10; i++) {
                if (b[i] != 0) {
                    mapped = false;
                    break;
                }
            }
            if (mapped && b[10] == (byte) 0xff && b[11] == (byte) 0xff) {
                byte[] v4 = new byte[4];
                System.arraycopy(b, 12, v4, 0, 4);
                return isPublicIpv4(v4);
            }

            // 唯一本地 fc00::/7
            if ((b[0] & 0xfe) == 0xfc) {
                return false;
            }

            // IPv4 兼容格式（::a.b.c.d，已废弃）与未指定地址
            boolean leadingZero = true;
            for (int i = 0; i < 12; i++) {
                if (b[i] != 0) {
                    leadingZero = false;
                    break;
                }
            }
            if (leadingZero) {
                return false;
            }

            return true;
        }

        /**
         * IPv4 公网判断：拒绝 0/8、10/8、100.64/10、127/8、169.254/16、
         * 172.16/12、192.0.0/24、192.0.2/24（TEST-NET-1）、192.168/16、
         * 198.18/15、198.51.100/24（TEST-NET-2）、203.0.113/24（TEST-NET-3）、
         * 224/4（组播）、240/4 及 255（保留/广播）
         */
        static boolean isPublicIpv4(byte[] b) {

            int first = b[0] & 0xff;
            int second = b[1] & 0xff;
            int third = b[2] & 0xff;

            if (first == 0 || first == 10 || first == 127) {
                return false;
            }
            if (first == 100 && (second & 0xc0) == 64) {
                return false;
            }
            if (first == 169 && second == 254) {
                return false;
            }
            if (first == 172 && (second & 0xf0) == 16) {
                return false;
            }
            if (first == 192) {
                if (second == 168) {
                    return false;
                }
                if (second == 0 && (third == 0 || third == 2)) {
                    return false;
                }
            }
            if (first == 198) {
                if ((second & 0xfe) == 18) {
                    return false;
                }
                if (second == 51 && third == 100) {
                    return false;
                }
            }
            if (first == 203 && second == 0 && third == 113) {
                return false;
            }
            if (first >= 224) {
                return false;
            }
            return true;
        }
    }
}
