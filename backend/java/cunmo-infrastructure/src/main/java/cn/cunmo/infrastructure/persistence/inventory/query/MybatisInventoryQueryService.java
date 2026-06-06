package cn.cunmo.infrastructure.persistence.inventory.query;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.application.inventory.query.InventoryQueryService;
import cn.cunmo.application.inventory.result.CursorPageResult;
import cn.cunmo.application.inventory.result.InventoryAnalyticsResult;
import cn.cunmo.application.inventory.result.InventoryBootstrapResult;
import cn.cunmo.application.inventory.result.InventoryCategoryResult;
import cn.cunmo.application.inventory.result.InventoryItemResult;
import cn.cunmo.application.inventory.result.InventoryDeletionPreviewResult;
import cn.cunmo.application.inventory.result.InventorySpaceResult;
import cn.cunmo.application.inventory.result.StockTransactionResult;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;
import cn.cunmo.infrastructure.persistence.inventory.mapper.InventoryQueryMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 基于 MyBatis 的库存读模型实现。
 */
@Component
public class MybatisInventoryQueryService
        implements InventoryQueryService {
    private final InventoryQueryMapper queryMapper;

    /**
     * 创建库存读模型服务。
     */
    public MybatisInventoryQueryService(
            InventoryQueryMapper queryMapper) {
        this.queryMapper = queryMapper;
    }

    /**
     * 查询首屏魔方域、空间、分类和容量信息。
     */
    @Override
    public InventoryBootstrapResult bootstrap(VaultId vaultId) {
        InventoryVaultSummaryRow summary =
                queryMapper.selectVaultSummary(vaultId.value());
        if (summary == null) {
            throw new ApplicationException(
                    "INVENTORY_VAULT_NOT_FOUND",
                    "库存魔方域不存在");
        }
        Map<Long, SpaceBuilder> spaces = new LinkedHashMap<>();
        for (InventoryStructureRow row
                : queryMapper.selectStructure(vaultId.value())) {
            SpaceBuilder builder = spaces.computeIfAbsent(
                    row.spaceId(),
                    ignored -> new SpaceBuilder(
                            row.spaceId(),
                            row.spaceCode(),
                            row.spaceName()));
            if (row.categoryId() == null) {
                continue;
            }
            Integer remaining = summary.itemLimitPerSpaceCategory() == null
                    ? null
                    : Math.max(
                            0,
                            summary.itemLimitPerSpaceCategory()
                                    - row.itemCount());
            builder.categories.add(new InventoryCategoryResult(
                    row.categoryId(),
                    row.categoryCode(),
                    row.categoryName(),
                    row.itemCount(),
                    remaining,
                    remaining != null && remaining == 0));
        }
        return new InventoryBootstrapResult(
                summary.vaultId(),
                summary.vaultName(),
                summary.totalQuantity(),
                summary.itemLimitPerSpaceCategory(),
                summary.itemLimitPerSpaceCategory() == null,
                spaces.values().stream()
                        .map(SpaceBuilder::build)
                        .toList());
    }

    /**
     * 查询物品游标分页。
     */
    @Override
    public CursorPageResult<InventoryItemResult> items(
            VaultId vaultId,
            Long spaceId,
            Long categoryId,
            String keyword,
            Long cursor,
            int size) {
        return page(
                queryMapper.selectItems(
                        vaultId.value(),
                        spaceId,
                        categoryId,
                        keyword == null ? null : keyword.trim(),
                        cursor,
                        size + 1),
                size,
                InventoryItemResult::id);
    }

    /**
     * 查询透视镜游标分页。
     */
    @Override
    public CursorPageResult<InventoryAnalyticsResult> analytics(
            VaultId vaultId,
            String dimension,
            Long cursor,
            int size) {
        List<InventoryAnalyticsRow> rows =
                "CATEGORY".equalsIgnoreCase(dimension)
                        ? queryMapper.selectCategoryAnalytics(
                                vaultId.value(), cursor, size + 1)
                        : queryMapper.selectSpaceAnalytics(
                                vaultId.value(), cursor, size + 1);
        long total = queryMapper.selectVaultSummary(vaultId.value())
                .totalQuantity();
        List<InventoryAnalyticsResult> results = rows.stream()
                .map(row -> new InventoryAnalyticsResult(
                        row.id(),
                        row.code(),
                        row.name(),
                        row.itemCount(),
                        row.quantity(),
                        total == 0
                                ? 0
                                : (int) Math.round(
                                        row.quantity() * 100.0 / total)))
                .toList();
        return page(results, size, InventoryAnalyticsResult::id);
    }

    /**
     * 查询库存流水游标分页。
     */
    @Override
    public CursorPageResult<StockTransactionResult> transactions(
            VaultId vaultId,
            Long cursor,
            int size) {
        return page(
                queryMapper.selectTransactions(
                        vaultId.value(),
                        cursor,
                        size + 1),
                size,
                StockTransactionResult::id);
    }

    /** 查询物品删除影响预览。 */
    @Override
    public InventoryDeletionPreviewResult previewItemDeletion(
            VaultId vaultId,
            long itemId) {
        return requirePreview(
                queryMapper.selectItemDeletionPreview(
                        vaultId.value(),
                        itemId),
                "ITEM_NOT_FOUND",
                "库存物品不存在");
    }

    /** 查询空间删除影响预览。 */
    @Override
    public InventoryDeletionPreviewResult previewSpaceDeletion(
            VaultId vaultId,
            long spaceId) {
        return requirePreview(
                queryMapper.selectSpaceDeletionPreview(
                        vaultId.value(),
                        spaceId),
                "SPACE_NOT_FOUND",
                "库存空间不存在");
    }

    /** 查询分类删除或解绑影响预览。 */
    @Override
    public InventoryDeletionPreviewResult previewCategoryDeletion(
            VaultId vaultId,
            long categoryId,
            Long spaceId) {
        return requirePreview(
                queryMapper.selectCategoryDeletionPreview(
                        vaultId.value(),
                        categoryId,
                        spaceId),
                "CATEGORY_NOT_FOUND",
                "库存分类不存在");
    }

    /** 校验删除预览查询结果存在。 */
    private InventoryDeletionPreviewResult requirePreview(
            InventoryDeletionPreviewResult preview,
            String code,
            String message) {
        if (preview == null) {
            throw new ApplicationException(code, message);
        }
        return preview;
    }

    /**
     * 将多取一条的查询结果转换为游标分页。
     */
    private <T> CursorPageResult<T> page(
            List<T> rows,
            int size,
            IdReader<T> idReader) {
        boolean hasMore = rows.size() > size;
        List<T> items = hasMore
                ? List.copyOf(rows.subList(0, size))
                : List.copyOf(rows);
        Long nextCursor = hasMore && !items.isEmpty()
                ? idReader.id(items.get(items.size() - 1))
                : null;
        return new CursorPageResult<>(items, nextCursor, hasMore);
    }

    /**
     * 分页数据主键读取函数。
     */
    @FunctionalInterface
    private interface IdReader<T> {
        /**
         * 返回分页元素主键。
         */
        long id(T value);
    }

    /**
     * 首屏空间聚合辅助对象。
     */
    private static final class SpaceBuilder {
        private final long id;
        private final String code;
        private final String name;
        private final List<InventoryCategoryResult> categories =
                new ArrayList<>();

        /**
         * 创建空间聚合辅助对象。
         */
        private SpaceBuilder(long id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        /**
         * 构建不可变空间结果。
         */
        private InventorySpaceResult build() {
            return new InventorySpaceResult(
                    id,
                    code,
                    name,
                    categories);
        }
    }
}
