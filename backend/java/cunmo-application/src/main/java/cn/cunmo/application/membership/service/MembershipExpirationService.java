package cn.cunmo.application.membership.service;

import cn.cunmo.application.membership.port.MembershipRepository;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 将已到期月度会员的持久化状态收敛为免费会员。
 */
public class MembershipExpirationService {
    private static final int BATCH_SIZE = 200;

    private final MembershipRepository repository;
    private final Clock clock;

    public MembershipExpirationService(
            MembershipRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(
            fixedDelayString =
                    "${membership.expiration-scan-delay:PT10M}")
    public void expireMemberships() {
        repository.expireMonthlyEntitlements(
                clock.instant(),
                BATCH_SIZE);
    }
}
