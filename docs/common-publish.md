# 公共发布功能

## 背景

多个应用/环境/版本经常共用同一份公共配置(如 `redis.properties`、某个开关 item)。原生 disconf 只能逐个打开每个配置页修改,效率低且容易漏改。

**公共发布**解决此问题:在一处选中"源配置",一次性将其值批量发布到多个目标(应用 + 环境 + 版本),并立即推送给所有客户端。

## 功能说明

- **发布粒度**: 配置项或配置文件,以源配置的 `key`(文件名或配置项名)为准,目标处按同名、同类型匹配。
- **目标已存在同名配置**: 覆盖更新值,写入 `config_history` 历史,并通知 ZooKeeper 实时推送。
- **目标不存在同名配置**:
  - `createIfAbsent=true`: 自动创建(值 = 源配置值),写历史并通知 ZooKeeper;
  - `createIfAbsent=false`(默认): 跳过,结果中标记 `skip`。
- **目标即源配置本身**: 自动跳过。
- **单个目标失败不影响其它目标**,逐条返回结果。
- **权限校验**: 当前用户必须同时具有源配置所属 app 和每个目标 app 的权限。
- **邮件通知**: 开启邮件监控时,发布完成后向源 app 及所有目标 app 的邮箱汇总发送一封通知邮件。
- **数据编码**: 目标值直接写入源配置在 DB 中的 unicode 编码值,与单条修改的存储格式完全一致。

## API

```
POST /api/web/config/publish/common
```

| 参数 | 必填 | 说明 |
| ---- | ---- | ---- |
| `configId` | 是 | 源配置 ID |
| `targets` | 是 | 目标列表, JSON 数组字符串 |
| `createIfAbsent` | 否 | 目标不存在时是否自动创建, 默认 `false` |

`targets` 示例:

```json
[
  {"appId": 2,  "envId": 1, "version": "1.0.0"},
  {"appId": 3,  "envId": 1, "version": "1.0.0"},
  {"appId": 99, "envId": 2, "version": "2.0.0"}
]
```

### 请求示例

```bash
curl -X POST 'http://127.0.0.1:8015/api/web/config/publish/common' \
  -b 'JSESSIONID=xxx' \
  -d 'configId=123' \
  -d 'createIfAbsent=true' \
  --data-urlencode 'targets=[{"appId":2,"envId":1,"version":"1.0.0"},{"appId":3,"envId":1,"version":"1.0.0"}]'
```

### 返回示例

```json
{
  "success": true,
  "page": {
    "totalCounts": 2
  },
  "result": [
    {
      "appId": 2, "appName": "appA", "envId": 1, "envName": "rd",
      "version": "1.0.0", "key": "redis.properties",
      "targetConfigId": 456, "result": "success", "message": "发布成功(覆盖更新)"
    },
    {
      "appId": 3, "appName": "appB", "envId": 1, "envName": "rd",
      "version": "1.0.0", "key": "redis.properties",
      "targetConfigId": 789, "result": "success", "message": "发布成功(自动新建)"
    }
  ]
}
```

`result` 取值: `success` / `skip` / `fail`。

### 错误码(message key)

| key | 说明 |
| --- | --- |
| `config.not.exist` | 源配置不存在 |
| `publish.targets.empty` | 目标列表为空 |
| `publish.targets.invalid` | 目标不完整(缺少 appId/envId/version) |
| `publish.targets.parse.error` | targets JSON 解析失败 |
| `app.not.exist` / `env.not.exist` / `version.not.exist` | 目标参数非法 |
| `app.auth.noright` | 无源 app 或目标 app 权限 |

## 代码结构

```
disconf-web/src/main/java/com/baidu/disconf/web/
├── service/config/
│   ├── form/ConfPublishTargetForm.java                  # 发布目标(appId/envId/version)
│   ├── vo/ConfigPublishResultVo.java                    # 单个目标的发布结果
│   └── service/
│       ├── ConfigPublishMgr.java                        # 公共发布接口
│       └── impl/ConfigPublishMgrImpl.java               # 实现(更新/新建/通知ZK/汇总邮件)
└── web/config/
    ├── controller/ConfigPublishController.java          # REST API
    └── validator/ConfigValidator.java                   # 新增 validateCommonPublish 校验

disconf-web/src/main/resources/messages/config-controller-info.properties   # 新增错误提示
disconf-web/src/test/java/com/baidu/disconf/web/test/service/
    ConfigPublishMgrParseTargetsTestCase.java            # 目标解析单测
```

## 使用建议

1. 建议在"修改配置"页完成修改并验证后,再执行公共发布,保证源配置是最新已验证的值。
2. 跨环境(如 rd → online)发布请确认权限已打通,并在邮件通知开启的情况下关注汇总结果中的 `skip`/`fail` 条目。
3. 前端如需增加"公共发布"按钮,可在 `disconf-web/html` 中调用上述 API 即可,后端已支持纯 JSON 交互。
