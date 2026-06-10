package cn.cunmo.api.membership.model.response;

public record MembershipOrderResponse(
        String orderNo,
        String status,
        PaymentParams paymentParams) {

    public record PaymentParams(
            String timeStamp,
            String nonceStr,
            String packageValue,
            String signType,
            String paySign) {
    }
}
