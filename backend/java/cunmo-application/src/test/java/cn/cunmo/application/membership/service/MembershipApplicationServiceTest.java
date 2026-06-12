package cn.cunmo.application.membership.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cunmo.application.membership.port.MembershipPaymentGateway;
import cn.cunmo.application.membership.port.MembershipRepository;
import cn.cunmo.application.membership.port.MembershipSettings;
import cn.cunmo.domain.membership.model.aggregate.MembershipEntitlement;
import cn.cunmo.domain.membership.model.enums.MembershipPlanType;
import cn.cunmo.domain.membership.model.enums.MembershipProductCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipApplicationServiceTest {
    private static final Instant NOW =
            Instant.parse("2026-06-12T08:00:00Z");

    @Mock
    private MembershipRepository repository;
    @Mock
    private MembershipPaymentGateway paymentGateway;
    @Mock
    private MembershipSettings settings;

    private MembershipApplicationService service;

    @BeforeEach
    void setUp() {
        service = new MembershipApplicationService(
                repository,
                paymentGateway,
                settings,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void completesMonthlyPaymentOnceAndGrantsThirtyDays() {
        MembershipRepository.OrderState order =
                new MembershipRepository.OrderState(
                        "CM100",
                        42,
                        MembershipProductCode.MONTHLY_PRO,
                        990,
                        false,
                        "PAYING",
                        "prepay");
        when(repository.registerNotification(
                "notify-1", "PAYMENT", "CM100"))
                .thenReturn(true);
        when(repository.lockOrder("CM100"))
                .thenReturn(Optional.of(order));
        when(repository.markOrderPaid(
                "CM100", "transaction-1", NOW))
                .thenReturn(true);
        when(repository.findEntitlement(42))
                .thenReturn(MembershipEntitlement.free(42));

        service.completePayment(new MembershipPaymentGateway
                .PaymentNotification(
                        "notify-1",
                        "CM100",
                        "transaction-1",
                        990,
                        "SUCCESS",
                        NOW));

        ArgumentCaptor<MembershipEntitlement> entitlement =
                ArgumentCaptor.forClass(MembershipEntitlement.class);
        verify(repository).saveEntitlement(entitlement.capture());
        assertEquals(
                MembershipPlanType.MONTHLY_PRO,
                entitlement.getValue().planType());
        assertEquals(
                Instant.parse("2026-07-12T08:00:00Z"),
                entitlement.getValue().expiresAt());
    }

    @Test
    void ignoresDuplicatePaymentNotification() {
        when(repository.registerNotification(
                "notify-1", "PAYMENT", "CM100"))
                .thenReturn(false);

        service.completePayment(new MembershipPaymentGateway
                .PaymentNotification(
                        "notify-1",
                        "CM100",
                        "transaction-1",
                        990,
                        "SUCCESS",
                        NOW));

        verify(repository, never()).lockOrder(any());
        verify(repository, never()).saveEntitlement(any());
    }

    @Test
    void terminatesRenewalWhenWechatReportsContractDeletion() {
        when(repository.registerNotification(
                "contract-notify", "CONTRACT", "CT100"))
                .thenReturn(true);
        when(repository.findRenewalByContractCode("CT100"))
                .thenReturn(Optional.of(
                        new MembershipRepository.RenewalState(
                                5,
                                42,
                                "ACTIVE",
                                true,
                                NOW,
                                "CT100",
                                "wechat-contract")));

        service.completeContract(new MembershipPaymentGateway
                .ContractNotification(
                        "contract-notify",
                        "CT100",
                        "wechat-contract",
                        "TERMINATED",
                        NOW));

        verify(repository).terminateRenewal(42, NOW);
    }
}
