package cn.cunmo.application.membership.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cunmo.application.membership.port.MembershipRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 月度会员到期任务测试。
 */
@ExtendWith(MockitoExtension.class)
class MembershipExpirationServiceTest {
    private static final Instant NOW =
            Instant.parse("2026-06-12T08:00:00Z");

    @Mock
    private MembershipRepository repository;

    /**
     * 验证任务将到期月卡持久化收敛为免费状态。
     */
    @Test
    void expiresMonthlyMembershipsAtCurrentTime() {
        when(repository.expireMonthlyEntitlements(NOW, 200))
                .thenReturn(3);
        MembershipExpirationService service =
                new MembershipExpirationService(
                        repository,
                        Clock.fixed(NOW, ZoneOffset.UTC));

        service.expireMemberships();

        verify(repository).expireMonthlyEntitlements(NOW, 200);
    }
}
