package cn.cunmo.api.inventory.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 创建库存物品请求。
 */
public record CreateInventoryItemRequest(
        @Positive(message = "空间主键无效")
        long spaceId,
        @Positive(message = "分类主键无效")
        long categoryId,
        @NotBlank(message = "物品名称不能为空")
        @Size(max = 128, message = "物品名称不能超过128个字符")
        String name,
        @Size(max = 64, message = "细分类不能超过64个字符")
        String smallCategory,
        @Size(max = 128, message = "精准位置不能超过128个字符")
        String detailLocation,
        @Min(value = 0, message = "初始库存不能为负数")
        int quantity,
        @Min(value = 0, message = "预警线不能为负数")
        int minimumQuantity) {
}
