package cn.cunmo.api.membership.model.response;

import java.util.Map;

public record RenewalAgreementResponse(
        String contractCode,
        String businessType,
        Map<String, String> invokeParams) {
}
