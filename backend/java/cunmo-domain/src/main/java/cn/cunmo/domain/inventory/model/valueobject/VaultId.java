package cn.cunmo.domain.inventory.model.valueobject;

/**
 * 库存魔方域标识。
 */
public record VaultId(Long value) {

    /**
     * 校验魔方域主键。
     */
    public VaultId {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException("魔方域主键必须为正数");
        }
    }

    /**
     * 创建已分配魔方域标识。
     */
    public static VaultId of(long value) {
        return new VaultId(value);
    }

    /**
     * 创建未分配魔方域标识。
     */
    public static VaultId unassigned() {
        return new VaultId(null);
    }
}
