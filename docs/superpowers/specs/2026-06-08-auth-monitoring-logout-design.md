# Java 登录监控与当前会话退出设计

## 目标

在现有微信登录链路上增加可观测指标，并提供只注销当前 Bearer Token 的退出接口。监控数据用于判断登录可用性、失败原因和性能趋势，不记录微信凭证或业务 Token。

## HTTP 协议

新增接口：

```http
POST /api/auth/logout
Authorization: Bearer <business-token>
```

成功返回：

```http
HTTP/1.1 204 No Content
```

规则：

- 必须携带有效登录态。
- 只注销当前请求携带的 Token。
- 不注销该用户在其他设备或客户端上的并发会话。
- 已注销、过期或无效 Token 返回现有 `401 UNAUTHORIZED` 协议。
- 接口不接收请求体，不返回 Token 或用户资料。

## 应用边界

扩展 `TokenService`：

```java
void logoutCurrentToken();
```

Sa-Token 适配器负责识别当前 Token 并执行单 Token 注销。Controller 和应用服务不直接依赖 `StpUtil`。

新增 `AuthMonitoring` 应用端口，表达以下业务观测事件：

- 登录成功及总耗时。
- 登录失败及稳定错误码。
- 退出成功及总耗时。
- 退出失败及稳定错误码。

Micrometer 实现位于 Infrastructure。应用层只依赖端口，不依赖 `MeterRegistry`。

## 指标

指标使用 Micrometer 命名，暴露时由具体 registry 转换：

- `cunmo.auth.login.success`：登录成功 Counter。
- `cunmo.auth.login.failure`：登录失败 Counter，标签 `error_code`。
- `cunmo.auth.login.duration`：登录总耗时 Timer，标签 `outcome=success|failure`。
- `cunmo.auth.logout.success`：退出成功 Counter。
- `cunmo.auth.logout.failure`：退出失败 Counter，标签 `error_code`。
- `cunmo.auth.logout.duration`：退出总耗时 Timer，标签 `outcome=success|failure`。

`error_code` 只允许应用已知的稳定错误码。未知异常统一使用 `INTERNAL_ERROR`，禁止把异常消息、用户 ID、Token、openid 或 traceId 作为指标标签，避免敏感信息和高基数问题。

## 登录监控流程

`WechatLoginApplicationService.login` 在用例入口记录开始时间：

1. 登录成功后记录成功计数和总耗时。
2. `DomainException`、`ApplicationException` 或其他运行时异常发生时，记录失败计数与失败耗时后原样抛出。
3. 领域与应用异常使用自身稳定错误码。
4. 其他运行时异常使用 `INTERNAL_ERROR`。

现有阶段日志继续保留，用于定位微信交换、用户读取、授权加载和 Token 签发中的具体慢点。Micrometer Timer 只统计端到端总耗时。

## 退出流程

新增 `LogoutApplicationService`：

1. 读取当前用户 ID，保证请求持有有效登录态。
2. 调用 `TokenService.logoutCurrentToken()`。
3. 记录退出成功指标和结构化日志。
4. 失败时记录稳定错误码和失败耗时，再原样抛出。

读取用户 ID 必须发生在注销前，因为当前 Token 注销后不能再依赖登录上下文获取用户。

Sa-Token 实现使用当前请求 Token 执行单 Token 注销，不调用用户级 `logout(userId)` 或踢下线全部会话的方法。

## 结构化日志

登录继续使用 `event=wechat_login`。退出使用 `event=auth_logout`，包含：

- `stage`
- 内部 `userId`，仅在成功读取当前用户后记录
- `elapsedMs`
- `errorCode`
- `exceptionType`

日志禁止记录：

- 微信登录 code
- Token 值或 Authorization 请求头
- openid、unionid、session_key
- AppSecret

请求 traceId 继续由 `RequestTraceFilter` 写入 MDC 和响应头，不作为指标标签。

## Actuator 暴露

Bootstrap 引入 Spring Boot Actuator，默认暴露：

- `health`
- `info`
- `metrics`
- `prometheus`

增加 Prometheus registry 依赖，使 `/actuator/prometheus` 可供采集。

生产环境应在网关或网络层限制 Actuator 访问。本次不增加应用内鉴权规则，避免与尚未执行的 RBAC 混合；README 和配置中明确该部署要求。

## 小程序退出降级

前端增加 `POST /api/auth/logout` 调用：

1. 用户确认退出后读取当前本地会话。
2. 有会话时请求后端退出接口。
3. 无论后端返回成功、401、网络错误或超时，都清除本地 Token 并恢复访客示例态。
4. 非成功响应可显示短提示，但不得阻止本地退出。

后端成功返回 204 时 HTTP 客户端必须接受空响应体。

## 测试

Java 测试覆盖：

- 登录成功记录成功 Counter 和成功 Timer。
- 已知业务错误记录对应 `error_code`。
- 未知异常记录 `INTERNAL_ERROR`。
- 退出先读取用户，再注销当前 Token。
- 退出成功记录指标。
- 无效 Token 返回 401 并记录失败指标。
- Controller 的 `POST /api/auth/logout` 返回 204。
- Sa-Token 适配器只注销当前 Token。
- Actuator Prometheus endpoint 配置可加载。

前端测试覆盖：

- 退出请求携带当前 Bearer Token。
- 204 后清除本地会话并进入访客态。
- 后端退出失败仍清除本地会话并进入访客态。

最终验证运行前端 Vitest、H5 与微信小程序构建，以及 Java Maven 全模块测试。
