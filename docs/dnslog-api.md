# DNSLog 配置管理接口

安全测试带外（OOB）检测辅助功能：管理 DNSLog 平台配置（域名 / 记录查询 API / 认证 Token），
生成随机 payload 子域名，并代查解析记录。

## 表结构

执行 `sql/dnslog.sql` 创建 `dnslog_config` 表。

## 接口列表

所有接口均需登录会话（Cookie `XID`），配置写操作（save/delete）需管理员角色（RoleEnum.ADMIN）。

| 方法 | 路径 | 说明 | 权限 |
| ---- | ---- | ---- | ---- |
| GET  | /api/web/dnslog/list    | 配置列表（apiToken 脱敏）        | 登录用户 |
| GET  | /api/web/dnslog/get     | 配置详情（apiToken 脱敏）        | 登录用户 |
| POST | /api/web/dnslog/save    | 新建/更新配置                    | 管理员 |
| POST | /api/web/dnslog/delete  | 删除配置                         | 管理员 |
| GET  | /api/web/dnslog/token   | 生成随机 payload 子域名          | 登录用户 |
| GET  | /api/web/dnslog/check   | 查询 token 的 DNS 解析记录       | 登录用户 |

## 使用示例

### 1. 新建配置（管理员）

```bash
curl -X POST http://host:8085/api/web/dnslog/save \
     -b "XID=xxx" \
     -d "name=dnslog-cn" \
     -d "domain=abc123.dnslog.cn" \
     -d "apiUrl=http://www.dnslog.cn/api/sites/{token}" \
     -d "enabled=1" \
     -d "description=dnslog.cn 公共平台"
```

`apiUrl` 为记录查询 API 模板，占位符：

- `{token}`：必填占位符，替换为 payload 子域标签（URL 编码后替换）
- `{api_token}`：可选占位符，替换为该配置的认证 Token

示例（ceye.io 风格）：
`http://api.ceye.io/v1/records?token={api_token}&type=dns&filter={token}`

更新时额外传 `id` 参数即可。

### 2. 生成 payload 子域名

```bash
curl -b "XID=xxx" "http://host:8085/api/web/dnslog/token?name=dnslog-cn"
```

返回：

```json
{
  "success": true,
  "message": {
    "token": {
      "token": "3f9a2c8d1b7e",
      "domain": "abc123.dnslog.cn",
      "fullDomain": "3f9a2c8d1b7e.abc123.dnslog.cn"
    }
  }
}
```

将 `fullDomain` 嵌入 payload（如 SSRF/RCE 盲检测场景）后触发目标行为。

### 3. 回查解析记录

```bash
curl -b "XID=xxx" "http://host:8085/api/web/dnslog/check?name=dnslog-cn&token=3f9a2c8d1b7e"
```

服务端按配置的 `apiUrl` 模板发起出站请求，返回平台原始响应（最多 64KB，超出截断）：

```json
{
  "success": true,
  "message": {
    "record": {
      "configName": "dnslog-cn",
      "token": "3f9a2c8d1b7e",
      "requestUrl": "http://www.dnslog.cn/api/sites/3f9a2c8d1b7e",
      "status": 200,
      "content": "...平台原始响应...",
      "truncated": false,
      "error": null
    }
  }
}
```

`status` 为 -1 且 `error` 非空表示出站请求失败（超时 3s/5s、被 SSRF 防护拦截等）。

## 安全设计说明

1. **认证**：`/api/web/dnslog/**` 默认被 LoginInterceptor 拦截，未登录请求直接拒绝；
2. **授权**：save/delete 仅管理员（RoleEnum.ADMIN）可用，其余接口登录用户可用；
3. **SSRF 防护**（check 出站请求）：
   - API URL 仅允许 http/https，禁止 userinfo/fragment，必须含 `{token}` 占位符；
   - 域名解析与建连共用同一 DnsResolver，逐 IP 校验为公网地址：
     环回(127/8)、私网(10/8、172.16/12、192.168/16)、链路本地(169.254/16)、
     CGNAT(100.64/10)、TEST-NET、组播、保留段、IPv6 ULA(fc00::/7) 等一律拒绝，
     缓解 DNS rebinding 绕过；
   - 禁止自动重定向、自动重试与自动认证；连接超时 3s、读超时 5s；
   - 响应体最多读取 64KB，超出截断；
   - HTTPS 保持默认证书与主机名校验；
4. **域名防滥用**：domain 仅允许公网域名格式，黑名单拒绝
   localhost/.local/.internal/.intranet/.lan/.corp/.home.arpa 等内网后缀；
5. **敏感信息**：apiToken 出参脱敏（首尾各 4 位），审计日志与回显 URL 不落明文；
6. **审计**：save/delete/token/check 均写专用 SECURITY_AUDIT 审计日志
   （操作人/接口/类型/关键参数，CRLF 已剥离，CWE-117）；
7. **token 生成**：SecureRandom 生成 12 位小写字母数字子域标签。
