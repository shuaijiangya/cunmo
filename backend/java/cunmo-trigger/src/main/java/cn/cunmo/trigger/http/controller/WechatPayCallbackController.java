package cn.cunmo.trigger.http.controller;

import cn.cunmo.application.membership.port.MembershipPaymentGateway;
import cn.cunmo.application.membership.service.MembershipApplicationService;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wechat-pay")
public class WechatPayCallbackController {
    private final MembershipApplicationService service;
    private final MembershipPaymentGateway paymentGateway;

    public WechatPayCallbackController(
            MembershipApplicationService service,
            MembershipPaymentGateway paymentGateway) {
        this.service = service;
        this.paymentGateway = paymentGateway;
    }

    @PostMapping("/payment-notify")
    public Map<String, String> paymentNotify(
            @RequestHeader HttpHeaders headers,
            @RequestBody String body) {
        service.completePayment(
                paymentGateway.parsePaymentNotification(
                        headers.toSingleValueMap(),
                        body));
        return Map.of("code", "SUCCESS", "message", "成功");
    }

    @PostMapping(
            value = "/contract-notify",
            produces = MediaType.APPLICATION_XML_VALUE)
    public String contractNotify(
            @RequestBody String body) {
        service.completeContract(
                paymentGateway.parseContractNotification(body));
        return successXml();
    }

    @PostMapping(
            value = "/renewal-notify",
            produces = MediaType.APPLICATION_XML_VALUE)
    public String renewalNotify(
            @RequestBody String body) {
        service.completePayment(
                paymentGateway.parseRenewalNotification(body));
        return successXml();
    }

    private String successXml() {
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                + "<return_msg><![CDATA[OK]]></return_msg></xml>";
    }
}
