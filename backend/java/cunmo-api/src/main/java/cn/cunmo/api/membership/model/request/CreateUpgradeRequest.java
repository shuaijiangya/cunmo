package cn.cunmo.api.membership.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUpgradeRequest(
        @NotBlank @Size(max = 128) String contact,
        @Size(max = 500) String remark) {
}
