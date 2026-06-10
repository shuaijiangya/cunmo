package cn.cunmo.application.membership.port;

import cn.cunmo.domain.membership.model.enums.MembershipProductCode;
import java.time.Instant;
import java.util.Map;

/**
 * 微信支付与委托代扣适配端口。
 */
public interface MembershipPaymentGateway {
    PaymentPreparation preparePayment(
            long userId,
            String orderNo,
            MembershipProductCode product);

    AgreementPreparation prepareRenewalAgreement(
            long userId,
            String contractCode);

    void terminateRenewalAgreement(String wechatContractId);

    void chargeRenewal(
            long userId,
            String wechatContractId,
            String attemptNo,
            int amountFen);

    PaymentNotification parsePaymentNotification(
            Map<String, String> headers,
            String body);

    ContractNotification parseContractNotification(
            Map<String, String> headers,
            String body);

    boolean renewalSupported();

    record PaymentPreparation(
            String prepayId,
            String timeStamp,
            String nonceStr,
            String packageValue,
            String signType,
            String paySign) {
    }

    record AgreementPreparation(
            String businessType,
            Map<String, String> invokeParams) {
    }

    record PaymentNotification(
            String notificationId,
            String orderNo,
            String transactionId,
            int amountFen,
            String tradeState,
            Instant paidAt) {
    }

    record ContractNotification(
            String notificationId,
            String contractCode,
            String wechatContractId,
            String status,
            Instant occurredAt) {
    }
}
