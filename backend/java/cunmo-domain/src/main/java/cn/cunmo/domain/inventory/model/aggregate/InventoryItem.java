package cn.cunmo.domain.inventory.model.aggregate;

import cn.cunmo.domain.auth.exception.DomainException;
import cn.cunmo.domain.inventory.model.enums.StockTransactionType;
import cn.cunmo.domain.inventory.model.valueobject.InventoryItemId;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;

/**
 * 库存物品聚合根。
 */
public final class InventoryItem {
    private final InventoryItemId id;
    private final VaultId vaultId;
    private final long spaceId;
    private final long categoryId;
    private final String name;
    private final String smallCategory;
    private final String detailLocation;
    private int quantity;
    private final int minimumQuantity;
    private final int version;

    /**
     * 创建库存物品聚合。
     */
    private InventoryItem(
            InventoryItemId id,
            VaultId vaultId,
            long spaceId,
            long categoryId,
            String name,
            String smallCategory,
            String detailLocation,
            int quantity,
            int minimumQuantity,
            int version) {
        this.id = id;
        this.vaultId = vaultId;
        this.spaceId = spaceId;
        this.categoryId = categoryId;
        this.name = requireText(name, "物品名称不能为空");
        this.smallCategory = normalize(smallCategory);
        this.detailLocation = normalize(detailLocation);
        if (quantity < 0 || minimumQuantity < 0) {
            throw new IllegalArgumentException("库存数量和预警线不能为负数");
        }
        this.quantity = quantity;
        this.minimumQuantity = minimumQuantity;
        this.version = version;
    }

    /**
     * 创建尚未持久化的库存物品。
     */
    public static InventoryItem create(
            VaultId vaultId,
            long spaceId,
            long categoryId,
            String name,
            String smallCategory,
            String detailLocation,
            int quantity,
            int minimumQuantity) {
        return new InventoryItem(
                InventoryItemId.unassigned(),
                vaultId,
                spaceId,
                categoryId,
                name,
                smallCategory,
                detailLocation,
                quantity,
                minimumQuantity,
                0);
    }

    /**
     * 从持久化状态还原库存物品。
     */
    public static InventoryItem reconstitute(
            InventoryItemId id,
            VaultId vaultId,
            long spaceId,
            long categoryId,
            String name,
            String smallCategory,
            String detailLocation,
            int quantity,
            int minimumQuantity,
            int version) {
        return new InventoryItem(
                id,
                vaultId,
                spaceId,
                categoryId,
                name,
                smallCategory,
                detailLocation,
                quantity,
                minimumQuantity,
                version);
    }

    /**
     * 调整库存并返回用于生成流水的领域结果。
     */
    public StockAdjustment adjust(int delta) {
        if (delta == 0) {
            throw new DomainException(
                    "INVALID_STOCK_ADJUSTMENT",
                    "库存变化量不能为零");
        }
        int before = quantity;
        long candidate = (long) before + delta;
        if (candidate < 0) {
            throw new DomainException(
                    "INSUFFICIENT_STOCK",
                    "库存数量不足");
        }
        if (candidate > Integer.MAX_VALUE) {
            throw new DomainException(
                    "INVALID_STOCK_ADJUSTMENT",
                    "库存数量超出允许范围");
        }
        quantity = (int) candidate;
        StockTransactionType type = quantity <= minimumQuantity
                ? StockTransactionType.WARN
                : delta > 0
                        ? StockTransactionType.IN
                        : StockTransactionType.OUT;
        return new StockAdjustment(delta, before, quantity, type);
    }

    /**
     * 校验文本并去除首尾空白。
     */
    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * 将可选文本规范为空字符串。
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /** 返回物品标识。 */
    public InventoryItemId id() {
        return id;
    }

    /** 返回魔方域标识。 */
    public VaultId vaultId() {
        return vaultId;
    }

    /** 返回空间主键。 */
    public long spaceId() {
        return spaceId;
    }

    /** 返回分类主键。 */
    public long categoryId() {
        return categoryId;
    }

    /** 返回物品名称。 */
    public String name() {
        return name;
    }

    /** 返回细分类。 */
    public String smallCategory() {
        return smallCategory;
    }

    /** 返回精准位置。 */
    public String detailLocation() {
        return detailLocation;
    }

    /** 返回当前数量。 */
    public int quantity() {
        return quantity;
    }

    /** 返回最低预警线。 */
    public int minimumQuantity() {
        return minimumQuantity;
    }

    /** 返回乐观锁版本。 */
    public int version() {
        return version;
    }
}
