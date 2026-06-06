package cn.cunmo.api.inventory.model.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 更新分类空间绑定请求。
 */
public record UpdateCategorySpacesRequest(
        @NotEmpty(message = "至少保留一个适用空间")
        List<Long> spaceIds) {

    /**
     * 复制空间主键列表为不可变集合。
     */
    public UpdateCategorySpacesRequest {
        spaceIds = spaceIds == null ? List.of() : List.copyOf(spaceIds);
    }
}
