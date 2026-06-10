package cn.cunmo.application.inventory.service;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.application.inventory.query.InventoryQueryService;
import cn.cunmo.application.inventory.result.CursorPageResult;
import cn.cunmo.application.inventory.result.InventoryAnalyticsResult;
import cn.cunmo.application.inventory.result.InventoryBootstrapResult;
import cn.cunmo.application.inventory.result.InventoryItemResult;
import cn.cunmo.application.inventory.result.InventoryDeletionPreviewResult;
import cn.cunmo.application.inventory.result.StockTransactionResult;
import cn.cunmo.application.membership.service.MembershipQuotaPolicy;
import cn.cunmo.domain.inventory.model.aggregate.InventoryItem;
import cn.cunmo.domain.inventory.model.aggregate.InventoryVault;
import cn.cunmo.domain.inventory.model.aggregate.StockAdjustment;
import cn.cunmo.domain.inventory.model.valueobject.InventoryItemId;
import cn.cunmo.domain.inventory.repository.InventoryRepository;
import cn.cunmo.domain.membership.model.valueobject.MembershipQuota;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存领域应用服务。
 */
public class InventoryApplicationService {
    private static final Logger log =
            LoggerFactory.getLogger(InventoryApplicationService.class);

    private final InventoryRepository repository;
    private final InventoryQueryService queryService;
    private final MembershipQuotaPolicy quotaPolicy;

    /**
     * 创建库存应用服务。
     */
    public InventoryApplicationService(
            InventoryRepository repository,
            InventoryQueryService queryService,
            MembershipQuotaPolicy quotaPolicy) {
        this.repository = repository;
        this.queryService = queryService;
        this.quotaPolicy = quotaPolicy;
    }

    /**
     * 初始化并查询当前用户库存首屏。
     */
    public InventoryBootstrapResult bootstrap(long userId) {
        log.info(
                "event=inventory_bootstrap stage=application_started userId={}",
                userId);
        InventoryVault vault = repository.getOrCreateDefaultVault(userId);
        InventoryBootstrapResult result = queryService.bootstrap(vault.id());
        log.info(
                "event=inventory_bootstrap stage=application_completed userId={} vaultId={} spaceCount={}",
                userId,
                vault.id().value(),
                result.spaces().size());
        return result;
    }

    /**
     * 创建空间。
     */
    @Transactional
    public long createSpace(long userId, String name) {
        InventoryVault vault = repository.lockVault(userId);
        MembershipQuota quota = quotaPolicy.quotaFor(userId);
        assertBelowLimit(
                repository.countSpaces(vault.id()),
                quota.rootSpaceLimit(),
                "ROOT_SPACE_QUOTA_EXCEEDED",
                "免费版最多创建3个根空间");
        long id = repository.createSpace(vault.id(), name);
        log.info(
                "event=inventory_structure stage=space_created userId={} spaceId={}",
                userId,
                id);
        return id;
    }

    /**
     * 创建分类并绑定多个空间。
     */
    @Transactional
    public long createCategory(
            long userId,
            String name,
            List<Long> spaceIds) {
        InventoryVault vault = repository.lockVault(userId);
        MembershipQuota quota = quotaPolicy.quotaFor(userId);
        for (Long spaceId : new HashSet<>(spaceIds)) {
            assertBelowLimit(
                    repository.countCategories(vault.id(), spaceId),
                    quota.categoryLimitPerSpace(),
                    "SPACE_CATEGORY_QUOTA_EXCEEDED",
                    "免费版每个空间最多绑定3个分类");
        }
        long id = repository.createCategory(vault.id(), name, spaceIds);
        log.info(
                "event=inventory_structure stage=category_created userId={} categoryId={} spaceCount={}",
                userId,
                id,
                spaceIds.size());
        return id;
    }

    /**
     * 更新分类空间绑定。
     */
    @Transactional
    public void updateCategorySpaces(
            long userId,
            long categoryId,
            List<Long> spaceIds) {
        InventoryVault vault = repository.lockVault(userId);
        MembershipQuota quota = quotaPolicy.quotaFor(userId);
        Set<Long> current = new HashSet<>(
                repository.findCategorySpaceIds(
                        vault.id(),
                        categoryId));
        for (Long spaceId : new HashSet<>(spaceIds)) {
            if (!current.contains(spaceId)) {
                assertBelowLimit(
                        repository.countCategories(vault.id(), spaceId),
                        quota.categoryLimitPerSpace(),
                        "SPACE_CATEGORY_QUOTA_EXCEEDED",
                        "免费版每个空间最多绑定3个分类");
            }
        }
        repository.updateCategorySpaces(
                vault.id(),
                categoryId,
                spaceIds);
        log.info(
                "event=inventory_category_binding stage=application_completed userId={} categoryId={} spaceCount={}",
                userId,
                categoryId,
                spaceIds.size());
    }

    /**
     * 创建库存物品并校验组合容量。
     */
    @Transactional
    public InventoryItem createItem(
            long userId,
            long spaceId,
            long categoryId,
            String name,
            String smallCategory,
            String detailLocation,
            int quantity,
            int minimumQuantity) {
        InventoryVault vault = repository.lockVault(userId);
        if (!repository.bindingExists(vault.id(), spaceId, categoryId)) {
            throw new ApplicationException(
                    "CATEGORY_NOT_BOUND",
                    "分类未绑定到当前空间");
        }
        int currentCount = repository.countItems(
                vault.id(),
                spaceId,
                categoryId);
        assertBelowLimit(
                currentCount,
                quotaPolicy.quotaFor(userId).itemLimitPerCavity(),
                "CAVITY_ITEM_QUOTA_EXCEEDED",
                "免费版每个分类腔体最多存放10条物品记录");
        InventoryItem item = InventoryItem.create(
                vault.id(),
                spaceId,
                categoryId,
                name,
                smallCategory,
                detailLocation,
                quantity,
                minimumQuantity);
        InventoryItem created = repository.createItem(item, userId);
        log.info(
                "event=inventory_item_create stage=application_completed userId={} itemId={} quantity={}",
                userId,
                created.id().value(),
                created.quantity());
        return created;
    }

    /**
     * 调整库存并生成流转流水。
     */
    @Transactional
    public InventoryItem adjustStock(
            long userId,
            long itemId,
            int delta,
            String description) {
        InventoryVault vault = repository.requireVault(userId);
        InventoryItem item = repository.requireItem(
                vault.id(),
                InventoryItemId.of(itemId));
        StockAdjustment adjustment = item.adjust(delta);
        InventoryItem saved = repository.saveAdjustment(
                item,
                adjustment,
                userId,
                description);
        log.info(
                "event=inventory_stock_adjust stage=application_completed userId={} itemId={} quantity={}",
                userId,
                itemId,
                saved.quantity());
        return saved;
    }

    /**
     * 查询当前用户物品分页。
     */
    public CursorPageResult<InventoryItemResult> items(
            long userId,
            Long spaceId,
            Long categoryId,
            String keyword,
            Long cursor,
            int size) {
        InventoryVault vault = repository.requireVault(userId);
        return queryService.items(
                vault.id(),
                spaceId,
                categoryId,
                keyword,
                cursor,
                normalizeSize(size));
    }

    /**
     * 查询当前用户透视镜分页。
     */
    public CursorPageResult<InventoryAnalyticsResult> analytics(
            long userId,
            String dimension,
            Long cursor,
            int size) {
        InventoryVault vault = repository.requireVault(userId);
        return queryService.analytics(
                vault.id(),
                dimension,
                cursor,
                normalizeSize(size));
    }

    /**
     * 查询当前用户库存流水分页。
     */
    public CursorPageResult<StockTransactionResult> transactions(
            long userId,
            Long cursor,
            int size) {
        InventoryVault vault = repository.requireVault(userId);
        return queryService.transactions(
                vault.id(),
                cursor,
                normalizeSize(size));
    }

    /** 查询物品删除影响预览。 */
    public InventoryDeletionPreviewResult previewItemDeletion(
            long userId,
            long itemId) {
        InventoryVault vault = repository.requireVault(userId);
        return queryService.previewItemDeletion(vault.id(), itemId);
    }

    /** 查询空间删除影响预览。 */
    public InventoryDeletionPreviewResult previewSpaceDeletion(
            long userId,
            long spaceId) {
        InventoryVault vault = repository.requireVault(userId);
        return queryService.previewSpaceDeletion(vault.id(), spaceId);
    }

    /** 查询分类删除或解绑影响预览。 */
    public InventoryDeletionPreviewResult previewCategoryDeletion(
            long userId,
            long categoryId,
            Long spaceId) {
        InventoryVault vault = repository.requireVault(userId);
        return queryService.previewCategoryDeletion(
                vault.id(),
                categoryId,
                spaceId);
    }

    /**
     * 规范分页大小。
     */
    private int normalizeSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private void assertBelowLimit(
            int current,
            Integer limit,
            String code,
            String message) {
        if (limit != null && current >= limit) {
            throw new ApplicationException(code, message);
        }
    }
}
