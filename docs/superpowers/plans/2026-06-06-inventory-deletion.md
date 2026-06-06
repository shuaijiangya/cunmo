# 库存结构删除能力实施计划

**目标：** 按已确认的 C/A/A/A/A/A/A 方案，实现空间、分类、物品的删除、迁移和清空能力，并完成小程序联调。

**架构：** 删除命令由独立的 `InventoryDeletionApplicationService` 编排，领域对象负责单个物品状态变化，仓储负责在同一事务中完成结构校验、容量校验、乐观锁更新和流水落库。查询侧提供删除预览，小程序使用统一管理弹层提交策略并刷新库存、透视镜和流转轴。

**技术栈：** Java 21、Spring Boot、MyBatis-Plus、MySQL 8、Vue 3、TypeScript、Pinia、uni-app。

## 任务 1：领域删除规则

**文件：**
- 修改：`backend/java/cunmo-domain/src/test/java/cn/cunmo/domain/inventory/model/aggregate/InventoryItemTest.java`
- 修改：`backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/aggregate/InventoryItem.java`
- 新增：`backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/aggregate/InventoryMovement.java`
- 新增：`backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/aggregate/InventoryDeletion.java`
- 修改：`backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/enums/StockTransactionType.java`

1. 先增加迁移后位置为“待整理”、清空删除数量归零的失败测试。
2. 运行定向 JUnit，确认测试因领域方法缺失失败。
3. 实现领域方法及 MOVE、DELETE 类型。
4. 再次运行定向 JUnit。

## 任务 2：数据库和持久化命令

**文件：**
- 新增：`backend/mysql/005_inventory_deletion.sql`
- 修改：`InventoryItemMapper.java`、`InventorySpaceMapper.java`、`InventoryCategoryMapper.java`、`SpaceCategoryMapper.java`
- 修改：`StockTransactionDO.java`
- 修改：`InventoryRepository.java`、`InventoryRepositoryImpl.java`

1. 增加来源和目标空间、分类、位置快照字段及注释。
2. 增加归属查询、结构下物品查询、迁移、逻辑删除、解绑和孤儿分类清理 SQL。
3. 所有批量命令使用魔方域行锁与 Spring 事务，容量超限或乐观锁冲突整体回滚。
4. 每个公开方法补充中文注释和关键阶段日志。

## 任务 3：应用服务和 HTTP 接口

**文件：**
- 新增：删除策略、预览结果、请求 DTO。
- 新增：`InventoryDeletionApplicationService.java`
- 修改：`InventoryController.java`
- 修改：Spring 配置装配类。

1. 先增加应用服务测试，覆盖物品删除、空间迁移、分类解绑和非法同目标。
2. 实现删除预览和命令接口。
3. 返回稳定错误码并记录请求、校验、提交日志。
4. 使用 Java 21 定向编译验证模块边界。

## 任务 4：小程序 API 与状态

**文件：**
- 修改：`src/types/inventory.ts`
- 修改：`src/services/inventoryApi.ts`
- 修改：`src/services/inventoryApi.spec.ts`
- 修改：`src/stores/inventoryStore.ts`
- 修改：`src/stores/inventory.spec.ts`

1. 先增加 API 地址和删除后刷新状态的失败测试。
2. 增加预览、迁移、删除、解绑 API。
3. 删除成功后统一刷新 bootstrap、items、analytics、transactions。
4. 当前筛选已失效时回退到“全部”。

## 任务 5：小程序管理交互

**文件：**
- 新增：`src/components/InventoryDeleteModal.vue`
- 修改：`src/components/SpaceNavigator.vue`
- 修改：`src/components/ItemCard.vue`
- 修改：`src/pages/index/index.vue`

1. 空间、分类和物品支持长按进入管理。
2. 弹层展示受影响物品和数量，提供迁移、清空删除两种策略。
3. 迁移目标使用选择器；清空删除要求二次确认。
4. 请求冲突时保留弹层并显示后端错误。

## 任务 6：验证与提交

1. 运行 Java 领域与应用定向测试、Java 21 定向编译。
2. 运行 `npm test -- --run`、`npm run build`、`npm run build:mp-weixin`。
3. 使用应用内浏览器检查首页管理入口和弹层布局。
4. 检查 `git diff`，确认没有环境变量、构建产物和私密配置。
5. 提交功能分支。
