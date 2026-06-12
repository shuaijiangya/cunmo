# 容量配额与会员商业化设计

## 1. 目标

在现有“存量魔方”系统中新增容量与配额看板、PRO 会员、微信支付、手动续费、自动续费、管理员升级申请与审批 API，并在库存写操作中严格执行免费版配额。

本次前端沿用现有 `uni-app + Vue 3 + TypeScript` 技术栈，构建产物为微信小程序 WXML/WXSS。页面视觉基于 `index2.html` 原型，保留深色套餐头图、配额卡、爆仓雷达、PRO 渐变卡与 2.5 秒 `shimmer` 流光动画。

## 2. 范围

### 2.1 本期包含

- 容量与配额动态看板。
- 月度 PRO：9.9 元，每次获得 30 天权益。
- 永久 PRO：69 元，一次购买永久有效。
- 月卡手动续费。
- 微信委托代扣自动续费的签约、扣款、失败重试、解约和到期处理。
- 微信客服、企业微信二维码和管理员电话入口。
- 用户提交管理员升级申请。
- 管理员查询、审批和驳回升级申请的 API。
- 管理员审批时授予自定义月数或永久 PRO。
- 免费版配额的服务端强制校验。

### 2.2 本期不包含

- 管理端可视化页面。
- 用户自助退款和管理员退款 API。
- 年卡、团队版、优惠券、促销价和赠送码。
- 自动续费权限的商户申请流程。

## 3. 架构

新增独立 `membership` 限界上下文，统一管理套餐、支付订单、会员权益、续费协议、续费扣款记录和管理员升级申请。微信支付作为基础设施适配器接入，会员应用服务不直接依赖微信 SDK 或 HTTP 客户端。

库存域不保存会员状态。库存应用服务通过 `QuotaPolicy` 端口获取当前用户的有效权益和限额，在创建空间、绑定分类、新增物品前执行强制校验。容量看板使用同一套权益与库存统计读模型，避免展示规则和写入规则分叉。

## 4. 会员模型

### 4.1 套餐

固定提供以下产品：

| 产品编码 | 权益 | 价格 |
| --- | --- | --- |
| `MONTHLY_PRO` | 30 天 PRO | 990 分 |
| `LIFETIME_PRO` | 永久 PRO | 6900 分 |

产品价格由服务端返回，前端不得自行计算支付金额。

### 4.2 当前权益

`membership_entitlement` 保存用户当前有效权益：

- `FREE`
- `MONTHLY_PRO`
- `LIFETIME_PRO`

月卡保存 `effective_at` 和 `expires_at`。永久会员的 `expires_at` 为空。读取权益时若月卡已到期，立即按 FREE 处理；定时任务负责将持久化状态补偿更新为过期状态。

永久会员优先级最高。永久权益生效后，自动续费协议被终止或标记为不再扣款，后续月卡支付回调不得覆盖永久权益。

### 4.3 月卡顺延

- 首次购买或已过期后购买：从支付成功时间开始增加 30 天。
- 未过期时续费：从当前 `expires_at` 增加 30 天。
- 自动续费扣款成功：从当前 `expires_at` 增加 30 天；若回调处理时已过期，则从支付成功时间增加 30 天。

时间统一以服务端 UTC 保存，接口返回 ISO 8601 时间。

## 5. 配额规则

FREE 用户严格执行：

- 最多 3 个根空间。
- 每个空间最多绑定 3 个分类。
- 每个“空间 + 分类”腔体最多存在 10 条未删除物品记录。

PRO 用户上述三项均无限。

配额按物品记录条数计算，不按 `quantity` 数量总和计算。库存写操作必须在事务内重新统计并校验，避免并发请求绕过限制。超过限额时返回稳定业务错误码：

- `ROOT_SPACE_QUOTA_EXCEEDED`
- `SPACE_CATEGORY_QUOTA_EXCEEDED`
- `CAVITY_ITEM_QUOTA_EXCEEDED`

已有数据超过免费配额时不自动删除。会员到期后仍允许查询、扣减库存和删除数据，但禁止继续创建超额结构或新增物品，直到数据回落到免费配额或重新开通 PRO。

## 6. 数据库设计

新增迁移 `backend/mysql/006_membership_quota.sql`：

### 6.1 `membership_entitlement`

保存用户当前权益类型、生效时间、到期时间、来源、版本号和更新时间。`user_id` 唯一。

### 6.2 `membership_order`

保存商户订单号、用户、产品、金额、订单状态、微信支付单号、支付时间和幂等版本。商户订单号与微信支付单号分别建立唯一索引。

订单状态为 `CREATED / PAYING / PAID / CLOSED / FAILED`。

### 6.3 `membership_renewal_agreement`

保存用户签约协议号、商户签约号、状态、下次扣款时间、连续失败次数及解约时间。状态为 `PENDING / ACTIVE / TERMINATED`。

### 6.4 `membership_renewal_attempt`

保存每次代扣的业务流水号、协议、计划扣款时间、实际扣款时间、金额、状态、微信支付单号和失败原因。业务流水号唯一。

### 6.5 `membership_upgrade_request`

保存申请人联系方式、备注、申请状态、审批人、审批结果、授予类型、自定义月数和审批时间。状态为 `PENDING / APPROVED / REJECTED`。

### 6.6 `membership_payment_notification`

记录微信通知 ID、通知类型和处理结果。通知 ID 唯一，用于第一层回调幂等；订单号或续费流水号作为第二层业务幂等键。

## 7. 支付与自动续费

系统使用微信支付普通直连商户模式。主动 JSAPI 支付使用 API v3；普通商户
委托代扣沿用微信支付 V2 XML 协议，两类协议不得混用。

### 7.1 主动支付

1. 小程序请求创建订单。
2. 服务端读取固定产品和服务端价格，创建本地订单。
3. 服务端调用微信 JSAPI 下单。
4. 服务端返回 `uni.requestPayment` 所需参数。
5. 微信支付回调到服务端。
6. 服务端验签、解密、核对商户号、订单号、金额和交易状态。
7. 在事务中将订单置为已支付并授予权益。
8. 前端轮询订单状态，不以 `requestPayment` 成功回调直接认定权益生效。

### 7.2 自动续费

自动续费依赖商户已获微信委托代扣权限和已配置签约模板。服务端通过配置开关控制能力：

- 权限与配置完整：看板返回 `renewal.supported = true`。
- 权限缺失或功能关闭：返回 `false`，前端隐藏自动续费开关，月卡手动续费不受影响。

签约参数使用 V2 `HMAC-SHA256` 生成，前端通过
`uni.navigateToMiniProgram` 跳转微信签约小程序；签约、扣款和解约接口使用
V2 XML。扣款/解约 URL 由商户获批的委托代扣产品决定，必须通过服务端配置提供，
不能假设所有普通商户使用同一个接口。

流程包括签约、签约回调、服务端到期扣款、扣款回调、失败重试和用户解约。
扣款失败按可配置策略重试，超过次数后停止本周期重试；月卡到期后读取立即按
FREE 处理，定时任务再将持久化状态批量收敛为 FREE。

### 7.3 幂等与安全

- JSAPI API v3 回调必须验证平台证书签名并使用 APIv3 密钥解密。
- 委托代扣 V2 回调必须验证 XML 中的 `HMAC-SHA256` 签名。
- 重复通知不得重复增加权益。
- 金额、商户号、AppID、订单号和产品必须与本地订单匹配。
- APIv2/APIv3 密钥、证书私钥和签约模板信息仅来自服务端配置。
- 永久会员不得继续自动扣款。

## 8. API 契约

### 8.1 用户接口

```text
GET  /api/membership/dashboard
POST /api/membership/orders
GET  /api/membership/orders/{orderNo}
POST /api/membership/renewal-agreements
POST /api/membership/renewal-agreements/terminate
POST /api/membership/upgrade-requests
```

### 8.2 微信回调

```text
POST /api/wechat-pay/payment-notify
POST /api/wechat-pay/contract-notify
POST /api/wechat-pay/renewal-notify
```

### 8.3 管理员接口

```text
GET  /api/admin/membership/upgrade-requests
POST /api/admin/membership/upgrade-requests/{id}/approve
POST /api/admin/membership/upgrade-requests/{id}/reject
```

管理员接口沿用现有鉴权体系，通过权限码限制访问。本期只提供 API，不创建管理页面。

### 8.4 看板响应

后端 Java record 和前端 TypeScript interface 使用完全一致的字段名：

```ts
interface MembershipDashboard {
  plan: MembershipPlan
  quota: MembershipQuota
  products: MembershipProduct[]
  renewal: RenewalSummary
  contact: MembershipContact
}

interface MembershipPlan {
  type: 'FREE' | 'MONTHLY_PRO' | 'LIFETIME_PRO'
  displayName: string
  effectiveAt: string | null
  expiresAt: string | null
}

interface QuotaUsage {
  used: number
  limit: number | null
  percent: number
  reached: boolean
}

interface CategoryQuotaUsage extends QuotaUsage {
  spaceId: number
  spaceName: string
}

interface CapacityAlert {
  spaceId: number
  spaceName: string
  categoryId: number
  categoryName: string
  used: number
  limit: number
  percent: number
  status: 'WARNING' | 'LOCKED'
}

interface MembershipQuota {
  rootSpaces: QuotaUsage
  constrainedCategories: CategoryQuotaUsage[]
  itemCapacityLimit: number | null
  capacityAlerts: CapacityAlert[]
}

interface MembershipProduct {
  code: 'MONTHLY_PRO' | 'LIFETIME_PRO'
  name: string
  priceFen: number
  priceText: string
  durationDays: number | null
  recommended: boolean
}

interface RenewalSummary {
  supported: boolean
  enabled: boolean
  status: 'NONE' | 'PENDING' | 'ACTIVE' | 'TERMINATED'
  nextChargeAt: string | null
}

interface MembershipContact {
  customerServiceEnabled: boolean
  enterpriseWechatQrUrl: string | null
  phone: string | null
}
```

创建订单响应包含 `orderNo`、`status` 和小程序调起支付所需 `paymentParams`。订单查询返回权威支付状态和最新权益摘要。

创建续费协议响应字段为：

```ts
interface RenewalAgreementResponse {
  contractCode: string
  appId: string
  path: string
  extraData: Record<string, string>
}
```

其中 `appId/path/extraData` 直接用于 `uni.navigateToMiniProgram`，前端不得重新
计算 V2 签名。

## 9. 前端页面

新增 `src/pages/quota/index.vue` 并注册到 `src/pages.json`。页面使用 Vue 3 Composition API 和 TypeScript，业务类型放入 `src/types/membership.ts`，请求封装放入 `src/services/membershipApi.ts`。

页面状态包括加载、加载失败、正常、创建订单中、等待支付确认、签约中和提交申请中。所有进度条、套餐、文案状态、爆仓列表和联系信息均来自接口数据。

视觉实现需精确映射原型的颜色、圆角、阴影和间距；PRO 卡使用：

```css
@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
```

动画周期为 2.5 秒并无限循环。页面增加月卡与永久卡选择、自动续费开关、会员协议提示、管理员申请和联系客服入口。

支付成功以订单查询结果为准。企业微信二维码使用预览图片，电话支持复制和拨打，微信客服在配置开启时使用小程序客服能力。

## 10. 错误处理

- 看板加载失败提供重试。
- 用户取消支付不显示系统错误，只恢复可操作状态。
- 支付结果未知时轮询订单；超时后提示用户稍后刷新，不重复创建权益。
- 自动续费不受支持时隐藏开关并保留手动购买。
- 配额错误由前端映射为明确提示，并可跳转容量看板。
- 管理员申请重复提交时返回已有待处理申请，避免生成重复工单。

## 11. 测试

### 11.1 后端

- 会员权益顺延、过期、永久覆盖月卡。
- 支付回调验签后的订单状态和双重幂等。
- 自动续费成功、失败重试、解约和永久会员停止扣款。
- 管理员自定义月数与永久授予。
- FREE 三类配额边界及 PRO 无限容量。
- 并发写入下的事务内配额校验。
- 控制器响应 JSON 与 API record 契约。

### 11.2 前端

- 看板接口与 TypeScript 类型映射。
- 套餐选择、支付参数传递与订单轮询。
- 自动续费支持与不支持两种状态。
- 管理员申请、客服、二维码和电话入口。
- 配额进度、锁定状态和空列表渲染。
- 微信小程序构建通过。

## 12. 验收标准

- 页面在微信小程序端还原已确认的高保真方案。
- FREE 用户无法通过任何库存写 API 绕过三项配额。
- PRO 用户不受三项配额限制。
- 月卡可首次购买、手动续费、自动续费、解约和到期降级。
- 永久会员支付后永久生效并停止自动续费。
- 支付和代扣重复回调不会重复增加权益。
- 管理员可通过 API 审批自定义月数或永久权益。
- 后端 JSON 字段与前端 TypeScript interface 完全一致。

## 13. 实现状态

截至 2026-06-12，本设计的用户端看板、9.9 元月卡、69 元永久会员、手动续费、
普通商户委托代扣、到期降级、严格配额、联系管理员升级及管理员审批 API 已实现。
管理端可视化页面仍明确不在本期范围内。
