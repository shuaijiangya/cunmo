# 存魔方库存领域设计

## 1. 目标

将当前小程序中的空间、分类、物品、库存增减、预警、透视统计和流转轴从前端 Mock 数据迁移为真实的 Java + MySQL 服务。

首版每个用户拥有一个独立魔方域。所有库存业务数据使用 `vault_id` 隔离，并预留未来家庭成员共享同一魔方域的扩展能力。

## 2. 领域边界

### 2.1 库存写模型

- `InventoryVault`：魔方域，库存数据的租户边界。
- `Space`：物品所属空间。
- `Category`：分类主数据。
- `SpaceCategory`：空间允许使用的分类关系。
- `InventoryItem`：物品聚合根，维护位置、数量、预警线和版本。
- `StockTransaction`：不可变库存流水。

### 2.2 查询模型

查询服务不修改聚合，负责：

- 按空间、分类和关键词查询物品。
- 按空间或分类聚合库存数量。
- 按游标分页查询库存流水。
- 返回小程序首屏所需空间、分类和总库存。

统计数据实时聚合，不建立冗余统计表。

所有表之间只保存关联主键并建立查询索引，不创建数据库外键。关联有效性、删除约束和事务一致性由领域服务与应用层维护，避免数据库级外键影响后续分库、归档和共享域扩展。

## 3. 核心规则

1. 用户首次进入库存域时自动创建默认魔方域。
2. 空间编码在同一魔方域内唯一。
3. 分类编码在同一魔方域内唯一。
4. 物品必须绑定当前魔方域下的有效空间和分类。
5. 库存数量不能小于零。
6. 库存变更通过乐观锁防止并发覆盖。
7. 库存数量更新和流水写入必须处于同一数据库事务。
8. 调整后数量小于等于预警线时，流水类型记录为 `WARN`。
9. 流水不可修改或删除。
10. 所有接口只能访问当前登录用户所属魔方域。
11. 普通用户每个“空间 + 分类”组合默认最多创建 20 个物品。
12. 容量上限由服务端魔方域权益控制，小程序用户不能修改。
13. `item_limit_per_space_category` 为 `NULL` 时表示无限容量，供未来会员权益使用。
14. 创建物品时必须在服务端统计当前空间分类组合的有效物品数，并在领域服务中执行容量校验。

## 4. 数据库设计

### 4.1 `inv_vault`

- `id`
- `owner_user_id`
- `vault_name`
- `item_limit_per_space_category`
- `status`
- `created_at`
- `updated_at`
- `version`
- `deleted`

`owner_user_id` 首版唯一。未来增加共享成员表时，库存业务表保持不变。

`item_limit_per_space_category` 默认值为 20，`NULL` 表示无限容量。该值由服务端管理配置或未来会员权益服务维护，不对普通小程序用户开放修改接口。

### 4.2 `inv_space`

- `id`
- `vault_id`
- `space_code`
- `space_name`
- `sort_order`
- `status`
- `created_at`
- `updated_at`
- `version`
- `deleted`

唯一键：`vault_id + space_code`。

### 4.3 `inv_category`

- `id`
- `vault_id`
- `category_code`
- `category_name`
- `sort_order`
- `status`
- `created_at`
- `updated_at`
- `version`
- `deleted`

唯一键：`vault_id + category_code`。

### 4.4 `inv_space_category`

- `vault_id`
- `space_id`
- `category_id`
- `created_at`

联合主键：`space_id + category_id`。

### 4.5 `inv_item`

- `id`
- `vault_id`
- `space_id`
- `category_id`
- `item_name`
- `small_category`
- `detail_location`
- `quantity`
- `minimum_quantity`
- `status`
- `created_at`
- `updated_at`
- `version`
- `deleted`

索引覆盖魔方域、空间、分类、名称和更新时间。

### 4.6 `inv_stock_transaction`

- `id`
- `vault_id`
- `item_id`
- `transaction_type`
- `quantity_delta`
- `quantity_before`
- `quantity_after`
- `description`
- `space_snapshot`
- `category_snapshot`
- `detail_location_snapshot`
- `operator_user_id`
- `occurred_at`

流水保存位置快照，避免空间或分类改名后历史轨迹失真。

## 5. API

### 5.1 初始化

`GET /api/inventory/bootstrap`

返回当前魔方域、空间、空间分类关系、总库存和容量权益。容量权益包含：

- `itemLimitPerSpaceCategory`：当前组合上限，无限时为 `null`。
- `unlimited`：是否无限容量。
- 每个空间分类绑定的 `itemCount`、`remainingCount` 和 `capacityReached`。

### 5.2 空间和分类

- `POST /api/inventory/spaces`
- `POST /api/inventory/categories`
- `PUT /api/inventory/categories/{categoryId}/spaces`

创建分类时允许一次绑定多个空间。修改绑定时，如果待解绑的空间分类组合下仍有有效物品，服务端拒绝解绑。

### 5.3 物品

- `GET /api/inventory/items?spaceId=&categoryId=&keyword=&cursor=&size=`
- `POST /api/inventory/items`
- `POST /api/inventory/items/{itemId}/adjustments`

调整请求使用 `delta`，服务端计算前后数量并生成流水。

### 5.4 流水和统计

- `GET /api/inventory/transactions?cursor=&size=`
- `GET /api/inventory/analytics?dimension=SPACE|CATEGORY&cursor=&size=`

列表返回 `nextCursor` 和 `hasMore`，支持小程序滑动加载。

## 6. DDD 模块职责

- `cunmo-domain`：库存聚合、值对象、领域规则、仓储端口。
- `cunmo-application`：命令、查询、事务用例和当前用户上下文。
- `cunmo-infrastructure`：MyBatis-Plus `BaseMapper`、仓储、查询 Mapper。
- `cunmo-api`：请求与响应协议。
- `cunmo-trigger`：Controller、参数校验和异常映射。
- `cunmo-bootstrap`：应用启动和配置。

领域层不依赖 Spring、MyBatis 或 Sa-Token。

## 7. 前端联调

新增 `inventoryApi`，Pinia 保留：

- 当前筛选条件。
- 搜索词。
- 当前视图和弹窗。
- 服务端返回的空间、分类、物品、统计和流水。
- 各列表游标、加载中和是否有更多数据。

登录成功后加载 `bootstrap` 和首批物品。切换筛选重新请求物品；创建空间、分类、物品和调整库存成功后局部更新状态，并刷新统计与流水。

## 8. 错误处理

- `INVENTORY_VAULT_NOT_FOUND`
- `SPACE_NOT_FOUND`
- `CATEGORY_NOT_FOUND`
- `ITEM_NOT_FOUND`
- `INSUFFICIENT_STOCK`
- `INVENTORY_CONFLICT`
- `CATEGORY_CAPACITY_EXCEEDED`
- `CATEGORY_UNBIND_BLOCKED`
- `DUPLICATE_SPACE`
- `DUPLICATE_CATEGORY`
- `INVALID_CURSOR`

所有错误通过统一错误协议返回，不暴露 SQL 或基础设施异常。

## 9. 验证标准

1. MySQL 脚本包含完整字段、索引和表注释。
2. 库存调整和流水写入具备事务一致性。
3. 不同用户无法读取或修改彼此魔方域。
4. 前端不再依赖库存 Mock 数据。
5. 首页、透视镜和流转轴支持真实分页。
6. 空状态原地建货保留空间和分类上下文。
7. Java 中文函数注释审计通过。
8. 前端测试、类型检查和微信小程序构建通过。

## 10. 默认初始化

用户首次创建魔方域时初始化三个空间：

- 卧室
- 客厅
- 办公室

初始化三个分类：

- 电子数码
- 衣物服饰
- 生活起居

默认绑定关系：

- 卧室：衣物服饰、生活起居
- 客厅：电子数码、生活起居
- 办公室：电子数码

该绑定展示一个分类可关联多个空间，同时避免默认界面产生无意义的全部交叉组合。用户后续可通过“快速多选绑定”调整关系。

## 11. 小程序交互

分类行右侧“＋”打开快速绑定弹窗：

1. 输入分类名称。
2. 多选适用空间，默认选中当前空间。
3. 展示只读权益信息，例如“每个空间分类最多 20 件”。
4. 创建后一次性写入分类与多个空间绑定。

物品创建界面展示当前组合用量：

- 普通容量：`12 / 20`。
- 达到上限：禁用确认入库，显示容量已满。
- 无限容量：显示“无限容量”，不显示剩余数量。

容量只能由服务端管理配置或会员权益策略修改，小程序不提供编辑控件。

透视镜由后端查询模型返回空间或分类维度的物品总数量、库存总件数和占比。流转轴读取不可变库存流水，并使用游标分页；两者均不依赖前端 Mock 计算。
