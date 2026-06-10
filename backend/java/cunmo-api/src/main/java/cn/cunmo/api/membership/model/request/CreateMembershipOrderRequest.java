package cn.cunmo.api.membership.model.request;

import jakarta.validation.constraints.NotBlank;
public record CreateMembershipOrderRequest(
        @NotBlank String productCode,
        boolean autoRenew) {
}
