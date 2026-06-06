package cn.cunmo.api.inventory.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 调整库存请求。
 */
public record AdjustStockRequest(
        @Min(value = -1000000, message = "库存变化量过小")
        @Max(value = 1000000, message = "库存变化量过大")
        int delta,
        @Size(max = 255, message = "库存说明不能超过255个字符")
        String description) {
}
