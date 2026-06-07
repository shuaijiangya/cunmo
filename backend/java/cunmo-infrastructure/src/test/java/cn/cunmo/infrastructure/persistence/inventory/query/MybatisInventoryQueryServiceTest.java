//package cn.cunmo.infrastructure.persistence.inventory.query;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//import cn.cunmo.application.inventory.result.InventoryBootstrapResult;
//import cn.cunmo.application.inventory.result.InventoryItemResult;
//import cn.cunmo.application.inventory.result.StockTransactionResult;
//import cn.cunmo.domain.inventory.model.valueobject.VaultId;
//import cn.cunmo.infrastructure.persistence.inventory.mapper.InventoryQueryMapper;
//import java.util.List;
//import org.junit.jupiter.api.Test;
//
///**
// * 库存读模型服务测试。
// */
//class MybatisInventoryQueryServiceTest {
//
//    /**
//     * 验证尚未绑定分类的新空间仍会出现在首屏结构中。
//     */
//    @Test
//    void includesSpaceWithoutCategories() {
//        InventoryQueryMapper mapper = new EmptySpaceQueryMapper();
//        MybatisInventoryQueryService service =
//                new MybatisInventoryQueryService(mapper);
//
//        InventoryBootstrapResult result = service.bootstrap(
//                VaultId.of(1L));
//
//        assertEquals(1, result.spaces().size());
//        assertEquals("厨房", result.spaces().getFirst().name());
//        assertEquals(0, result.spaces().getFirst().categories().size());
//    }
//
//    /**
//     * 只返回一个空空间的查询 Mapper。
//     */
//    private static final class EmptySpaceQueryMapper
//            implements InventoryQueryMapper {
//
//        /**
//         * 返回测试魔方域汇总。
//         */
//        @Override
//        public InventoryVaultSummaryRow selectVaultSummary(long vaultId) {
//            return new InventoryVaultSummaryRow(
//                    vaultId,
//                    "测试魔方",
//                    20,
//                    0);
//        }
//
//        /**
//         * 返回一个没有分类绑定的空间。
//         */
//        @Override
//        public List<InventoryStructureRow> selectStructure(long vaultId) {
//            return List.of(new InventoryStructureRow(
//                    10L,
//                    "kitchen",
//                    "厨房",
//                    null,
//                    null,
//                    null,
//                    0));
//        }
//
//        /**
//         * 返回空物品列表。
//         */
//        @Override
//        public List<InventoryItemResult> selectItems(
//                long vaultId,
//                Long spaceId,
//                Long categoryId,
//                String keyword,
//                Long cursor,
//                int limit) {
//            return List.of();
//        }
//
//        /**
//         * 返回空空间统计。
//         */
//        @Override
//        public List<InventoryAnalyticsRow> selectSpaceAnalytics(
//                long vaultId,
//                Long cursor,
//                int limit) {
//            return List.of();
//        }
//
//        /**
//         * 返回空分类统计。
//         */
//        @Override
//        public List<InventoryAnalyticsRow> selectCategoryAnalytics(
//                long vaultId,
//                Long cursor,
//                int limit) {
//            return List.of();
//        }
//
//        /**
//         * 返回空流水。
//         */
//        @Override
//        public List<StockTransactionResult> selectTransactions(
//                long vaultId,
//                Long cursor,
//                int limit) {
//            return List.of();
//        }
//    }
//}
