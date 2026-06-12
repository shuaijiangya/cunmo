package cn.cunmo.infrastructure.config;

import cn.cunmo.application.membership.port.MembershipSettings;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 会员展示及微信支付配置。
 */
@ConfigurationProperties(prefix = "membership")
public record MembershipProperties(
        Contact contact,
        WechatPay wechatPay) implements MembershipSettings {

    public MembershipProperties {
        contact = contact == null
                ? new Contact(false, null, null)
                : contact;
        wechatPay = wechatPay == null
                ? WechatPay.disabled()
                : wechatPay;
    }

    @Override
    public boolean customerServiceEnabled() {
        return contact.customerServiceEnabled();
    }

    @Override
    public String enterpriseWechatQrUrl() {
        return blankToNull(contact.enterpriseWechatQrUrl());
    }

    @Override
    public String phone() {
        return blankToNull(contact.phone());
    }

    @Override
    public int renewalMaxAttempts() {
        return Math.max(1, wechatPay.renewal().maxAttempts());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record Contact(
            boolean customerServiceEnabled,
            String enterpriseWechatQrUrl,
            String phone) {
    }

    public record WechatPay(
            boolean enabled,
            String appId,
            String mchId,
            String merchantSerialNo,
            String merchantPrivateKeyPath,
            String platformCertificatePath,
            String apiV3Key,
            String paymentNotifyUrl,
            Duration responseTimeout,
            Renewal renewal) {

        public WechatPay {
            responseTimeout = responseTimeout == null
                    ? Duration.ofSeconds(8)
                    : responseTimeout;
            renewal = renewal == null
                    ? Renewal.disabled()
                    : renewal;
        }

        public static WechatPay disabled() {
            return new WechatPay(
                    false, null, null, null, null, null, null, null,
                    Duration.ofSeconds(8), Renewal.disabled());
        }

        public boolean paymentConfigured() {
            return enabled
                    && present(appId)
                    && present(mchId)
                    && present(merchantSerialNo)
                    && present(merchantPrivateKeyPath)
                    && present(platformCertificatePath)
                    && present(apiV3Key)
                    && present(paymentNotifyUrl);
        }

        public boolean renewalConfigured() {
            return paymentConfigured()
                    && renewal.enabled()
                    && present(renewal.apiV2Key())
                    && present(renewal.signingMiniProgramAppId())
                    && present(renewal.signingPath())
                    && present(renewal.planId())
                    && present(renewal.contractNotifyUrl())
                    && present(renewal.renewalNotifyUrl())
                    && present(renewal.clientIp())
                    && present(renewal.chargeUrl())
                    && present(renewal.terminateUrl())
                    && present(renewal.contractDisplayAccount());
        }

        private static boolean present(String value) {
            return value != null && !value.isBlank();
        }
    }

    public record Renewal(
            boolean enabled,
            String apiV2Key,
            String signingMiniProgramAppId,
            String signingPath,
            String planId,
            String contractNotifyUrl,
            String renewalNotifyUrl,
            String clientIp,
            String chargeUrl,
            String terminateUrl,
            String contractDisplayAccount,
            int maxAttempts) {
        public static Renewal disabled() {
            return new Renewal(
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    3);
        }
    }
}
