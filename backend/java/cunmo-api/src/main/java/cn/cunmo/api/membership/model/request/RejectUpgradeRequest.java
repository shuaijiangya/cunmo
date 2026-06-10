package cn.cunmo.api.membership.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectUpgradeRequest(
        @NotBlank @Size(max = 500) String reviewNote) {
}
