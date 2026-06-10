package cn.cunmo.api.membership.model.response;

import java.time.Instant;
import java.util.List;

public record MembershipDashboardResponse(
        Plan plan,
        Quota quota,
        List<Product> products,
        Renewal renewal,
        Contact contact) {

    public record Plan(
            String type,
            String displayName,
            Instant effectiveAt,
            Instant expiresAt) {
    }

    public record Quota(
            QuotaUsage rootSpaces,
            List<CategoryQuotaUsage> constrainedCategories,
            Integer itemCapacityLimit,
            List<CapacityAlert> capacityAlerts) {
    }

    public record QuotaUsage(
            int used,
            Integer limit,
            int percent,
            boolean reached) {
    }

    public record CategoryQuotaUsage(
            long spaceId,
            String spaceName,
            int used,
            Integer limit,
            int percent,
            boolean reached) {
    }

    public record CapacityAlert(
            long spaceId,
            String spaceName,
            long categoryId,
            String categoryName,
            int used,
            int limit,
            int percent,
            String status) {
    }

    public record Product(
            String code,
            String name,
            int priceFen,
            String priceText,
            Integer durationDays,
            boolean recommended) {
    }

    public record Renewal(
            boolean supported,
            boolean enabled,
            String status,
            Instant nextChargeAt) {
    }

    public record Contact(
            boolean customerServiceEnabled,
            String enterpriseWechatQrUrl,
            String phone) {
    }
}
