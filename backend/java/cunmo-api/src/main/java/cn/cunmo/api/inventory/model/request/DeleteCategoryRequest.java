package cn.cunmo.api.inventory.model.request;

import jakarta.validation.constraints.NotNull;

/**
 * 分类全局删除请求。
 *
 * @param strategy 删除策略
 * @param targetCategoryId 统一迁移目标分类主键
 */
public record DeleteCategoryRequest(
        @NotNull DeletionStrategyRequest strategy,
        Long targetCategoryId) {
}
