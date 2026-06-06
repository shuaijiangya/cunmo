package cn.cunmo.api.inventory.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建库存分类及空间绑定请求。
 */
public record CreateCategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称不能超过64个字符")
        String name,
        @NotEmpty(message = "至少选择一个适用空间")
        List<Long> spaceIds) {

    /**
     * 复制空间主键列表为不可变集合。
     */
    public CreateCategoryRequest {
        spaceIds = spaceIds == null ? List.of() : List.copyOf(spaceIds);
    }
}
