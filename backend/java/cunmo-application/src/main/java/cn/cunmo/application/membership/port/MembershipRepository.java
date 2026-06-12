package cn.cunmo.application.membership.port;

import cn.cunmo.domain.membership.model.aggregate.MembershipEntitlement;
import cn.cunmo.domain.membership.model.enums.MembershipProductCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 会员聚合与读模型持久化端口。
 */
public interface MembershipRepository {
    MembershipEntitlement findEntitlement(long userId);

    void saveEntitlement(MembershipEntitlement entitlement);

    void insertOrder(
            String orderNo,
            long userId,
            MembershipProductCode product,
            boolean autoRenew);

    String requireWechatOpenId(long userId);

    void markOrderPaying(String orderNo, String prepayId);

    Optional<OrderState> findOrder(long userId, String orderNo);

    Optional<OrderState> lockOrder(String orderNo);

    boolean markOrderPaid(
            String orderNo,
            String transactionId,
            Instant paidAt);

    boolean registerNotification(
            String notificationId,
            String type,
            String businessReference);

    QuotaSnapshot loadQuotaSnapshot(long userId, boolean pro);

    RenewalState findRenewal(long userId);

    Optional<RenewalState> findRenewalByContractCode(
            String contractCode);

    List<RenewalState> findDueRenewals(Instant dueAt, int limit);

    void savePendingRenewal(long userId, String contractCode);

    void activateRenewal(
            String contractCode,
            String wechatContractId,
            Instant nextChargeAt);

    void terminateRenewal(long userId, Instant terminatedAt);

    void scheduleNextRenewal(long userId, Instant nextChargeAt);

    void insertRenewalAttempt(
            long agreementId,
            String attemptNo,
            int amountFen,
            Instant scheduledAt);

    void markRenewalAttemptFailed(
            String attemptNo,
            String reason,
            Instant retryAt,
            int maxAttempts);

    void markRenewalAttemptPaid(
            String attemptNo,
            String transactionId,
            Instant paidAt);

    int expireMonthlyEntitlements(Instant expiredAt, int limit);

    UpgradeState createOrGetPendingUpgrade(
            long userId,
            String contact,
            String remark);

    List<UpgradeState> findUpgradeRequests(String status, int limit);

    Optional<UpgradeState> lockUpgradeRequest(long requestId);

    void approveUpgrade(
            long requestId,
            long reviewerUserId,
            String grantType,
            Integer months,
            String note,
            Instant reviewedAt);

    void rejectUpgrade(
            long requestId,
            long reviewerUserId,
            String note,
            Instant reviewedAt);

    record OrderState(
            String orderNo,
            long userId,
            MembershipProductCode productCode,
            int amountFen,
            boolean autoRenew,
            String status,
            String prepayId) {
    }

    record RenewalState(
            long agreementId,
            long userId,
            String status,
            boolean enabled,
            Instant nextChargeAt,
            String contractCode,
            String wechatContractId) {
        public static RenewalState none() {
            return new RenewalState(
                    0, 0, "NONE", false, null, null, null);
        }
    }

    record SpaceQuota(
            long spaceId,
            String spaceName,
            int categoryCount) {
    }

    record CapacityUsage(
            long spaceId,
            String spaceName,
            long categoryId,
            String categoryName,
            int itemCount) {
    }

    record QuotaSnapshot(
            int rootSpaceCount,
            List<SpaceQuota> spaces,
            List<CapacityUsage> capacities) {
    }

    record UpgradeState(
            long id,
            long userId,
            String contact,
            String remark,
            String status,
            String grantType,
            Integer grantMonths,
            String reviewNote,
            Instant createdAt,
            Instant reviewedAt) {
    }
}
