package cn.cunmo.api.inventory.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 分类空间解绑请求。
 *
 * @param spaceId 当前空间主键
 * @param strategy 删除策略
 * @param targetCategoryId 统一迁移目标分类主键
 */
public record UnbindCategoryRequest(
        @Positive long spaceId,
        @NotNull DeletionStrategyRequest strategy,
        Long targetCategoryId) {
}
