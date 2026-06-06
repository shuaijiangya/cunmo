package cn.cunmo.api.inventory.model.request;

import jakarta.validation.constraints.NotNull;

/**
 * 空间删除请求。
 *
 * @param strategy 删除策略
 * @param targetSpaceId 统一迁移目标空间主键
 */
public record DeleteSpaceRequest(
        @NotNull DeletionStrategyRequest strategy,
        Long targetSpaceId) {
}
