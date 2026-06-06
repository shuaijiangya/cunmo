# 存魔 Java DDD 登录与权限设计

## 1. 目标

将现有 `backend/java` 单模块示例重构为基于 Java 21 的 DDD
模块化单体，实现微信小程序登录、用户注册、角色分配和权限查询。

所有业务包名必须以 `cn.cunmo` 开始。模块、包和类名需要直接表达业务职责，
禁止使用含义模糊的 `common`、`utils`、`manager` 作为业务逻辑容器。

## 2. 工程模块

```text
backend/java
├── cunmo-api
├── cunmo-application
├── cunmo-domain
├── cunmo-infrastructure
├── cunmo-trigger
├── cunmo-bootstrap
└── pom.xml
```

### 2.1 `cunmo-api`

对外接口契约，不包含业务实现：

```text
cn.cunmo.api.auth
├── model.request.WechatLoginRequest
├── model.response.LoginResponse
├── model.response.LoginUserResponse
└── model.response.ErrorResponse
```

### 2.2 `cunmo-domain`

只表达业务模型和领域规则，不依赖 Spring、MyBatis、Sa-Token、WebClient。

```text
cn.cunmo.domain.auth
├── model.aggregate.User
├── model.entity.Role
├── model.entity.Permission
├── model.entity.WechatIdentity
├── model.valueobject.UserId
├── model.valueobject.WechatPrincipal
├── model.enums.UserStatus
├── model.enums.PermissionType
├── repository.UserRepository
├── repository.AuthorizationRepository
├── gateway.WechatGateway
├── service.UserRegistrationService
└── exception.DomainException
```

`User` 是用户聚合根，负责：

- 用户状态校验。
- 新用户创建。
- 用户资料完整度判断。
- 角色集合的领域表达。

角色和权限由授权仓储按用户查询。第一阶段不把完整权限树塞入用户聚合，
避免用户聚合承担后台权限配置职责。

### 2.3 `cunmo-application`

负责编排用例、事务边界和端口调用：

```text
cn.cunmo.application.auth
├── command.WechatLoginCommand
├── service.WechatLoginApplicationService
├── assembler.LoginResponseAssembler
├── port.TokenService
└── result.LoginResult
```

应用服务不直接依赖 MyBatis Mapper 或微信 HTTP DTO。

### 2.4 `cunmo-infrastructure`

实现领域仓储和外部系统端口：

```text
cn.cunmo.infrastructure
├── persistence.user
│   ├── dataobject.UserDO
│   ├── dataobject.WechatIdentityDO
│   ├── dataobject.RoleDO
│   ├── dataobject.PermissionDO
│   ├── mapper.UserMapper
│   ├── mapper.WechatIdentityMapper
│   ├── mapper.AuthorizationMapper
│   ├── converter.UserPersistenceConverter
│   ├── UserRepositoryImpl
│   └── AuthorizationRepositoryImpl
├── gateway.wechat
│   ├── WechatApiClient
│   ├── WechatGatewayImpl
│   └── dto.WechatCodeSessionResponse
├── security.satoken.SaTokenService
└── config
    ├── WechatProperties
    ├── WebClientConfiguration
    └── MybatisPlusConfiguration
```

DO 仅用于数据库映射，不泄漏到 Domain 或 Controller。

### 2.5 `cunmo-trigger`

负责 HTTP 协议适配：

```text
cn.cunmo.trigger.http
├── controller.WechatAuthController
├── advice.GlobalExceptionHandler
└── converter.AuthHttpConverter
```

Controller 只做参数接收、调用应用服务和返回响应，不编写注册、角色或 Token
逻辑。

### 2.6 `cunmo-bootstrap`

唯一可执行模块：

```text
cn.cunmo.bootstrap
└── CunmoApplication
```

包含 `application.yml`、环境变量绑定、Spring 组件扫描和启动配置。

## 3. 模块依赖

```text
cunmo-api
    ↑
cunmo-trigger → cunmo-application → cunmo-domain
                           ↑              ↑
                    cunmo-infrastructure ┘
                           ↑
                    cunmo-bootstrap
```

具体 Maven 约束：

- `cunmo-domain` 不依赖任何内部模块。
- `cunmo-api` 不依赖任何内部模块。
- `cunmo-application` 依赖 `cunmo-domain` 和 `cunmo-api`。
- `cunmo-infrastructure` 依赖 `cunmo-domain` 和 `cunmo-application`。
- `cunmo-trigger` 依赖 `cunmo-api` 和 `cunmo-application`。
- `cunmo-bootstrap` 聚合 `trigger` 与 `infrastructure`，提供运行时依赖。

## 4. RBAC 数据模型

### 4.1 用户表 `sys_user`

```text
id                BIGINT UNSIGNED PRIMARY KEY  COMMENT '用户主键'
username          VARCHAR(64) NULL              COMMENT '业务用户名，微信注册用户可为空'
nickname          VARCHAR(64) NULL              COMMENT '用户昵称'
avatar_url        VARCHAR(512) NULL             COMMENT '用户头像业务访问地址'
status            TINYINT NOT NULL              COMMENT '用户状态：1-启用，0-禁用'
last_login_at     DATETIME(3) NULL              COMMENT '最近一次成功登录时间'
created_at        DATETIME(3) NOT NULL          COMMENT '创建时间'
updated_at        DATETIME(3) NOT NULL          COMMENT '最后更新时间'
version           INT UNSIGNED NOT NULL         COMMENT '乐观锁版本号'
deleted           TINYINT NOT NULL              COMMENT '逻辑删除标记：0-未删除，1-已删除'
```

`version` 用于乐观锁，`deleted` 用于逻辑删除。用户禁用通过 `status` 表达。
表注释为 `系统用户表`。

### 4.2 微信身份表 `sys_wechat_identity`

```text
id                BIGINT UNSIGNED PRIMARY KEY  COMMENT '微信身份主键'
user_id           BIGINT UNSIGNED NOT NULL     COMMENT '关联的系统用户主键'
appid             VARCHAR(64) NOT NULL         COMMENT '微信小程序 AppID'
openid            VARCHAR(128) NOT NULL        COMMENT '用户在当前小程序下的唯一标识'
unionid           VARCHAR(128) NULL            COMMENT '微信开放平台统一用户标识'
created_at        DATETIME(3) NOT NULL         COMMENT '创建时间'
updated_at        DATETIME(3) NOT NULL         COMMENT '最后更新时间'
```

约束：

- 唯一索引 `(appid, openid)`。
- 普通索引 `user_id`、`unionid`。
- 不持久化 `session_key`；后续业务确有需要时单独增加加密凭证模型。
- 表注释为 `用户微信身份表`。

### 4.3 角色表 `sys_role`

```text
id                BIGINT UNSIGNED PRIMARY KEY  COMMENT '角色主键'
role_code         VARCHAR(64) NOT NULL UNIQUE  COMMENT '角色唯一编码'
role_name         VARCHAR(64) NOT NULL         COMMENT '角色显示名称'
description       VARCHAR(255) NULL            COMMENT '角色用途说明'
status            TINYINT NOT NULL             COMMENT '角色状态：1-启用，0-禁用'
data_scope        VARCHAR(32) NOT NULL         COMMENT '数据权限范围编码'
created_at        DATETIME(3) NOT NULL         COMMENT '创建时间'
updated_at        DATETIME(3) NOT NULL         COMMENT '最后更新时间'
deleted           TINYINT NOT NULL             COMMENT '逻辑删除标记：0-未删除，1-已删除'
```

初始角色：

- `USER`：普通用户，微信首次注册时自动绑定。
- `ADMIN`：平台管理员，不通过代码硬编码用户 ID。
- 表注释为 `系统角色表`。

### 4.4 权限表 `sys_permission`

```text
id                BIGINT UNSIGNED PRIMARY KEY   COMMENT '权限主键'
parent_id         BIGINT UNSIGNED NOT NULL      COMMENT '父权限主键，根节点为 0'
permission_code   VARCHAR(128) NOT NULL UNIQUE  COMMENT '权限唯一编码'
permission_name   VARCHAR(64) NOT NULL          COMMENT '权限显示名称'
permission_type   VARCHAR(16) NOT NULL          COMMENT '权限类型：MENU、BUTTON、API'
resource_path     VARCHAR(255) NULL             COMMENT '菜单路由或 API 资源路径'
http_method       VARCHAR(16) NULL              COMMENT 'API 权限对应的 HTTP 方法'
sort_order        INT NOT NULL                  COMMENT '同级权限显示顺序'
status            TINYINT NOT NULL              COMMENT '权限状态：1-启用，0-禁用'
created_at        DATETIME(3) NOT NULL          COMMENT '创建时间'
updated_at        DATETIME(3) NOT NULL          COMMENT '最后更新时间'
deleted           TINYINT NOT NULL              COMMENT '逻辑删除标记：0-未删除，1-已删除'
```

`permission_type` 支持 `MENU`、`BUTTON`、`API`。权限编码采用
`领域:资源:动作` 格式，例如：

- `inventory:item:read`
- `inventory:item:create`
- `inventory:item:update`
- `inventory:item:delete`

表注释为 `系统权限资源表`。

### 4.5 关联表

`sys_user_role`：

```text
user_id           BIGINT UNSIGNED NOT NULL  COMMENT '系统用户主键'
role_id           BIGINT UNSIGNED NOT NULL  COMMENT '系统角色主键'
created_at        DATETIME(3) NOT NULL      COMMENT '角色授权时间'
PRIMARY KEY (user_id, role_id)
```

表注释为 `用户角色关联表`。

`sys_role_permission`：

```text
role_id           BIGINT UNSIGNED NOT NULL  COMMENT '系统角色主键'
permission_id     BIGINT UNSIGNED NOT NULL  COMMENT '系统权限主键'
created_at        DATETIME(3) NOT NULL      COMMENT '权限授权时间'
PRIMARY KEY (role_id, permission_id)
```

表注释为 `角色权限关联表`。

数据库初始化脚本必须幂等插入 `USER`、`ADMIN` 和基础库存权限。

### 4.6 数据库注释规范

- 所有建表语句必须使用 `COMMENT='中文表说明'`。
- 所有业务字段必须使用列级 `COMMENT '中文字段说明'`。
- 枚举和状态字段的注释必须列出全部有效值，不能只写“状态”。
- 外键关联字段必须注明关联的业务表和主键含义。
- 时间字段必须区分创建、更新、授权和最近登录等业务时间。
- 索引名称必须表达用途：
  - 唯一索引使用 `uk_表意字段`。
  - 普通索引使用 `idx_表意字段`。
  - 联合主键和唯一索引需在 SQL 文件上方补充一行用途注释。
- 初始化 SQL 需按“用户、微信身份、角色、权限、关联关系”分区书写，
  每个区域使用简洁的 SQL 行注释说明用途。

## 5. 微信登录用例

1. Controller 校验 `code` 非空且长度不超过 256。
2. Application 创建 `WechatLoginCommand`。
3. `WechatGateway` 调用微信 `jscode2session`。
4. 通过 `(appid, openid)` 查询微信身份及用户聚合。
5. 用户不存在时，在同一数据库事务内：
   - 创建 `sys_user`。
   - 创建 `sys_wechat_identity`。
   - 查询默认 `USER` 角色。
   - 写入 `sys_user_role`。
6. 校验用户状态为 `ENABLED`。
7. 更新 `last_login_at`。
8. 查询有效角色编码和权限编码并去重。
9. `TokenService` 使用 Sa-Token 建立登录态。
10. 返回 Token、用户资料、角色编码和权限编码。

并发首次登录依赖 `(appid, openid)` 唯一索引。发生重复键时重新读取已经创建的
身份，不允许产生两个业务用户。

## 6. 登录响应

```json
{
  "token": "business-token",
  "expiresIn": 7200,
  "user": {
    "id": 10001,
    "nickname": null,
    "avatarUrl": null,
    "profileCompleted": false,
    "roles": ["USER"],
    "permissions": [
      "inventory:item:read",
      "inventory:item:create"
    ]
  }
}
```

前端认证类型同步增加 `roles` 与 `permissions`。为兼容已有缓存，读取旧缓存时
将缺失字段归一化为空数组。

## 7. 异常与事务

稳定错误码：

- `INVALID_REQUEST`
- `INVALID_CODE`
- `WECHAT_TIMEOUT`
- `WECHAT_UNAVAILABLE`
- `DEFAULT_ROLE_MISSING`
- `USER_DISABLED`
- `DATABASE_ERROR`
- `INTERNAL_ERROR`

用户、微信身份和默认角色绑定必须处于同一事务。微信 HTTP 请求必须在数据库
事务开始前完成，避免事务期间等待外部网络。

日志只记录 trace ID、内部用户 ID、用例耗时和错误码。禁止记录微信 code、
openid、session_key、secret 和完整 Token。

## 8. 注释与编码规范

- Java 使用 JDK 21。
- 所有业务包以 `cn.cunmo` 开始。
- 公共接口、领域规则、事务边界和微信错误映射使用简洁中文 Javadoc。
- 对显而易见的 getter、赋值和简单分支不写重复注释。
- Domain 不使用 Lombok、Spring 注解或 MyBatis 注解。
- 优先使用不可变 `record` 表达 Command、Result、值对象和 API DTO。
- Entity/DO 不混用；数据库字段变化由 Infrastructure 层吸收。
- 禁止 Controller 直接调用 Mapper。
- 禁止 Infrastructure 类型出现在 Application 对外方法签名中。

## 9. 测试策略

### Domain

- 新用户创建及默认状态。
- 禁用用户拒绝登录。
- 用户资料完整度判断。

### Application

- 已有用户登录。
- 首次登录注册并绑定 `USER`。
- 并发重复身份恢复。
- 默认角色缺失。
- 微信 code 无效和超时。
- Token 返回角色与权限。

### Infrastructure

- Testcontainers MySQL 验证唯一索引和事务回滚。
- MockWebServer 验证微信响应映射及超时。
- Mapper 验证多角色、多权限去重查询。

### Trigger

- MockMvc 验证请求校验、成功响应和统一错误结构。

## 10. 验收标准

- Maven 聚合工程能在 JDK 21 下完整构建。
- 模块依赖符合第 3 节，不存在反向依赖。
- 微信登录端到端写入用户、身份和默认角色。
- 登录响应包含去重后的角色与权限编码。
- SQL 支持多用户、多角色、多权限关系。
- 每张表和每个业务字段均包含可在 MySQL 元数据中查询到的中文注释。
- 前端类型、测试和微信小程序构建通过。
- 源码及配置不存在硬编码微信 Secret 或敏感凭证日志。
