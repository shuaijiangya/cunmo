package cn.cunmo.infrastructure.persistence.membership;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.application.membership.port.MembershipRepository;
import cn.cunmo.domain.membership.model.aggregate.MembershipEntitlement;
import cn.cunmo.domain.membership.model.enums.MembershipPlanType;
import cn.cunmo.domain.membership.model.enums.MembershipProductCode;
import cn.cunmo.infrastructure.persistence.membership.mapper.MembershipMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipRepositoryImpl implements MembershipRepository {
    private final MembershipMapper mapper;

    public MembershipRepositoryImpl(MembershipMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public MembershipEntitlement findEntitlement(long userId) {
        MembershipMapper.EntitlementRow row =
                mapper.selectEntitlement(userId);
        return row == null
                ? MembershipEntitlement.free(userId)
                : MembershipEntitlement.reconstitute(
                        row.userId(),
                        MembershipPlanType.valueOf(row.planType()),
                        row.effectiveAt(),
                        row.expiresAt(),
                        row.sourceReference());
    }

    @Override
    public void saveEntitlement(MembershipEntitlement entitlement) {
        mapper.upsertEntitlement(
                entitlement.userId(),
                entitlement.planType().name(),
                entitlement.effectiveAt(),
                entitlement.expiresAt(),
                entitlement.sourceReference());
    }

    @Override
    public void insertOrder(
            String orderNo,
            long userId,
            MembershipProductCode product,
            boolean autoRenew) {
        mapper.insertOrder(
                orderNo,
                userId,
                product.name(),
                product.priceFen(),
                autoRenew);
    }

    @Override
    public String requireWechatOpenId(long userId) {
        String openId = mapper.selectWechatOpenId(userId);
        if (openId == null || openId.isBlank()) {
            throw new ApplicationException(
                    "WECHAT_IDENTITY_NOT_FOUND",
                    "当前用户未绑定微信身份");
        }
        return openId;
    }

    @Override
    public void markOrderPaying(String orderNo, String prepayId) {
        mapper.markOrderPaying(orderNo, prepayId);
    }

    @Override
    public Optional<OrderState> findOrder(
            long userId,
            String orderNo) {
        return Optional.ofNullable(
                toOrder(mapper.selectOwnedOrder(userId, orderNo)));
    }

    @Override
    public Optional<OrderState> lockOrder(String orderNo) {
        return Optional.ofNullable(
                toOrder(mapper.selectOrderForUpdate(orderNo)));
    }

    @Override
    public boolean markOrderPaid(
            String orderNo,
            String transactionId,
            Instant paidAt) {
        return mapper.markOrderPaid(
                orderNo,
                transactionId,
                paidAt) == 1;
    }

    @Override
    public boolean registerNotification(
            String notificationId,
            String type,
            String businessReference) {
        return mapper.insertNotification(
                notificationId,
                type,
                businessReference) == 1;
    }

    @Override
    public QuotaSnapshot loadQuotaSnapshot(
            long userId,
            boolean pro) {
        return new QuotaSnapshot(
                mapper.countRootSpaces(userId),
                mapper.selectSpaceQuotas(userId).stream()
                        .map(row -> new SpaceQuota(
                                row.spaceId(),
                                row.spaceName(),
                                row.categoryCount()))
                        .toList(),
                mapper.selectCapacityUsages(userId).stream()
                        .map(row -> new CapacityUsage(
                                row.spaceId(),
                                row.spaceName(),
                                row.categoryId(),
                                row.categoryName(),
                                row.itemCount()))
                        .toList());
    }

    @Override
    public RenewalState findRenewal(long userId) {
        MembershipMapper.RenewalRow row =
                mapper.selectRenewal(userId);
        return row == null
                ? RenewalState.none()
                : new RenewalState(
                        row.id(),
                        row.userId(),
                        row.status(),
                        "ACTIVE".equals(row.status()),
                        row.nextChargeAt(),
                        row.contractCode(),
                        row.wechatContractId());
    }

    @Override
    public Optional<RenewalState> findRenewalByContractCode(
            String contractCode) {
        MembershipMapper.RenewalRow row =
                mapper.selectRenewalByContractCode(contractCode);
        if (row == null) return Optional.empty();
        return Optional.of(new RenewalState(
                row.id(),
                row.userId(),
                row.status(),
                "ACTIVE".equals(row.status()),
                row.nextChargeAt(),
                row.contractCode(),
                row.wechatContractId()));
    }

    @Override
    public List<RenewalState> findDueRenewals(
            Instant dueAt,
            int limit) {
        return mapper.selectDueRenewals(dueAt, limit).stream()
                .map(row -> new RenewalState(
                        row.id(),
                        row.userId(),
                        row.status(),
                        true,
                        row.nextChargeAt(),
                        row.contractCode(),
                        row.wechatContractId()))
                .toList();
    }

    @Override
    public void savePendingRenewal(
            long userId,
            String contractCode) {
        mapper.savePendingRenewal(userId, contractCode);
    }

    @Override
    public void activateRenewal(
            String contractCode,
            String wechatContractId,
            Instant nextChargeAt) {
        mapper.activateRenewal(
                contractCode,
                wechatContractId,
                nextChargeAt);
    }

    @Override
    public void terminateRenewal(
            long userId,
            Instant terminatedAt) {
        mapper.terminateRenewal(userId, terminatedAt);
    }

    @Override
    public void scheduleNextRenewal(
            long userId,
            Instant nextChargeAt) {
        mapper.scheduleNextRenewal(userId, nextChargeAt);
    }

    @Override
    public void insertRenewalAttempt(
            long agreementId,
            String attemptNo,
            int amountFen,
            Instant scheduledAt) {
        mapper.insertRenewalAttempt(
                agreementId,
                attemptNo,
                amountFen,
                scheduledAt);
    }

    @Override
    public void markRenewalAttemptFailed(
            String attemptNo,
            String reason,
            Instant retryAt,
            int maxAttempts) {
        mapper.failRenewalAttempt(attemptNo, reason);
        mapper.recordRenewalFailure(
                attemptNo,
                retryAt,
                maxAttempts);
    }

    @Override
    public void markRenewalAttemptPaid(
            String attemptNo,
            String transactionId,
            Instant paidAt) {
        mapper.payRenewalAttempt(
                attemptNo,
                transactionId,
                paidAt);
    }

    @Override
    public int expireMonthlyEntitlements(
            Instant expiredAt,
            int limit) {
        return mapper.expireMonthlyEntitlements(expiredAt, limit);
    }

    @Override
    public UpgradeState createOrGetPendingUpgrade(
            long userId,
            String contact,
            String remark) {
        MembershipMapper.UpgradeRow existing =
                mapper.selectPendingUpgrade(userId);
        if (existing != null) return toUpgrade(existing);
        mapper.insertUpgrade(userId, contact, remark);
        return new UpgradeState(
                mapper.lastInsertId(),
                userId,
                contact,
                remark,
                "PENDING",
                null,
                null,
                null,
                Instant.now(),
                null);
    }

    @Override
    public List<UpgradeState> findUpgradeRequests(
            String status,
            int limit) {
        return mapper.selectUpgrades(status, limit).stream()
                .map(this::toUpgrade)
                .toList();
    }

    @Override
    public Optional<UpgradeState> lockUpgradeRequest(long requestId) {
        return Optional.ofNullable(
                toUpgrade(mapper.selectUpgradeForUpdate(requestId)));
    }

    @Override
    public void approveUpgrade(
            long requestId,
            long reviewerUserId,
            String grantType,
            Integer months,
            String note,
            Instant reviewedAt) {
        mapper.approveUpgrade(
                requestId,
                reviewerUserId,
                grantType,
                months,
                note,
                reviewedAt);
    }

    @Override
    public void rejectUpgrade(
            long requestId,
            long reviewerUserId,
            String note,
            Instant reviewedAt) {
        mapper.rejectUpgrade(
                requestId,
                reviewerUserId,
                note,
                reviewedAt);
    }

    private OrderState toOrder(MembershipMapper.OrderRow row) {
        if (row == null) return null;
        return new OrderState(
                row.orderNo(),
                row.userId(),
                MembershipProductCode.valueOf(row.productCode()),
                row.amountFen(),
                row.autoRenew(),
                row.status(),
                row.prepayId());
    }

    private UpgradeState toUpgrade(MembershipMapper.UpgradeRow row) {
        if (row == null) return null;
        return new UpgradeState(
                row.id(),
                row.userId(),
                row.contact(),
                row.remark(),
                row.status(),
                row.grantType(),
                row.grantMonths(),
                row.reviewNote(),
                row.createdAt(),
                row.reviewedAt());
    }
}
