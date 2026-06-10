package cn.cunmo.api.membership.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveUpgradeRequest(
        @NotBlank String grantType,
        @Min(1) @Max(120) Integer months,
        @Size(max = 500) String reviewNote) {
}
