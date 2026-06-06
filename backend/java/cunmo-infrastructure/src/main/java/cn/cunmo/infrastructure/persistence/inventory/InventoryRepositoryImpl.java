package cn.cunmo.infrastructure.persistence.inventory;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.domain.inventory.model.aggregate.InventoryItem;
import cn.cunmo.domain.inventory.model.aggregate.InventoryVault;
import cn.cunmo.domain.inventory.model.aggregate.StockAdjustment;
import cn.cunmo.domain.inventory.model.valueobject.InventoryItemId;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;
import cn.cunmo.domain.inventory.repository.InventoryRepository;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventoryCategoryDO;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventoryItemDO;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventorySpaceDO;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventoryVaultDO;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.SpaceCategoryDO;
import cn.cunmo.infrastructure.persistence.inventory.dataobject.StockTransactionDO;
import cn.cunmo.infrastructure.persistence.inventory.mapper.InventoryCategoryMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.InventoryItemMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.InventorySpaceMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.InventoryVaultMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.SpaceCategoryMapper;
import cn.cunmo.infrastructure.persistence.inventory.mapper.StockTransactionMapper;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * MySQL 库存聚合仓储实现。
 */
@Repository
public class InventoryRepositoryImpl implements InventoryRepository {
    private static final Logger log =
            LoggerFactory.getLogger(InventoryRepositoryImpl.class);

    private final InventoryVaultMapper vaultMapper;
    private final InventorySpaceMapper spaceMapper;
    private final InventoryCategoryMapper categoryMapper;
    private final SpaceCategoryMapper bindingMapper;
    private final InventoryItemMapper itemMapper;
    private final StockTransactionMapper transactionMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建库存仓储。
     */
    public InventoryRepositoryImpl(
            InventoryVaultMapper vaultMapper,
            InventorySpaceMapper spaceMapper,
            InventoryCategoryMapper categoryMapper,
            SpaceCategoryMapper bindingMapper,
            InventoryItemMapper itemMapper,
            StockTransactionMapper transactionMapper,
            PlatformTransactionManager transactionManager) {
        this.vaultMapper = vaultMapper;
        this.spaceMapper = spaceMapper;
        this.categoryMapper = categoryMapper;
        this.bindingMapper = bindingMapper;
        this.itemMapper = itemMapper;
        this.transactionMapper = transactionMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 获取或初始化用户默认魔方域。
     */
    @Override
    public InventoryVault getOrCreateDefaultVault(long userId) {
        InventoryVaultDO existing = vaultMapper.selectByOwner(userId);
        if (existing != null) {
            return toDomain(existing);
        }
        try {
            transactionTemplate.executeWithoutResult(status ->
                    initializeDefaultVault(userId));
        } catch (DuplicateKeyException concurrentCreation) {
            log.info(
                    "event=inventory_bootstrap stage=concurrent_vault_creation userId={}",
                    userId);
        }
        return requireVault(userId);
    }

    /**
     * 读取当前用户有效魔方域。
     */
    @Override
    public InventoryVault requireVault(long userId) {
        InventoryVaultDO data = vaultMapper.selectByOwner(userId);
        if (data == null) {
            throw new ApplicationException(
                    "INVENTORY_VAULT_NOT_FOUND",
                    "库存魔方域不存在");
        }
        return toDomain(data);
    }

    /**
     * 使用数据库行锁读取当前用户魔方域。
     */
    @Override
    public InventoryVault lockVault(long userId) {
        InventoryVaultDO data = vaultMapper.selectByOwnerForUpdate(userId);
        if (data == null) {
            throw new ApplicationException(
                    "INVENTORY_VAULT_NOT_FOUND",
                    "库存魔方域不存在");
        }
        return toDomain(data);
    }

    /**
     * 判断空间分类绑定是否有效。
     */
    @Override
    public boolean bindingExists(
            VaultId vaultId,
            long spaceId,
            long categoryId) {
        return bindingMapper.countBinding(
                vaultId.value(),
                spaceId,
                categoryId) > 0;
    }

    /**
     * 统计空间分类组合有效物品数。
     */
    @Override
    public int countItems(
            VaultId vaultId,
            long spaceId,
            long categoryId) {
        return itemMapper.countActiveItems(
                vaultId.value(),
                spaceId,
                categoryId);
    }

    /**
     * 保存新物品并生成初始库存流水。
     */
    @Override
    public InventoryItem createItem(
            InventoryItem item,
            long operatorUserId) {
        InventoryItemDO data = toData(item);
        transactionTemplate.executeWithoutResult(status -> {
            itemMapper.insert(data);
            writeTransaction(
                    data,
                    item.quantity(),
                    0,
                    item.quantity(),
                    item.quantity() <= item.minimumQuantity()
                            ? "WARN"
                            : "IN",
                    item.quantity() <= item.minimumQuantity()
                            ? "创建物品并触发预警线"
                            : "创建物品并完成初始入库",
                    operatorUserId);
        });
        log.info(
                "event=inventory_item_create stage=persistence_committed vaultId={} itemId={} operatorUserId={}",
                item.vaultId().value(),
                data.getId(),
                operatorUserId);
        return requireItem(
                item.vaultId(),
                InventoryItemId.of(data.getId()));
    }

    /**
     * 按魔方域读取物品。
     */
    @Override
    public InventoryItem requireItem(
            VaultId vaultId,
            InventoryItemId itemId) {
        InventoryItemDO data = itemMapper.selectOwnedItem(
                vaultId.value(),
                itemId.value());
        if (data == null) {
            throw new ApplicationException(
                    "ITEM_NOT_FOUND",
                    "库存物品不存在");
        }
        return toDomain(data);
    }

    /**
     * 使用乐观锁保存库存调整和流水。
     */
    @Override
    public InventoryItem saveAdjustment(
            InventoryItem item,
            StockAdjustment adjustment,
            long operatorUserId,
            String description) {
        transactionTemplate.executeWithoutResult(status -> {
            int affected = itemMapper.updateQuantityWithVersion(
                    item.vaultId().value(),
                    item.id().value(),
                    item.quantity(),
                    item.version());
            if (affected != 1) {
                throw new ApplicationException(
                        "INVENTORY_CONFLICT",
                        "库存已被其他操作更新，请刷新后重试");
            }
            InventoryItemDO snapshot = toData(item);
            snapshot.setId(item.id().value());
            writeTransaction(
                    snapshot,
                    adjustment.delta(),
                    adjustment.quantityBefore(),
                    adjustment.quantityAfter(),
                    adjustment.type().name(),
                    normalizeDescription(description, adjustment),
                    operatorUserId);
        });
        log.info(
                "event=inventory_stock_adjust stage=persistence_committed vaultId={} itemId={} delta={} operatorUserId={}",
                item.vaultId().value(),
                item.id().value(),
                adjustment.delta(),
                operatorUserId);
        return requireItem(item.vaultId(), item.id());
    }

    /**
     * 创建库存空间。
     */
    @Override
    public long createSpace(VaultId vaultId, String name) {
        InventorySpaceDO space = new InventorySpaceDO();
        space.setVaultId(vaultId.value());
        space.setSpaceCode(dynamicCode("space"));
        space.setSpaceName(requireText(name));
        space.setSortOrder(100);
        space.setStatus(1);
        space.setVersion(0);
        space.setDeleted(0);
        try {
            spaceMapper.insert(space);
        } catch (DuplicateKeyException duplicate) {
            throw new ApplicationException(
                    "DUPLICATE_SPACE",
                    "空间已存在",
                    duplicate);
        }
        return space.getId();
    }

    /**
     * 创建分类并绑定多个空间。
     */
    @Override
    public long createCategory(
            VaultId vaultId,
            String name,
            List<Long> spaceIds) {
        List<Long> normalizedIds = normalizeSpaceIds(spaceIds);
        return transactionTemplate.execute(status -> {
            assertSpacesOwned(vaultId, normalizedIds);
            InventoryCategoryDO category = new InventoryCategoryDO();
            category.setVaultId(vaultId.value());
            category.setCategoryCode(dynamicCode("category"));
            category.setCategoryName(requireText(name));
            category.setSortOrder(100);
            category.setStatus(1);
            category.setVersion(0);
            category.setDeleted(0);
            try {
                categoryMapper.insert(category);
            } catch (DuplicateKeyException duplicate) {
                throw new ApplicationException(
                        "DUPLICATE_CATEGORY",
                        "分类已存在",
                        duplicate);
            }
            bindSpaces(vaultId, category.getId(), normalizedIds);
            return category.getId();
        });
    }

    /**
     * 更新分类绑定空间集合。
     */
    @Override
    public void updateCategorySpaces(
            VaultId vaultId,
            long categoryId,
            List<Long> spaceIds) {
        List<Long> normalizedIds = normalizeSpaceIds(spaceIds);
        transactionTemplate.executeWithoutResult(status -> {
            assertCategoryOwned(vaultId, categoryId);
            assertSpacesOwned(vaultId, normalizedIds);
            Set<Long> requested = new HashSet<>(normalizedIds);
            List<Long> current = bindingMapper.selectSpaceIds(
                    vaultId.value(),
                    categoryId);
            for (Long spaceId : current) {
                if (!requested.contains(spaceId)) {
                    if (countItems(vaultId, spaceId, categoryId) > 0) {
                        throw new ApplicationException(
                                "CATEGORY_UNBIND_BLOCKED",
                                "待解绑空间分类下仍存在物品");
                    }
                    bindingMapper.deleteBinding(
                            vaultId.value(),
                            categoryId,
                            spaceId);
                }
            }
            Set<Long> currentSet = new HashSet<>(current);
            bindSpaces(
                    vaultId,
                    categoryId,
                    normalizedIds.stream()
                            .filter(id -> !currentSet.contains(id))
                            .toList());
        });
    }

    /**
     * 初始化默认域、空间、分类和绑定。
     */
    private void initializeDefaultVault(long userId) {
        InventoryVaultDO vault = new InventoryVaultDO();
        vault.setOwnerUserId(userId);
        vault.setVaultName("我的存量魔方");
        vault.setItemLimitPerSpaceCategory(20);
        vault.setStatus(1);
        vault.setVersion(0);
        vault.setDeleted(0);
        vaultMapper.insert(vault);

        long bedroom = insertSpace(vault.getId(), "bedroom", "卧室", 10);
        long living = insertSpace(vault.getId(), "living", "客厅", 20);
        long office = insertSpace(vault.getId(), "office", "办公室", 30);
        long digital = insertCategory(
                vault.getId(), "digital", "电子数码", 10);
        long clothes = insertCategory(
                vault.getId(), "clothes", "衣物服饰", 20);
        long household = insertCategory(
                vault.getId(), "household", "生活起居", 30);
        bindSpaces(
                VaultId.of(vault.getId()),
                clothes,
                List.of(bedroom));
        bindSpaces(
                VaultId.of(vault.getId()),
                household,
                List.of(bedroom, living));
        bindSpaces(
                VaultId.of(vault.getId()),
                digital,
                List.of(living, office));
        log.info(
                "event=inventory_bootstrap stage=default_vault_created userId={} vaultId={}",
                userId,
                vault.getId());
    }

    /**
     * 插入默认空间。
     */
    private long insertSpace(
            long vaultId,
            String code,
            String name,
            int sortOrder) {
        InventorySpaceDO data = new InventorySpaceDO();
        data.setVaultId(vaultId);
        data.setSpaceCode(code);
        data.setSpaceName(name);
        data.setSortOrder(sortOrder);
        data.setStatus(1);
        data.setVersion(0);
        data.setDeleted(0);
        spaceMapper.insert(data);
        return data.getId();
    }

    /**
     * 插入默认分类。
     */
    private long insertCategory(
            long vaultId,
            String code,
            String name,
            int sortOrder) {
        InventoryCategoryDO data = new InventoryCategoryDO();
        data.setVaultId(vaultId);
        data.setCategoryCode(code);
        data.setCategoryName(name);
        data.setSortOrder(sortOrder);
        data.setStatus(1);
        data.setVersion(0);
        data.setDeleted(0);
        categoryMapper.insert(data);
        return data.getId();
    }

    /**
     * 批量写入空间分类绑定。
     */
    private void bindSpaces(
            VaultId vaultId,
            long categoryId,
            List<Long> spaceIds) {
        int order = 10;
        for (Long spaceId : spaceIds) {
            SpaceCategoryDO binding = new SpaceCategoryDO();
            binding.setVaultId(vaultId.value());
            binding.setSpaceId(spaceId);
            binding.setCategoryId(categoryId);
            binding.setSortOrder(order);
            bindingMapper.insertBinding(binding);
            order += 10;
        }
    }

    /**
     * 写入库存流水快照。
     */
    private void writeTransaction(
            InventoryItemDO item,
            int delta,
            int before,
            int after,
            String type,
            String description,
            long operatorUserId) {
        InventorySpaceDO space = spaceMapper.selectById(item.getSpaceId());
        InventoryCategoryDO category = categoryMapper.selectById(
                item.getCategoryId());
        StockTransactionDO transaction = new StockTransactionDO();
        transaction.setVaultId(item.getVaultId());
        transaction.setItemId(item.getId());
        transaction.setTransactionType(type);
        transaction.setQuantityDelta(delta);
        transaction.setQuantityBefore(before);
        transaction.setQuantityAfter(after);
        transaction.setDescription(description);
        transaction.setItemNameSnapshot(item.getItemName());
        transaction.setSpaceSnapshot(space.getSpaceName());
        transaction.setCategorySnapshot(category.getCategoryName());
        transaction.setDetailLocationSnapshot(item.getDetailLocation());
        transaction.setOperatorUserId(operatorUserId);
        transaction.setOccurredAt(Instant.now());
        transactionMapper.insert(transaction);
    }

    /**
     * 校验空间均属于当前魔方域。
     */
    private void assertSpacesOwned(
            VaultId vaultId,
            List<Long> spaceIds) {
        for (Long spaceId : spaceIds) {
            InventorySpaceDO space = spaceMapper.selectById(spaceId);
            if (space == null
                    || !vaultId.value().equals(space.getVaultId())
                    || !Integer.valueOf(1).equals(space.getStatus())) {
                throw new ApplicationException(
                        "SPACE_NOT_FOUND",
                        "库存空间不存在");
            }
        }
    }

    /**
     * 校验分类属于当前魔方域。
     */
    private void assertCategoryOwned(
            VaultId vaultId,
            long categoryId) {
        InventoryCategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null
                || !vaultId.value().equals(category.getVaultId())
                || !Integer.valueOf(1).equals(category.getStatus())) {
            throw new ApplicationException(
                    "CATEGORY_NOT_FOUND",
                    "库存分类不存在");
        }
    }

    /**
     * 规范空间主键列表。
     */
    private List<Long> normalizeSpaceIds(List<Long> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) {
            throw new ApplicationException(
                    "INVALID_REQUEST",
                    "至少选择一个适用空间");
        }
        return spaceIds.stream().distinct().toList();
    }

    /**
     * 生成动态业务编码。
     */
    private String dynamicCode(String prefix) {
        return prefix + "_" + System.currentTimeMillis();
    }

    /**
     * 校验必填文本。
     */
    private String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new ApplicationException(
                    "INVALID_REQUEST",
                    "名称不能为空");
        }
        return value.trim();
    }

    /**
     * 生成默认库存调整说明。
     */
    private String normalizeDescription(
            String description,
            StockAdjustment adjustment) {
        if (description != null && !description.isBlank()) {
            return description.trim();
        }
        return adjustment.delta() > 0
                ? "手动入库"
                : "手动出库";
    }

    /**
     * 将魔方域数据库对象转换为领域对象。
     */
    private InventoryVault toDomain(InventoryVaultDO data) {
        return InventoryVault.reconstitute(
                VaultId.of(data.getId()),
                data.getItemLimitPerSpaceCategory());
    }

    /**
     * 将库存物品数据库对象转换为领域对象。
     */
    private InventoryItem toDomain(InventoryItemDO data) {
        return InventoryItem.reconstitute(
                InventoryItemId.of(data.getId()),
                VaultId.of(data.getVaultId()),
                data.getSpaceId(),
                data.getCategoryId(),
                data.getItemName(),
                data.getSmallCategory(),
                data.getDetailLocation(),
                data.getQuantity(),
                data.getMinimumQuantity(),
                data.getVersion());
    }

    /**
     * 将库存物品领域对象转换为数据库对象。
     */
    private InventoryItemDO toData(InventoryItem item) {
        InventoryItemDO data = new InventoryItemDO();
        data.setVaultId(item.vaultId().value());
        data.setSpaceId(item.spaceId());
        data.setCategoryId(item.categoryId());
        data.setItemName(item.name());
        data.setSmallCategory(item.smallCategory());
        data.setDetailLocation(item.detailLocation());
        data.setQuantity(item.quantity());
        data.setMinimumQuantity(item.minimumQuantity());
        data.setStatus(1);
        data.setVersion(item.version());
        data.setDeleted(0);
        return data;
    }
}
