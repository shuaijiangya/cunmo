package cn.cunmo.domain.inventory.model.enums;

/**
 * 库存结构删除策略。
 */
public enum DeletionStrategy {
    /** 将受影响物品迁移到统一目标。 */
    MOVE,
    /** 清空受影响库存后删除。 */
    CLEAR_DELETE
}
