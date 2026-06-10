package cn.cunmo.application.membership.service;

import cn.cunmo.application.membership.port.MembershipPaymentGateway;
import cn.cunmo.application.membership.port.MembershipRepository;
import cn.cunmo.application.membership.port.MembershipSettings;
import cn.cunmo.domain.membership.model.enums.MembershipPlanType;
import cn.cunmo.domain.membership.model.enums.MembershipProductCode;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 扫描到期协议并发起微信委托代扣。
 */
public class MembershipRenewalService {
    private final MembershipRepository repository;
    private final MembershipPaymentGateway paymentGateway;
    private final MembershipSettings settings;
    private final Clock clock;

    public MembershipRenewalService(
            MembershipRepository repository,
            MembershipPaymentGateway paymentGateway,
            MembershipSettings settings,
            Clock clock) {
        this.repository = repository;
        this.paymentGateway = paymentGateway;
        this.settings = settings;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${membership.renewal-scan-delay:PT5M}")
    public void processDueRenewals() {
        if (!paymentGateway.renewalSupported()) return;
        for (MembershipRepository.RenewalState renewal
                : repository.findDueRenewals(clock.instant(), 50)) {
            process(renewal);
        }
    }

    private void process(MembershipRepository.RenewalState renewal) {
        if (repository.findEntitlement(renewal.userId())
                .effectivePlanAt(clock.instant())
                == MembershipPlanType.LIFETIME_PRO) {
            paymentGateway.terminateRenewalAgreement(
                    renewal.wechatContractId());
            repository.terminateRenewal(
                    renewal.userId(),
                    clock.instant());
            return;
        }
        String attemptNo = "AR" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase(Locale.ROOT);
        repository.insertOrder(
                attemptNo,
                renewal.userId(),
                MembershipProductCode.MONTHLY_PRO,
                true);
        repository.markOrderPaying(attemptNo, "AUTO_RENEWAL");
        repository.insertRenewalAttempt(
                renewal.agreementId(),
                attemptNo,
                MembershipProductCode.MONTHLY_PRO.priceFen(),
                clock.instant());
        try {
            paymentGateway.chargeRenewal(
                    renewal.userId(),
                    renewal.wechatContractId(),
                    attemptNo,
                    MembershipProductCode.MONTHLY_PRO.priceFen());
        } catch (RuntimeException error) {
            repository.markRenewalAttemptFailed(
                    attemptNo,
                    error.getMessage(),
                    clock.instant().plus(1, ChronoUnit.DAYS),
                    settings.renewalMaxAttempts());
        }
    }
}
