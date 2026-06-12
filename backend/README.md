# 存魔 Java 后端

后端统一使用 Java 21、Spring Boot、MyBatis-Plus、Sa-Token 与 MySQL，
采用 DDD 模块化单体架构。
启动服务前按顺序执行数据库迁移。

## 工程模块

```text
java
├── cunmo-api             # HTTP 请求与响应契约
├── cunmo-domain          # 用户、库存、会员聚合和领域端口
├── cunmo-application     # 登录、库存、会员用例与应用端口
├── cunmo-infrastructure  # MySQL、微信登录/支付、Sa-Token 实现
├── cunmo-trigger         # Controller 与统一异常处理
└── cunmo-bootstrap       # Spring Boot 启动和运行配置
```

所有 Java 业务包均以 `cn.cunmo` 开始。领域模块不依赖 Spring、
MyBatis、Sa-Token 或微信 HTTP 实现。

数据库初始化顺序：

1. `mysql/001_wechat_users.sql`
2. `mysql/002_rbac_seed.sql`
3. `mysql/003_inventory_domain.sql`
4. `mysql/005_inventory_deletion.sql`
5. `mysql/006_membership_quota.sql`

本地需要验证透视镜和流转轴分页时，先登录并调用一次
`GET /api/inventory/bootstrap`，再执行
`mysql/004_inventory_seed.sql`。该脚本只用于开发环境，可重复执行。

## 库存接口

- `GET /api/inventory/bootstrap`：初始化并返回默认魔方域、空间、分类和容量
- `POST /api/inventory/spaces`：创建空间
- `POST /api/inventory/categories`：创建分类并绑定多个空间
- `PUT /api/inventory/categories/{categoryId}/spaces`：更新分类空间绑定
- `GET /api/inventory/items`：按空间、分类、关键词和游标查询物品
- `POST /api/inventory/items`：创建物品，FREE 用户每个空间分类组合最多 10 条记录
- `POST /api/inventory/items/{itemId}/adjustments`：增减库存并同步写入流水
- `GET /api/inventory/analytics`：按空间或分类查询透视统计
- `GET /api/inventory/transactions`：按游标查询不可变库存流水

所有库存接口均使用当前登录用户的 `vault_id` 隔离数据。表之间只保存关联
主键和索引，不创建数据库外键；关联有效性由应用事务和领域规则保证。

FREE 用户最多创建 3 个根空间、每个空间最多绑定 3 个分类、每个
“空间 + 分类”腔体最多保存 10 条有效物品记录。月度或永久 PRO 不受这三项限制。

## 会员与配额接口

- `GET /api/membership/dashboard`：返回权益、配额、套餐、续费和联系方式
- `POST /api/membership/orders`：创建 9.9 元月卡或 69 元永久会员订单
- `GET /api/membership/orders/{orderNo}`：查询权威支付状态
- `POST /api/membership/renewal-agreements`：创建委托代扣签约参数
- `POST /api/membership/renewal-agreements/terminate`：终止自动续费
- `POST /api/membership/upgrade-requests`：提交联系管理员升级申请

微信回调：

- `POST /api/wechat-pay/payment-notify`：JSAPI API v3 JSON 回调
- `POST /api/wechat-pay/contract-notify`：委托代扣 V2 XML 签约回调
- `POST /api/wechat-pay/renewal-notify`：委托代扣 V2 XML 扣款回调

主动支付使用微信支付 API v3 RSA 验签与 AES-GCM 解密。普通商户委托代扣使用
V2 XML 和 `HMAC-SHA256`，小程序通过微信签约小程序完成签约。两类回调都会核对
`appid`、商户号、订单金额和本地业务状态，并通过通知表与订单状态双重幂等。

月卡每次增加 30 天，未过期续费从原到期时间顺延。到期时读取立即按 FREE
处理，定时任务按批次将数据库权益收敛为 FREE。永久会员生效后会终止自动续费。

管理员审批接口位于 `/api/admin/membership/**`，需要
`membership:upgrade:review` 权限。本期只提供 API，不包含管理端页面。

## 登录协议

`POST /api/auth/wechat/login`

请求：

```json
{ "code": "wx.login 返回的一次性临时凭证" }
```

成功响应：

```json
{
  "token": "业务登录态",
  "expiresIn": 7200,
  "user": {
    "id": 10001,
    "nickname": null,
    "avatarUrl": null,
    "profileCompleted": false,
    "roles": ["USER"],
    "permissions": ["inventory:item:read"]
  }
}
```

错误响应：

```json
{ "code": "INVALID_CODE", "message": "登录凭证无效或已过期" }
```

稳定错误码：

- `INVALID_REQUEST`: 请求缺少 code
- `INVALID_CODE`: 微信拒绝或 code 已使用、过期
- `WECHAT_TIMEOUT`: 微信服务超时
- `WECHAT_UNAVAILABLE`: 微信服务异常
- `WECHAT_RATE_LIMITED`: 微信登录调用频率超限
- `WECHAT_RISK_CONTROL`: 微信安全风控拒绝登录
- `USER_DISABLED`: 用户被禁用
- `INTERNAL_ERROR`: 数据库或服务内部异常

## 退出协议

`POST /api/auth/logout`

请求必须携带当前业务 Token：

```http
Authorization: Bearer <business-token>
```

成功返回 `204 No Content`。服务只注销当前请求携带的 Token，不影响同一用户的其他设备。Sa-Token 配置使用 `is-concurrent: true`、`is-share: false`，保证并发设备使用独立 Token。

无效、过期或已注销 Token 返回：

```json
{ "code": "UNAUTHORIZED", "message": "登录状态已失效，请重新登录" }
```

## 用户资料协议

- `POST /api/users/me/avatar`：使用 multipart 字段 `file` 上传 JPG、PNG 或 WebP 头像，文件最大 2MB
- `PUT /api/users/me/profile`：保存昵称和已上传成功的头像 URL

头像默认写入 `AVATAR_STORAGE_DIRECTORY` 指定的本地目录，并通过 `/uploads/avatars/**` 提供访问。生产进程必须拥有该目录的创建和写入权限；Nginx 同时需要允许 2MB 请求体：

```nginx
client_max_body_size 2m;
```

`server.forward-headers-strategy: native` 配合反向代理传入的 `Host` 和 `X-Forwarded-Proto`，用于让后端生成正确的 HTTPS 头像 URL。它不参与 TLS 握手，也不会导致上传连接在 HTTPS 建立前断开。

## 认证监控

Actuator 暴露 `health`、`info`、`metrics` 和 `prometheus`。认证指标包括：

- `cunmo.auth.login.success`
- `cunmo.auth.login.failure`
- `cunmo.auth.login.duration`
- `cunmo.auth.logout.success`
- `cunmo.auth.logout.failure`
- `cunmo.auth.logout.duration`

失败指标只使用受控 `error_code` 标签，耗时只使用 `outcome=success|failure`。生产环境必须在网关或网络层限制 `/actuator` 访问，不应直接暴露到公网。

Micrometer 默认只在 Java 进程内保存聚合计数和耗时，不写入业务数据库或日志文件。请求次数增加不会产生同等数量的内存记录；但不得为指标增加用户 ID、Token、openid、traceId 或异常消息等高基数标签。应用重启会清空内存指标，长期存储应由 Prometheus 抓取：

```yaml
scrape_configs:
  - job_name: cunmo-backend
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ["127.0.0.1:8080"]
```

## 环境变量

Java 服务：

```dotenv
WECHAT_APP_ID=wx_your_appid
WECHAT_APP_SECRET=replace_me
DATABASE_URL=jdbc:mysql://127.0.0.1:3306/cunmo
DATABASE_USERNAME=cunmo
DATABASE_PASSWORD=replace_me
TOKEN_TTL_SECONDS=7200
MEMBERSHIP_CUSTOMER_SERVICE_ENABLED=false
MEMBERSHIP_ENTERPRISE_WECHAT_QR_URL=
MEMBERSHIP_CONTACT_PHONE=
WECHAT_PAY_ENABLED=false
WECHAT_PAY_MCH_ID=
WECHAT_PAY_MERCHANT_SERIAL_NO=
WECHAT_PAY_PRIVATE_KEY_PATH=
WECHAT_PAY_PLATFORM_CERT_PATH=
WECHAT_PAY_API_V3_KEY=
WECHAT_PAY_API_V2_KEY=
WECHAT_PAY_NOTIFY_URL=https://example.com/api/wechat-pay/payment-notify
WECHAT_PAY_RENEWAL_ENABLED=false
WECHAT_PAY_RENEWAL_PLAN_ID=
WECHAT_PAY_RENEWAL_DISPLAY_ACCOUNT=
WECHAT_PAY_CONTRACT_NOTIFY_URL=https://example.com/api/wechat-pay/contract-notify
WECHAT_PAY_RENEWAL_NOTIFY_URL=https://example.com/api/wechat-pay/renewal-notify
WECHAT_PAY_RENEWAL_CLIENT_IP=<服务端公网出口IP>
WECHAT_PAY_RENEWAL_CHARGE_URL=
WECHAT_PAY_RENEWAL_TERMINATE_URL=
```

委托代扣的扣款和解约 URL 取决于商户实际获批的产品能力，必须填写微信商户平台
提供的正式接口地址；`WECHAT_PAY_RENEWAL_CLIENT_IP` 填服务端公网出口 IP。
任一必需配置缺失时自动续费能力关闭，不影响手动购买。

Java 使用 Sa-Token 配置生成业务 Token；生产集群应按 Sa-Token
官方方式接入 Redis 共享登录态。

本地启动时，在 `backend/java` 目录创建 `.env`：

```dotenv
WECHAT_APP_ID=微信小程序AppID
WECHAT_APP_SECRET=微信公众平台生成的新AppSecret
DATABASE_URL=jdbc:mysql://127.0.0.1:3306/cunmo
DATABASE_USERNAME=cunmo
DATABASE_PASSWORD=数据库密码
TOKEN_TTL_SECONDS=7200
```

应用会加载当前目录或项目根下的 `.env`。如果 AppID、AppSecret 为空或仍是
`${WECHAT_APP_ID}` 形式的占位符，应用将在启动阶段给出明确配置错误，不再等到
WebClient 构建 URI 时抛出模板变量展开异常。

启动类：

```text
cn.cunmo.bootstrap.CunmoApplication
```

## Nginx 反向代理

生产环境至少应传递以下请求头：

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    client_max_body_size 2m;
}
```

使用自定义完整配置启动时，始终传绝对路径，并用同一配置执行校验和重载：

```bash
sudo nginx -t -c /absolute/path/cummo.conf
sudo nginx -c /absolute/path/cummo.conf
sudo nginx -s reload -c /absolute/path/cummo.conf
```

若客户端提示 TLS 尚未建立便断开，问题发生在请求进入 Spring 之前。应检查 `nginx -T`、443 监听进程、证书与私钥匹配情况，以及是否误启动了多个 Nginx 实例。

## 安全边界

- `appid`、`secret` 只能由服务端配置，不能编译进小程序。
- 不向前端返回 `openid`、`unionid` 或 `session_key`。
- 示例不持久化 `session_key`；业务若确有需要，必须加密后保存。
- 日志只记录内部用户 ID、耗时和错误码，不记录 code、Token、openid、
  session_key 或 secret。
- 生产环境必须使用 HTTPS，并为微信请求设置连接和响应超时。

## 日志规范

- `RequestTraceFilter` 为每个请求生成或接收安全格式的 `X-Trace-Id`，
  写入 MDC 并回传响应头。
- 登录链路使用 `event`、`stage`、`userId`、`elapsedMs`、`errorCode`
  等结构化键值记录阶段和耗时。
- 请求进入、用户读取/注册、授权加载、Token 签发和请求完成使用 `INFO`。
- 内部查询和持久化细节使用 `DEBUG`，可恢复异常使用 `WARN`，
  未知系统异常使用 `ERROR`。
- 禁止记录微信 code、openid、session_key、AppSecret 和完整 Token。

## Java 注释规范

- 每个显式方法和构造函数必须提供中文 Javadoc。
- 公共方法说明职责、关键参数和返回值，复杂私有方法说明算法或边界。
- Mapper 方法说明对应的数据操作目的，测试方法说明验证场景。
- 自动生成的 record 访问器和 Java 隐式默认构造函数不重复添加注释。

微信官方参考：

- https://developers.weixin.qq.com/miniprogram/dev/api/open-api/login/wx.login.html
- https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html
- https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/userProfile.html
