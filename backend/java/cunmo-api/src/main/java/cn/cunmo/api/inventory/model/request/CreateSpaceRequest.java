package cn.cunmo.api.inventory.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建库存空间请求。
 */
public record CreateSpaceRequest(
        @NotBlank(message = "空间名称不能为空")
        @Size(max = 64, message = "空间名称不能超过64个字符")
        String name) {
}
