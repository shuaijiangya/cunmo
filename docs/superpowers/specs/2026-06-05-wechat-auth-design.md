# 存魔微信授权登录设计

## 目标

为微信小程序「存魔」提供真实的微信静默登录流程，后端统一使用 Java Spring Boot。数据库使用 MySQL，微信 `appid`、`secret`、数据库密码及 Token 配置均从环境变量或配置中心读取。

## 统一接口

### 登录

`POST /api/auth/wechat/login`

请求：

```json
{
  "code": "wx.login 返回的一次性 code"
}
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
    "profileCompleted": false
  }
}
```

错误响应采用稳定业务错误码：

- `INVALID_CODE`：code 缺失、已使用、过期或微信拒绝。
- `WECHAT_TIMEOUT`：调用微信服务超时。
- `WECHAT_UPSTREAM_ERROR`：微信服务返回其他错误。
- `DATABASE_ERROR`：用户查询或注册失败。
- `INTERNAL_ERROR`：未分类的服务端异常。

### 更新用户资料

`PUT /api/users/me/profile`

请求：

```json
{
  "nickname": "用户选择或输入的昵称",
  "avatarUrl": "头像临时文件上传后的业务地址"
}
```

该接口必须携带业务 Token。小程序使用 `button open-type="chooseAvatar"` 获取头像临时路径，使用 `input type="nickname"` 获取昵称。头像文件应先上传到业务对象存储，数据库只保存业务 URL。

## 登录时序

1. 小程序调用 `wx.login` 获取一次性 `code`。
2. 小程序将 `code` 发送至业务后端。
3. 后端使用服务端配置的 `appid`、`secret` 和 `code` 请求微信 `jscode2session`。
4. 后端校验微信响应，取得 `openid` 与 `session_key`。
5. 后端在 MySQL 中按 `appid + openid` 查询用户；不存在则注册。
6. 后端仅在服务端保存 `session_key`，并生成业务 Token。
7. 小程序保存 Token，后续请求添加 `Authorization: Bearer <token>`。
8. Token 失效时清除缓存并重新走 `wx.login`，不得复用旧 code。

## MySQL 模型

表 `wechat_users`：

- `id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY`
- `appid VARCHAR(64) NOT NULL`
- `openid VARCHAR(128) NOT NULL`
- `unionid VARCHAR(128) NULL`
- `session_key_ciphertext VARCHAR(512) NULL`
- `nickname VARCHAR(64) NULL`
- `avatar_url VARCHAR(512) NULL`
- `status TINYINT NOT NULL DEFAULT 1`
- `last_login_at DATETIME NOT NULL`
- `created_at DATETIME NOT NULL`
- `updated_at DATETIME NOT NULL`
- 唯一索引 `uk_appid_openid(appid, openid)`
- 普通索引 `idx_unionid(unionid)`

`session_key` 属于敏感凭证，不写日志、不返回前端；如业务暂不使用手机号解密等能力，可不持久化。确需持久化时必须使用服务端密钥加密。

## 前端边界

- `wechatAuthService` 负责调用 `wx.login`、请求登录接口、校验响应、存取 Token。
- `authRequest` 统一附加 Bearer Token，并在 401 时清除登录态。
- Pinia 只保存当前用户和登录状态，不直接操作微信 API。
- 登录按钮必须防止重复点击。
- 网络错误、微信登录失败、业务错误分别转换成稳定的用户可读错误。

## Java 映射

- Spring Boot：Controller 与异常处理。
- WebClient：请求 `jscode2session`，配置连接和响应超时。
- MyBatis-Plus：查询或创建 MySQL 用户。
- Sa-Token：创建业务登录态；响应中返回 Sa-Token 值及有效期。
- SLF4J：记录请求阶段、微信错误码和用户 ID，不记录 code、secret、openid、session_key 或 Token。

## 安全与可靠性

- `code` 只能使用一次，服务端不缓存、不记录。
- `secret` 仅存在于后端环境变量或密钥服务。
- 后端必须为微信请求设置超时，并区分超时与微信业务错误。
- `openid` 不作为前端身份凭证。
- 生产环境必须使用 HTTPS。
- 用户注册依赖数据库唯一索引保证幂等，不能只依赖“先查后插”。
- 前端缓存 Token 不等于可信身份，所有受保护接口都必须在后端验证 Token。

## 验证范围

- 前端：微信登录成功、拒绝/失败、后端 401、异常响应、Token 存取。
- 后端：微信成功响应、无效 code、超时、并发注册、数据库失败、Token 生成。
- Java 实现必须保持与前端类型一致的 HTTP 协议和错误码。
