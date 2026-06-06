package cn.cunmo.api.inventory.model.request;

/**
 * 库存结构删除策略请求枚举。
 */
public enum DeletionStrategyRequest {
    /** 将受影响物品迁移到统一目标。 */
    MOVE,
    /** 清空受影响库存后删除。 */
    CLEAR_DELETE
}
