package cn.cunmo.domain.membership.model.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cunmo.domain.membership.model.enums.MembershipPlanType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class MembershipEntitlementTest {
    private static final Instant NOW =
            Instant.parse("2026-06-10T08:00:00Z");

    @Test
    void activatesMonthlyMembershipForThirtyDays() {
        MembershipEntitlement entitlement =
                MembershipEntitlement.free(42L);

        entitlement.grantMonthly(NOW, 1, "ORDER-1");

        assertEquals(MembershipPlanType.MONTHLY_PRO, entitlement.planType());
        assertEquals(NOW, entitlement.effectiveAt());
        assertEquals(NOW.plus(30, ChronoUnit.DAYS), entitlement.expiresAt());
        assertTrue(entitlement.isProAt(NOW.plus(29, ChronoUnit.DAYS)));
    }

    @Test
    void extendsActiveMonthlyMembershipFromExistingExpiry() {
        MembershipEntitlement entitlement =
                MembershipEntitlement.free(42L);
        entitlement.grantMonthly(NOW, 1, "ORDER-1");

        entitlement.grantMonthly(
                NOW.plus(10, ChronoUnit.DAYS),
                2,
                "ORDER-2");

        assertEquals(NOW.plus(90, ChronoUnit.DAYS), entitlement.expiresAt());
    }

    @Test
    void treatsExpiredMonthlyMembershipAsFree() {
        MembershipEntitlement entitlement =
                MembershipEntitlement.free(42L);
        entitlement.grantMonthly(NOW, 1, "ORDER-1");

        assertFalse(entitlement.isProAt(NOW.plus(31, ChronoUnit.DAYS)));
        assertEquals(
                MembershipPlanType.FREE,
                entitlement.effectivePlanAt(
                        NOW.plus(31, ChronoUnit.DAYS)));
    }

    @Test
    void lifetimeMembershipOverridesMonthlyMembership() {
        MembershipEntitlement entitlement =
                MembershipEntitlement.free(42L);
        entitlement.grantMonthly(NOW, 1, "ORDER-1");

        entitlement.grantLifetime(
                NOW.plus(1, ChronoUnit.DAYS),
                "ORDER-2");
        entitlement.grantMonthly(
                NOW.plus(2, ChronoUnit.DAYS),
                1,
                "ORDER-3");

        assertEquals(MembershipPlanType.LIFETIME_PRO, entitlement.planType());
        assertNull(entitlement.expiresAt());
        assertTrue(entitlement.isProAt(NOW.plus(10000, ChronoUnit.DAYS)));
    }
}
