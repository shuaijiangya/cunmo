package cn.cunmo.application.membership.service;

import cn.cunmo.application.membership.port.MembershipRepository;
import cn.cunmo.domain.membership.model.valueobject.MembershipQuota;
import java.time.Clock;

/**
 * 为库存域提供当前用户有效配额。
 */
public class MembershipQuotaPolicy {
    private final MembershipRepository repository;
    private final Clock clock;

    public MembershipQuotaPolicy(
            MembershipRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public MembershipQuota quotaFor(long userId) {
        return repository.findEntitlement(userId)
                .quotaAt(clock.instant());
    }
}
