package cn.cunmo.application.membership.service;

import cn.cunmo.api.membership.model.response.MembershipDashboardResponse;
import cn.cunmo.api.membership.model.response.MembershipOrderResponse;
import cn.cunmo.api.membership.model.response.RenewalAgreementResponse;
import cn.cunmo.api.membership.model.response.UpgradeRequestResponse;
import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.application.membership.port.MembershipPaymentGateway;
import cn.cunmo.application.membership.port.MembershipRepository;
import cn.cunmo.application.membership.port.MembershipSettings;
import cn.cunmo.domain.membership.model.aggregate.MembershipEntitlement;
import cn.cunmo.domain.membership.model.enums.MembershipPlanType;
import cn.cunmo.domain.membership.model.enums.MembershipProductCode;
import cn.cunmo.domain.membership.model.valueobject.MembershipQuota;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会员看板、购买、续费和升级申请应用服务。
 */
public class MembershipApplicationService {
    private final MembershipRepository repository;
    private final MembershipPaymentGateway paymentGateway;
    private final MembershipSettings settings;
    private final Clock clock;

    public MembershipApplicationService(
            MembershipRepository repository,
            MembershipPaymentGateway paymentGateway,
            MembershipSettings settings,
            Clock clock) {
        this.repository = repository;
        this.paymentGateway = paymentGateway;
        this.settings = settings;
        this.clock = clock;
    }

    public MembershipDashboardResponse dashboard(long userId) {
        Instant now = clock.instant();
        MembershipEntitlement entitlement =
                repository.findEntitlement(userId);
        MembershipPlanType effectivePlan =
                entitlement.effectivePlanAt(now);
        boolean pro = effectivePlan != MembershipPlanType.FREE;
        MembershipQuota quota = entitlement.quotaAt(now);
        MembershipRepository.QuotaSnapshot snapshot =
                repository.loadQuotaSnapshot(userId, pro);
        MembershipRepository.RenewalState renewal =
                repository.findRenewal(userId);
        return new MembershipDashboardResponse(
                new MembershipDashboardResponse.Plan(
                        effectivePlan.name(),
                        displayName(effectivePlan),
                        pro ? entitlement.effectiveAt() : null,
                        effectivePlan == MembershipPlanType.MONTHLY_PRO
                                ? entitlement.expiresAt()
                                : null),
                toQuota(snapshot, quota),
                List.of(
                        product(MembershipProductCode.MONTHLY_PRO, true),
                        product(MembershipProductCode.LIFETIME_PRO, false)),
                new MembershipDashboardResponse.Renewal(
                        paymentGateway.renewalSupported(),
                        renewal.enabled(),
                        renewal.status(),
                        renewal.nextChargeAt()),
                new MembershipDashboardResponse.Contact(
                        settings.customerServiceEnabled(),
                        settings.enterpriseWechatQrUrl(),
                        settings.phone()));
    }

    @Transactional
    public MembershipOrderResponse createOrder(
            long userId,
            String productCode,
            boolean autoRenew) {
        MembershipProductCode product = parseProduct(productCode);
        if (autoRenew
                && product != MembershipProductCode.MONTHLY_PRO) {
            throw new ApplicationException(
                    "INVALID_MEMBERSHIP_ORDER",
                    "永久会员不支持自动续费");
        }
        String orderNo = "CM" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase(Locale.ROOT);
        repository.insertOrder(orderNo, userId, product, autoRenew);
        MembershipPaymentGateway.PaymentPreparation preparation =
                paymentGateway.preparePayment(userId, orderNo, product);
        repository.markOrderPaying(orderNo, preparation.prepayId());
        return new MembershipOrderResponse(
                orderNo,
                "PAYING",
                new MembershipOrderResponse.PaymentParams(
                        preparation.timeStamp(),
                        preparation.nonceStr(),
                        preparation.packageValue(),
                        preparation.signType(),
                        preparation.paySign()));
    }

    public MembershipOrderResponse order(
            long userId,
            String orderNo) {
        MembershipRepository.OrderState order = repository
                .findOrder(userId, orderNo)
                .orElseThrow(() -> new ApplicationException(
                        "MEMBERSHIP_ORDER_NOT_FOUND",
                        "会员订单不存在"));
        return new MembershipOrderResponse(
                order.orderNo(),
                order.status(),
                null);
    }

    @Transactional
    public void completePayment(
            MembershipPaymentGateway.PaymentNotification notification) {
        if (!repository.registerNotification(
                notification.notificationId(),
                "PAYMENT",
                notification.orderNo())) {
            return;
        }
        MembershipRepository.OrderState order = repository
                .lockOrder(notification.orderNo())
                .orElseThrow(() -> new ApplicationException(
                        "MEMBERSHIP_ORDER_NOT_FOUND",
                        "会员订单不存在"));
        if ("PAID".equals(order.status())) {
            return;
        }
        if (!"SUCCESS".equals(notification.tradeState())
                || order.amountFen() != notification.amountFen()) {
            throw new ApplicationException(
                    "WECHAT_PAYMENT_MISMATCH",
                    "微信支付通知与本地订单不一致");
        }
        if (!repository.markOrderPaid(
                order.orderNo(),
                notification.transactionId(),
                notification.paidAt())) {
            return;
        }
        MembershipEntitlement entitlement =
                repository.findEntitlement(order.userId());
        if (order.productCode()
                == MembershipProductCode.LIFETIME_PRO) {
            entitlement.grantLifetime(
                    notification.paidAt(),
                    order.orderNo());
            terminateRenewal(order.userId(), entitlement);
        } else {
            entitlement.grantMonthly(
                    notification.paidAt(),
                    1,
                    order.orderNo());
        }
        repository.saveEntitlement(entitlement);
        if (order.autoRenew()) {
            repository.markRenewalAttemptPaid(
                    order.orderNo(),
                    notification.transactionId(),
                    notification.paidAt());
            repository.scheduleNextRenewal(
                    order.userId(),
                    entitlement.expiresAt());
        }
    }

    @Transactional
    public void completeContract(
            MembershipPaymentGateway.ContractNotification notification) {
        if (!repository.registerNotification(
                notification.notificationId(),
                "CONTRACT",
                notification.contractCode())) {
            return;
        }
        if ("TERMINATED".equals(notification.status())) {
            repository.findRenewalByContractCode(
                            notification.contractCode())
                    .ifPresent(renewal -> repository.terminateRenewal(
                            renewal.userId(),
                            notification.occurredAt()));
            return;
        }
        if ("SIGNED".equals(notification.status())
                || "ACTIVE".equals(notification.status())) {
            MembershipRepository.RenewalState renewal = repository
                    .findRenewalByContractCode(
                            notification.contractCode())
                    .orElseThrow(() -> new ApplicationException(
                            "RENEWAL_AGREEMENT_NOT_FOUND",
                            "自动续费协议不存在"));
            MembershipEntitlement entitlement =
                    repository.findEntitlement(renewal.userId());
            Instant nextChargeAt =
                    entitlement.effectivePlanAt(clock.instant())
                            == MembershipPlanType.MONTHLY_PRO
                            && entitlement.expiresAt() != null
                            && entitlement.expiresAt()
                                    .isAfter(clock.instant())
                            ? entitlement.expiresAt()
                            : notification.occurredAt()
                                    .plus(30, ChronoUnit.DAYS);
            repository.activateRenewal(
                    notification.contractCode(),
                    notification.wechatContractId(),
                    nextChargeAt);
        }
    }

    @Transactional
    public RenewalAgreementResponse createRenewalAgreement(
            long userId) {
        if (!paymentGateway.renewalSupported()) {
            throw new ApplicationException(
                    "RENEWAL_NOT_SUPPORTED",
                    "当前商户尚未开通自动续费能力");
        }
        if (repository.findEntitlement(userId)
                .effectivePlanAt(clock.instant())
                == MembershipPlanType.LIFETIME_PRO) {
            throw new ApplicationException(
                    "RENEWAL_NOT_ALLOWED",
                    "永久会员无需自动续费");
        }
        String contractCode = "CT" + UUID.randomUUID()
                .toString()
                .replace("-", "");
        repository.savePendingRenewal(userId, contractCode);
        MembershipPaymentGateway.AgreementPreparation prepared =
                paymentGateway.prepareRenewalAgreement(
                        userId,
                        contractCode);
        return new RenewalAgreementResponse(
                contractCode,
                prepared.appId(),
                prepared.path(),
                prepared.extraData());
    }

    @Transactional
    public void terminateRenewal(long userId) {
        terminateRenewal(
                userId,
                repository.findEntitlement(userId));
    }

    public UpgradeRequestResponse submitUpgradeRequest(
            long userId,
            String contact,
            String remark) {
        MembershipRepository.UpgradeState request =
                repository.createOrGetPendingUpgrade(
                        userId,
                        contact.trim(),
                        remark == null ? "" : remark.trim());
        return new UpgradeRequestResponse(
                request.id(),
                request.status());
    }

    private void terminateRenewal(
            long userId,
            MembershipEntitlement entitlement) {
        MembershipRepository.RenewalState renewal =
                repository.findRenewal(userId);
        if (renewal.enabled()
                && renewal.wechatContractId() != null) {
            paymentGateway.terminateRenewalAgreement(
                    renewal.wechatContractId());
        }
        if (!"NONE".equals(renewal.status())) {
            repository.terminateRenewal(userId, clock.instant());
        }
    }

    private MembershipDashboardResponse.Quota toQuota(
            MembershipRepository.QuotaSnapshot snapshot,
            MembershipQuota quota) {
        Integer rootLimit = quota.rootSpaceLimit();
        List<MembershipDashboardResponse.CategoryQuotaUsage> spaces =
                snapshot.spaces().stream()
                        .filter(space -> rootLimit != null
                                && space.categoryCount() >= 2)
                        .map(space -> new MembershipDashboardResponse
                                .CategoryQuotaUsage(
                                space.spaceId(),
                                space.spaceName(),
                                space.categoryCount(),
                                quota.categoryLimitPerSpace(),
                                percent(
                                        space.categoryCount(),
                                        quota.categoryLimitPerSpace()),
                                reached(
                                        space.categoryCount(),
                                        quota.categoryLimitPerSpace())))
                        .toList();
        List<MembershipDashboardResponse.CapacityAlert> alerts =
                quota.itemLimitPerCavity() == null
                        ? List.of()
                        : snapshot.capacities().stream()
                                .filter(value -> value.itemCount() >= 8)
                                .map(value -> new MembershipDashboardResponse
                                        .CapacityAlert(
                                        value.spaceId(),
                                        value.spaceName(),
                                        value.categoryId(),
                                        value.categoryName(),
                                        value.itemCount(),
                                        quota.itemLimitPerCavity(),
                                        percent(
                                                value.itemCount(),
                                                quota.itemLimitPerCavity()),
                                        value.itemCount()
                                                >= quota.itemLimitPerCavity()
                                                ? "LOCKED"
                                                : "WARNING"))
                                .toList();
        return new MembershipDashboardResponse.Quota(
                new MembershipDashboardResponse.QuotaUsage(
                        snapshot.rootSpaceCount(),
                        rootLimit,
                        percent(snapshot.rootSpaceCount(), rootLimit),
                        reached(snapshot.rootSpaceCount(), rootLimit)),
                spaces,
                quota.itemLimitPerCavity(),
                alerts);
    }

    private MembershipDashboardResponse.Product product(
            MembershipProductCode code,
            boolean recommended) {
        return new MembershipDashboardResponse.Product(
                code.name(),
                code == MembershipProductCode.MONTHLY_PRO
                        ? "月度 PRO"
                        : "永久 PRO",
                code.priceFen(),
                code == MembershipProductCode.MONTHLY_PRO
                        ? "¥9.9 / 30天"
                        : "¥69 / 永久",
                code.durationDays(),
                recommended);
    }

    private int percent(int used, Integer limit) {
        if (limit == null || limit == 0) return 0;
        return Math.min(100, (int) Math.round(used * 100.0 / limit));
    }

    private boolean reached(int used, Integer limit) {
        return limit != null && used >= limit;
    }

    private String displayName(MembershipPlanType type) {
        return switch (type) {
            case FREE -> "免费基础版";
            case MONTHLY_PRO -> "月度 PRO";
            case LIFETIME_PRO -> "永久 PRO";
        };
    }

    private MembershipProductCode parseProduct(String value) {
        try {
            return MembershipProductCode.valueOf(value);
        } catch (RuntimeException error) {
            throw new ApplicationException(
                    "INVALID_MEMBERSHIP_PRODUCT",
                    "会员产品不存在",
                    error);
        }
    }
}
