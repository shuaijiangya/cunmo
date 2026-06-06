package cn.cunmo.api.inventory.model.request;

import jakarta.validation.constraints.Positive;

/**
 * 物品迁移请求。
 *
 * @param targetSpaceId 目标空间主键
 * @param targetCategoryId 目标分类主键
 */
public record MoveInventoryItemRequest(
        @Positive long targetSpaceId,
        @Positive long targetCategoryId) {
}
