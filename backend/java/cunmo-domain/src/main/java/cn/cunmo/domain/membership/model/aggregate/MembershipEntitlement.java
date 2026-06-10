package cn.cunmo.domain.membership.model.aggregate;

import cn.cunmo.domain.membership.model.enums.MembershipPlanType;
import cn.cunmo.domain.membership.model.valueobject.MembershipQuota;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 用户当前会员权益聚合。
 */
public final class MembershipEntitlement {
    private final long userId;
    private MembershipPlanType planType;
    private Instant effectiveAt;
    private Instant expiresAt;
    private String sourceReference;

    private MembershipEntitlement(
            long userId,
            MembershipPlanType planType,
            Instant effectiveAt,
            Instant expiresAt,
            String sourceReference) {
        this.userId = userId;
        this.planType = Objects.requireNonNull(planType);
        this.effectiveAt = effectiveAt;
        this.expiresAt = expiresAt;
        this.sourceReference = sourceReference;
    }

    public static MembershipEntitlement free(long userId) {
        return new MembershipEntitlement(
                userId,
                MembershipPlanType.FREE,
                null,
                null,
                null);
    }

    public static MembershipEntitlement reconstitute(
            long userId,
            MembershipPlanType planType,
            Instant effectiveAt,
            Instant expiresAt,
            String sourceReference) {
        return new MembershipEntitlement(
                userId,
                planType,
                effectiveAt,
                expiresAt,
                sourceReference);
    }

    public void grantMonthly(
            Instant grantedAt,
            int months,
            String reference) {
        if (months <= 0) {
            throw new IllegalArgumentException("会员月数必须大于0");
        }
        if (planType == MembershipPlanType.LIFETIME_PRO) {
            return;
        }
        Instant base = expiresAt != null && expiresAt.isAfter(grantedAt)
                ? expiresAt
                : grantedAt;
        if (effectiveAt == null || !isProAt(grantedAt)) {
            effectiveAt = grantedAt;
        }
        planType = MembershipPlanType.MONTHLY_PRO;
        expiresAt = base.plus((long) months * 30, ChronoUnit.DAYS);
        sourceReference = reference;
    }

    public void grantLifetime(Instant grantedAt, String reference) {
        planType = MembershipPlanType.LIFETIME_PRO;
        effectiveAt = grantedAt;
        expiresAt = null;
        sourceReference = reference;
    }

    public boolean isProAt(Instant instant) {
        if (planType == MembershipPlanType.LIFETIME_PRO) {
            return true;
        }
        return planType == MembershipPlanType.MONTHLY_PRO
                && expiresAt != null
                && expiresAt.isAfter(instant);
    }

    public MembershipPlanType effectivePlanAt(Instant instant) {
        return isProAt(instant) ? planType : MembershipPlanType.FREE;
    }

    public MembershipQuota quotaAt(Instant instant) {
        return isProAt(instant)
                ? MembershipQuota.unlimited()
                : MembershipQuota.free();
    }

    public long userId() {
        return userId;
    }

    public MembershipPlanType planType() {
        return planType;
    }

    public Instant effectiveAt() {
        return effectiveAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public String sourceReference() {
        return sourceReference;
    }
}
