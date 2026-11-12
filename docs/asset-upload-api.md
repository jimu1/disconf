# 资产上传接口说明

支持将本地资产文件上传到 disconf-web 服务器，文件实体保存在服务器本地磁盘，元信息保存在 `asset` 表。

## 部署步骤

1. 执行建表 SQL：

   ```bash
   mysql -u root -p disconf < sql/asset.sql
   ```

2. （可选）在 `application.properties` 中配置：

   ```properties
   # 资产文件保存目录（默认为工作目录下 asset/）
   disconf.web.asset.dir=/data/disconf/asset
   # 单个资产大小上限，默认 33554432（32MB）
   disconf.web.asset.max.size=33554432
   ```

3. 重启 disconf-web。

## 接口列表

所有接口均需要登录态（Cookie `XID`）。返回格式为 disconf 标准 JSON（`success` / `message` / `data`）。

### 1. 上传本地资产

```
POST /api/web/asset/upload
Content-Type: multipart/form-data
```

| 参数   | 类型 | 必填 | 说明                       |
|--------|------|------|----------------------------|
| file   | file | 是   | 本地资产文件               |
| name   | text | 否   | 展示名，默认原始文件名     |
| app    | text | 否   | 归属 APP 名                |
| desc   | text | 否   | 描述                       |

curl 示例：

```bash
curl -X POST 'http://localhost:8085/api/web/asset/upload' \
     -b 'XID=<登录Cookie>' \
     -F 'file=@/Users/me/logo.png' \
     -F 'name=首页Logo' \
     -F 'app=myApp' \
     -F 'desc=静态资源'
```

返回示例：

```json
{
  "success": "true",
  "message": "success",
  "data": {
    "asset": {
      "id": 1,
      "name": "首页Logo",
      "originalFileName": "logo.png",
      "fileSize": 10240,
      "fileType": ".png",
      "app": "myApp",
      "desc": "静态资源",
      "owner": "admin",
      "createTime": "20241112160000",
      "downloadUrl": "/api/web/asset/download?id=1"
    }
  }
}
```

### 2. 资产列表

```
GET /api/web/asset/list
```

### 3. 资产详情

```
GET /api/web/asset/get?id={id}
```

### 4. 下载资产

```
GET /api/web/asset/download?id={id}
```

### 5. 删除资产

```
POST /api/web/asset/delete?id={id}
```

## 安全设计

- 上传、下载、删除均需登录态，受 `LoginInterceptor` 保护。
- 服务器端生成存储文件名（UUID + 后缀），与用户输入完全隔离，杜绝路径穿越。
- 原始文件名入库前会剥离所有路径成分并清理控制字符。
- 单文件大小上限默认 32MB，可通过 `disconf.web.asset.max.size` 调整。
- 下载时 `Content-Disposition` 使用 RFC 5987 编码文件名，防止响应拆分。

## 相关代码

```
disconf-web/src/main/java/com/baidu/disconf/web/service/asset/   # BO/DAO/Form/VO/Service
disconf-web/src/main/java/com/baidu/disconf/web/web/asset/controller/AssetController.java  # 接口
sql/asset.sql                                                    # 建表 SQL
```
