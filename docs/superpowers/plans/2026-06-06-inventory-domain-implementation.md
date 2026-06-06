# 存魔方库存领域实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将小程序空间、分类、物品、库存调整、透视镜和流转轴从 Mock 数据迁移到 Java DDD 后端与 MySQL，并完成真实分页联调。

**Architecture:** 写模型以魔方域和库存物品聚合维护业务规则，库存调整与流水在同一事务中完成；读模型使用独立查询 Mapper 返回首屏、物品列表、透视统计和流水分页。前端 Pinia 仅维护服务端状态、筛选条件与分页游标。

**Tech Stack:** Java 21、Spring Boot、MyBatis-Plus、MySQL 8、Sa-Token、Vue 3、TypeScript、Pinia、uni-app。

---

### Task 1: 库存领域核心

**Files:**
- Create: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/aggregate/InventoryItem.java`
- Create: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/aggregate/InventoryVault.java`
- Create: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/entity/InventorySpace.java`
- Create: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/entity/InventoryCategory.java`
- Create: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/entity/StockTransaction.java`
- Create: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/valueobject/InventoryItemId.java`
- Create: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/valueobject/VaultId.java`
- Create: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/model/enums/StockTransactionType.java`
- Create: `backend/java/cunmo-domain/src/main/java/cn/cunmo/domain/inventory/repository/InventoryRepository.java`
- Test: `backend/java/cunmo-domain/src/test/java/cn/cunmo/domain/inventory/model/aggregate/InventoryItemTest.java`

- [ ] 编写库存不能减为负数、达到预警线生成 WARN、容量 20 与无限容量规则的失败测试。
- [ ] 直接使用 JDK 21 编译并执行测试，确认因领域类型缺失失败。
- [ ] 实现最小领域聚合和值对象。
- [ ] 重新执行领域测试并确认通过。

### Task 2: 数据对象与 BaseMapper

**Files:**
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/dataobject/InventoryVaultDO.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/dataobject/InventorySpaceDO.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/dataobject/InventoryCategoryDO.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/dataobject/SpaceCategoryDO.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/dataobject/InventoryItemDO.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/dataobject/StockTransactionDO.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/mapper/InventoryVaultMapper.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/mapper/InventorySpaceMapper.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/mapper/InventoryCategoryMapper.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/mapper/SpaceCategoryMapper.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/mapper/InventoryItemMapper.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/mapper/StockTransactionMapper.java`

- [ ] 编写 Mapper 继承 `BaseMapper<T>` 的契约测试。
- [ ] 添加表名、主键、逻辑删除与乐观锁注解。
- [ ] 保留复杂聚合 SQL 为自定义 Mapper 方法。
- [ ] 定向编译基础设施持久化代码。

### Task 3: 默认魔方域初始化

**Files:**
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/inventory/service/InventoryBootstrapApplicationService.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/InventoryRepositoryImpl.java`
- Test: `backend/java/cunmo-application/src/test/java/cn/cunmo/application/inventory/service/InventoryBootstrapApplicationServiceTest.java`

- [ ] 测试首次访问创建一个默认魔方域、三个空间和三个分类。
- [ ] 测试默认绑定为卧室/衣物服饰、卧室/生活起居、客厅/电子数码、客厅/生活起居、办公室/电子数码。
- [ ] 在事务中完成幂等初始化。
- [ ] 返回容量权益和每个绑定的物品用量。

### Task 4: 空间与多空间分类绑定

**Files:**
- Create: `backend/java/cunmo-api/src/main/java/cn/cunmo/api/inventory/model/request/CreateSpaceRequest.java`
- Create: `backend/java/cunmo-api/src/main/java/cn/cunmo/api/inventory/model/request/CreateCategoryRequest.java`
- Create: `backend/java/cunmo-api/src/main/java/cn/cunmo/api/inventory/model/request/UpdateCategorySpacesRequest.java`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/inventory/service/InventoryStructureApplicationService.java`
- Test: `backend/java/cunmo-application/src/test/java/cn/cunmo/application/inventory/service/InventoryStructureApplicationServiceTest.java`

- [ ] 测试分类可一次绑定多个当前魔方域空间。
- [ ] 测试其他用户空间不能被绑定。
- [ ] 测试存在物品的空间分类组合不能解绑。
- [ ] 实现创建空间、创建分类和绑定差量同步。

### Task 5: 物品创建与库存调整

**Files:**
- Create: `backend/java/cunmo-api/src/main/java/cn/cunmo/api/inventory/model/request/CreateInventoryItemRequest.java`
- Create: `backend/java/cunmo-api/src/main/java/cn/cunmo/api/inventory/model/request/AdjustStockRequest.java`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/inventory/service/InventoryItemApplicationService.java`
- Test: `backend/java/cunmo-application/src/test/java/cn/cunmo/application/inventory/service/InventoryItemApplicationServiceTest.java`

- [ ] 测试普通容量第 21 个物品返回 `CATEGORY_CAPACITY_EXCEEDED`。
- [ ] 测试无限容量不执行数量限制。
- [ ] 测试物品必须绑定有效空间分类关系。
- [ ] 测试库存更新和流水写入同一事务。
- [ ] 使用版本号条件更新，冲突返回 `INVENTORY_CONFLICT`。

### Task 6: 查询模型和游标分页

**Files:**
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/inventory/query/InventoryQueryService.java`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/inventory/result/InventoryBootstrapResult.java`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/inventory/result/InventoryItemPageResult.java`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/inventory/result/InventoryAnalyticsPageResult.java`
- Create: `backend/java/cunmo-application/src/main/java/cn/cunmo/application/inventory/result/StockTransactionPageResult.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/query/MybatisInventoryQueryService.java`
- Create: `backend/java/cunmo-infrastructure/src/main/java/cn/cunmo/infrastructure/persistence/inventory/mapper/InventoryQueryMapper.java`

- [ ] 测试物品按空间、分类和关键词过滤。
- [ ] 测试透视镜分别按空间和分类统计库存件数、物品数与占比。
- [ ] 测试流水按 `id < cursor` 稳定分页。
- [ ] 所有查询强制带当前用户魔方域。

### Task 7: HTTP 接口

**Files:**
- Create: `backend/java/cunmo-trigger/src/main/java/cn/cunmo/trigger/http/controller/InventoryController.java`
- Create: `backend/java/cunmo-trigger/src/main/java/cn/cunmo/trigger/http/converter/InventoryHttpConverter.java`
- Modify: `backend/java/cunmo-trigger/src/main/java/cn/cunmo/trigger/http/advice/GlobalExceptionHandler.java`
- Test: `backend/java/cunmo-trigger/src/test/java/cn/cunmo/trigger/http/controller/InventoryControllerTest.java`

- [ ] 实现 bootstrap、空间、分类、物品、库存调整、透视统计和流水接口。
- [ ] 每个接口通过 `CurrentUserProvider` 获取当前用户。
- [ ] 增加容量满、解绑阻塞、库存不足和乐观锁冲突状态映射。
- [ ] 验证响应 DTO 不暴露持久化对象。

### Task 8: 前端库存 API

**Files:**
- Create: `src/services/inventoryApi.ts`
- Modify: `src/types/inventory.ts`
- Test: `src/services/inventoryApi.spec.ts`

- [ ] 定义 bootstrap、分页、空间、分类、创建物品和调整库存接口类型。
- [ ] 测试所有请求自动携带登录 Token。
- [ ] 测试分页响应保留 `nextCursor` 和 `hasMore`。
- [ ] 将 BIGINT 标识按字符串处理，避免 JavaScript 精度损失。

### Task 9: Pinia 服务端状态重构

**Files:**
- Modify: `src/stores/inventoryStore.ts`
- Modify: `src/stores/inventory.spec.ts`
- Remove runtime dependency: `src/data/mockInventory.ts`

- [ ] 登录后加载 bootstrap 和首批物品。
- [ ] 切换空间、分类和关键词时重载物品。
- [ ] 创建空间、分类、物品和调整库存改为异步服务端动作。
- [ ] 保存各列表游标、加载状态和错误状态。
- [ ] Mock 文件仅可保留为测试夹具，运行时不得导入。

### Task 10: 小程序交互联调

**Files:**
- Modify: `src/components/InventoryModals.vue`
- Modify: `src/components/SpaceNavigator.vue`
- Modify: `src/components/ItemList.vue`
- Modify: `src/components/LensView.vue`
- Modify: `src/components/AxisTimeline.vue`
- Modify: `src/components/ItemCard.vue`
- Modify: `src/pages/index/index.vue`

- [ ] 分类创建弹窗增加空间多选并显示只读容量权益。
- [ ] 物品创建显示当前组合用量，20/20 时禁用提交。
- [ ] 透视镜和流转轴滚动到底调用服务端游标分页。
- [ ] 保留空腔原地带参建货。
- [ ] 增加加载、空数据和错误重试状态。

### Task 11: 数据库种子与联调验证

**Files:**
- Create: `backend/mysql/004_inventory_seed.sql`
- Modify: `backend/README.md`

- [ ] 增加仅用于开发环境的示例库存数据脚本。
- [ ] 验证表结构无外键且所有字段和表均有注释。
- [ ] 使用 JDK 21 定向编译所有可用模块。
- [ ] 执行 Java 中文注释审计。
- [ ] 执行 `npm test`。
- [ ] 执行 `npm run build:mp-weixin`。
- [ ] 启动后端后调用登录、bootstrap、创建物品、调整库存、透视镜和流水接口完成联调。
