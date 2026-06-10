package cn.cunmo.application.membership.service;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.application.membership.port.MembershipRepository;
import cn.cunmo.domain.membership.model.aggregate.MembershipEntitlement;
import cn.cunmo.domain.membership.model.enums.MembershipPlanType;
import java.time.Clock;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员会员升级审批服务。
 */
public class AdminMembershipApplicationService {
    private final MembershipRepository repository;
    private final Clock clock;

    public AdminMembershipApplicationService(
            MembershipRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<MembershipRepository.UpgradeState> requests(
            String status,
            int limit) {
        return repository.findUpgradeRequests(
                status == null || status.isBlank()
                        ? "PENDING"
                        : status,
                Math.min(Math.max(limit, 1), 100));
    }

    @Transactional
    public void approve(
            long requestId,
            long reviewerUserId,
            String grantTypeValue,
            Integer months,
            String note) {
        MembershipRepository.UpgradeState request =
                requirePending(requestId);
        MembershipPlanType grantType = parseGrantType(grantTypeValue);
        MembershipEntitlement entitlement =
                repository.findEntitlement(request.userId());
        if (grantType == MembershipPlanType.LIFETIME_PRO) {
            entitlement.grantLifetime(
                    clock.instant(),
                    "ADMIN:" + requestId);
        } else if (grantType == MembershipPlanType.MONTHLY_PRO
                && months != null
                && months > 0) {
            entitlement.grantMonthly(
                    clock.instant(),
                    months,
                    "ADMIN:" + requestId);
        } else {
            throw new ApplicationException(
                    "INVALID_MEMBERSHIP_GRANT",
                    "管理员必须授予月度或永久 PRO");
        }
        repository.saveEntitlement(entitlement);
        repository.approveUpgrade(
                requestId,
                reviewerUserId,
                grantType.name(),
                months,
                note,
                clock.instant());
    }

    @Transactional
    public void reject(
            long requestId,
            long reviewerUserId,
            String note) {
        requirePending(requestId);
        repository.rejectUpgrade(
                requestId,
                reviewerUserId,
                note,
                clock.instant());
    }

    private MembershipRepository.UpgradeState requirePending(
            long requestId) {
        MembershipRepository.UpgradeState request = repository
                .lockUpgradeRequest(requestId)
                .orElseThrow(() -> new ApplicationException(
                        "UPGRADE_REQUEST_NOT_FOUND",
                        "升级申请不存在"));
        if (!"PENDING".equals(request.status())) {
            throw new ApplicationException(
                    "UPGRADE_REQUEST_PROCESSED",
                    "升级申请已处理");
        }
        return request;
    }

    private MembershipPlanType parseGrantType(String value) {
        try {
            return MembershipPlanType.valueOf(value);
        } catch (RuntimeException error) {
            throw new ApplicationException(
                    "INVALID_MEMBERSHIP_GRANT",
                    "会员授予类型无效",
                    error);
        }
    }
}
