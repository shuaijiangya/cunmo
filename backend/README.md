# 存魔 Java 微信登录后端

后端统一使用 Java 21、Spring Boot、MyBatis-Plus、Sa-Token 与 MySQL，
采用 DDD 模块化单体架构。
启动服务前先执行 `mysql/001_wechat_users.sql`。

## 工程模块

```text
java
├── cunmo-api             # HTTP 请求与响应契约
├── cunmo-domain          # 用户聚合、角色权限模型和领域端口
├── cunmo-application     # 微信登录应用用例与 Token 端口
├── cunmo-infrastructure  # MySQL、微信 API、Sa-Token 实现
├── cunmo-trigger         # Controller 与统一异常处理
└── cunmo-bootstrap       # Spring Boot 启动和运行配置
```

所有 Java 业务包均以 `cn.cunmo` 开始。领域模块不依赖 Spring、
MyBatis、Sa-Token 或微信 HTTP 实现。

数据库初始化顺序：

1. `mysql/001_wechat_users.sql`
2. `mysql/002_rbac_seed.sql`
3. `mysql/003_inventory_domain.sql`

本地需要验证透视镜和流转轴分页时，先登录并调用一次
`GET /api/inventory/bootstrap`，再执行
`mysql/004_inventory_seed.sql`。该脚本只用于开发环境，可重复执行。

## 库存接口

- `GET /api/inventory/bootstrap`：初始化并返回默认魔方域、空间、分类和容量
- `POST /api/inventory/spaces`：创建空间
- `POST /api/inventory/categories`：创建分类并绑定多个空间
- `PUT /api/inventory/categories/{categoryId}/spaces`：更新分类空间绑定
- `GET /api/inventory/items`：按空间、分类、关键词和游标查询物品
- `POST /api/inventory/items`：创建物品，普通用户每个空间分类组合最多 20 个
- `POST /api/inventory/items/{itemId}/adjustments`：增减库存并同步写入流水
- `GET /api/inventory/analytics`：按空间或分类查询透视统计
- `GET /api/inventory/transactions`：按游标查询不可变库存流水

所有库存接口均使用当前登录用户的 `vault_id` 隔离数据。表之间只保存关联
主键和索引，不创建数据库外键；关联有效性由应用事务和领域规则保证。

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

## 环境变量

Java 服务：

```dotenv
WECHAT_APP_ID=wx_your_appid
WECHAT_APP_SECRET=replace_me
DATABASE_URL=jdbc:mysql://127.0.0.1:3306/cunmo
DATABASE_USERNAME=cunmo
DATABASE_PASSWORD=replace_me
TOKEN_TTL_SECONDS=7200
```

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
