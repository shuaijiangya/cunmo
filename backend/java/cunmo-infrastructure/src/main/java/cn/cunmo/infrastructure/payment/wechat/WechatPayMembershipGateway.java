package cn.cunmo.infrastructure.payment.wechat;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.application.membership.port.MembershipPaymentGateway;
import cn.cunmo.application.membership.port.MembershipRepository;
import cn.cunmo.domain.membership.model.enums.MembershipProductCode;
import cn.cunmo.infrastructure.config.MembershipProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 微信支付 API v3 直连普通商户适配器。
 */
@Component
public class WechatPayMembershipGateway
        implements MembershipPaymentGateway {
    private static final String JSAPI_PATH =
            "/v3/pay/transactions/jsapi";
    private static final DateTimeFormatter V2_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId CHINA_ZONE =
            ZoneId.of("Asia/Shanghai");

    private final MembershipProperties properties;
    private final MembershipRepository repository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public WechatPayMembershipGateway(
            MembershipProperties properties,
            MembershipRepository repository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.mch.weixin.qq.com")
                .build();
    }

    @Override
    public PaymentPreparation preparePayment(
            long userId,
            String orderNo,
            MembershipProductCode product) {
        requirePaymentConfigured();
        MembershipProperties.WechatPay pay = properties.wechatPay();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appid", pay.appId());
        body.put("mchid", pay.mchId());
        body.put(
                "description",
                product == MembershipProductCode.MONTHLY_PRO
                        ? "存量魔方月度PRO"
                        : "存量魔方永久PRO");
        body.put("out_trade_no", orderNo);
        body.put("notify_url", pay.paymentNotifyUrl());
        body.put("amount", Map.of(
                "total", product.priceFen(),
                "currency", "CNY"));
        body.put("payer", Map.of(
                "openid", repository.requireWechatOpenId(userId)));
        String requestBody = json(body);
        String authorization = authorization(
                "POST",
                JSAPI_PATH,
                requestBody);
        String responseBody = webClient.post()
                .uri(JSAPI_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authorization)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(pay.responseTimeout())
                .block();
        String prepayId = text(parse(responseBody), "prepay_id");
        String timeStamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = nonce();
        String packageValue = "prepay_id=" + prepayId;
        String paySign = sign(
                pay.appId() + "\n"
                        + timeStamp + "\n"
                        + nonce + "\n"
                        + packageValue + "\n");
        return new PaymentPreparation(
                prepayId,
                timeStamp,
                nonce,
                packageValue,
                "RSA",
                paySign);
    }

    @Override
    public AgreementPreparation prepareRenewalAgreement(
            long userId,
            String contractCode) {
        if (!renewalSupported()) {
            throw new ApplicationException(
                    "RENEWAL_NOT_SUPPORTED",
                    "当前商户尚未开通自动续费能力");
        }
        MembershipProperties.WechatPay pay = properties.wechatPay();
        MembershipProperties.Renewal renewal = pay.renewal();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appid", pay.appId());
        params.put("mch_id", pay.mchId());
        params.put("contract_appid", pay.appId());
        params.put("contract_mchid", pay.mchId());
        params.put("plan_id", renewal.planId());
        params.put("contract_code", contractCode);
        params.put("request_serial", contractCode);
        params.put("contract_display_account",
                renewal.contractDisplayAccount());
        params.put("notify_url", renewal.contractNotifyUrl());
        params.put("openid", repository.requireWechatOpenId(userId));
        params.put("timestamp", String.valueOf(
                Instant.now().getEpochSecond()));
        params.put("return_web", "1");
        params.put("version", "1.0");
        params.put("sign_type", "HMAC-SHA256");
        return new AgreementPreparation(
                renewal.signingMiniProgramAppId(),
                renewal.signingPath(),
                WechatPayV2Codec.copyWithSignature(
                        params,
                        renewal.apiV2Key()));
    }

    @Override
    public void terminateRenewalAgreement(String wechatContractId) {
        if (!renewalSupported()) return;
        MembershipProperties.WechatPay pay = properties.wechatPay();
        Map<String, String> params = baseV2Params(pay);
        params.put("contract_id", wechatContractId);
        params.put("plan_id", pay.renewal().planId());
        postV2(pay.renewal().terminateUrl(), params);
    }

    @Override
    public void chargeRenewal(
            long userId,
            String wechatContractId,
            String attemptNo,
            int amountFen) {
        if (!renewalSupported()) {
            throw new ApplicationException(
                    "RENEWAL_NOT_SUPPORTED",
                    "当前商户尚未开通自动续费能力");
        }
        MembershipProperties.WechatPay pay = properties.wechatPay();
        Map<String, String> params = baseV2Params(pay);
        params.put("contract_id", wechatContractId);
        params.put("body", "存量魔方月度PRO自动续费");
        params.put("out_trade_no", attemptNo);
        params.put("total_fee", String.valueOf(amountFen));
        params.put("spbill_create_ip", pay.renewal().clientIp());
        params.put("notify_url", pay.renewal().renewalNotifyUrl());
        params.put("trade_type", "PAP");
        postV2(pay.renewal().chargeUrl(), params);
    }

    @Override
    public PaymentNotification parsePaymentNotification(
            Map<String, String> headers,
            String body) {
        JsonNode root = verifiedNotification(headers, body);
        JsonNode resource = decryptResource(root.path("resource"));
        verifyV3Identity(resource);
        JsonNode amount = resource.path("amount");
        return new PaymentNotification(
                text(root, "id"),
                text(resource, "out_trade_no"),
                text(resource, "transaction_id"),
                amount.path("payer_total").asInt(
                        amount.path("total").asInt()),
                text(resource, "trade_state"),
                Instant.parse(text(resource, "success_time")));
    }

    @Override
    public ContractNotification parseContractNotification(
            String body) {
        Map<String, String> values = verifiedV2Notification(body);
        String contractCode = required(values, "contract_code");
        String status = contractStatus(values);
        return new ContractNotification(
                notificationId("V2C", body),
                contractCode,
                required(values, "contract_id"),
                status,
                parseV2Time(values.get("operate_time")));
    }

    @Override
    public PaymentNotification parseRenewalNotification(String body) {
        Map<String, String> values = verifiedV2Notification(body);
        String result = values.getOrDefault(
                "result_code",
                values.getOrDefault("return_code", "FAIL"));
        return new PaymentNotification(
                notificationId("V2R", body),
                required(values, "out_trade_no"),
                required(values, "transaction_id"),
                parseInt(values, "total_fee"),
                "SUCCESS".equals(result) ? "SUCCESS" : result,
                parseV2Time(values.get("time_end")));
    }

    @Override
    public boolean renewalSupported() {
        return properties.wechatPay().renewalConfigured();
    }

    private JsonNode verifiedNotification(
            Map<String, String> headers,
            String body) {
        requirePaymentConfigured();
        String timestamp = header(headers, "wechatpay-timestamp");
        String nonce = header(headers, "wechatpay-nonce");
        String signature = header(headers, "wechatpay-signature");
        String message = timestamp + "\n"
                + nonce + "\n"
                + body + "\n";
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(platformCertificate().getPublicKey());
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(
                    Base64.getDecoder().decode(signature))) {
                throw new ApplicationException(
                        "INVALID_WECHAT_PAY_SIGNATURE",
                        "微信支付通知签名无效");
            }
            return parse(body);
        } catch (ApplicationException error) {
            throw error;
        } catch (Exception error) {
            throw paymentError("微信支付通知验签失败", error);
        }
    }

    private JsonNode decryptResource(JsonNode resource) {
        try {
            byte[] key = properties.wechatPay()
                    .apiV3Key()
                    .getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance(
                    "AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(
                            128,
                            resource.path("nonce")
                                    .asText()
                                    .getBytes(StandardCharsets.UTF_8)));
            String associatedData =
                    resource.path("associated_data").asText("");
            cipher.updateAAD(
                    associatedData.getBytes(StandardCharsets.UTF_8));
            byte[] plain = cipher.doFinal(
                    Base64.getDecoder().decode(
                            resource.path("ciphertext").asText()));
            return parse(new String(plain, StandardCharsets.UTF_8));
        } catch (Exception error) {
            throw paymentError("微信支付通知解密失败", error);
        }
    }

    private String authorization(
            String method,
            String path,
            String body) {
        MembershipProperties.WechatPay pay = properties.wechatPay();
        String timestamp = String.valueOf(
                Instant.now().getEpochSecond());
        String nonce = nonce();
        String signature = sign(
                method + "\n"
                        + path + "\n"
                        + timestamp + "\n"
                        + nonce + "\n"
                        + body + "\n");
        return "WECHATPAY2-SHA256-RSA2048 "
                + "mchid=\"" + pay.mchId() + "\","
                + "nonce_str=\"" + nonce + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + pay.merchantSerialNo() + "\","
                + "signature=\"" + signature + "\"";
    }

    private String sign(String message) {
        try {
            Signature signature = Signature.getInstance(
                    "SHA256withRSA");
            signature.initSign(merchantPrivateKey());
            signature.update(
                    message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    signature.sign());
        } catch (Exception error) {
            throw paymentError("微信支付请求签名失败", error);
        }
    }

    private PrivateKey merchantPrivateKey() throws Exception {
        String pem = Files.readString(Path.of(
                properties.wechatPay().merchantPrivateKeyPath()));
        String encoded = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(
                        Base64.getDecoder().decode(encoded)));
    }

    private X509Certificate platformCertificate() throws Exception {
        try (var input = Files.newInputStream(Path.of(
                properties.wechatPay().platformCertificatePath()))) {
            return (X509Certificate) CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(input);
        }
    }

    private Map<String, String> baseV2Params(
            MembershipProperties.WechatPay pay) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appid", pay.appId());
        params.put("mch_id", pay.mchId());
        params.put("contract_appid", pay.appId());
        params.put("contract_mchid", pay.mchId());
        params.put("nonce_str", nonce());
        params.put("sign_type", "HMAC-SHA256");
        return params;
    }

    private void postV2(
            String url,
            Map<String, String> params) {
        MembershipProperties.WechatPay pay = properties.wechatPay();
        Map<String, String> signed =
                WechatPayV2Codec.copyWithSignature(
                        params,
                        pay.renewal().apiV2Key());
        String responseBody = WebClient.create().post()
                .uri(url)
                .contentType(MediaType.APPLICATION_XML)
                .bodyValue(WechatPayV2Codec.toXml(signed))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(pay.responseTimeout())
                .block();
        Map<String, String> response =
                WechatPayV2Codec.parseXml(responseBody);
        if (response.containsKey("sign")) {
            WechatPayV2Codec.verify(
                    response,
                    pay.renewal().apiV2Key());
        }
        if (!"SUCCESS".equals(response.get("return_code"))
                || (response.containsKey("result_code")
                && !"SUCCESS".equals(response.get("result_code")))) {
            throw new ApplicationException(
                    "WECHAT_PAY_FAILED",
                    response.getOrDefault(
                            "err_code_des",
                            response.getOrDefault(
                                    "return_msg",
                                    "微信委托代扣请求失败")));
        }
    }

    private Map<String, String> verifiedV2Notification(String body) {
        if (!renewalSupported()) {
            throw new ApplicationException(
                    "RENEWAL_NOT_SUPPORTED",
                    "当前商户尚未开通自动续费能力");
        }
        Map<String, String> values =
                WechatPayV2Codec.parseXml(body);
        WechatPayV2Codec.verify(
                values,
                properties.wechatPay().renewal().apiV2Key());
        verifyV2Identity(values);
        return values;
    }

    private void verifyV3Identity(JsonNode resource) {
        requireIdentity(
                resource.path("appid").asText(),
                properties.wechatPay().appId(),
                "appid");
        requireIdentity(
                resource.path("mchid").asText(),
                properties.wechatPay().mchId(),
                "mchid");
    }

    private void verifyV2Identity(Map<String, String> values) {
        String appId = values.getOrDefault(
                "appid",
                values.get("contract_appid"));
        String mchId = values.getOrDefault(
                "mch_id",
                values.get("contract_mchid"));
        requireIdentity(
                appId,
                properties.wechatPay().appId(),
                "appid");
        requireIdentity(
                mchId,
                properties.wechatPay().mchId(),
                "mch_id");
    }

    private void requireIdentity(
            String actual,
            String expected,
            String field) {
        if (!expected.equals(actual)) {
            throw new ApplicationException(
                    "WECHAT_PAYMENT_MISMATCH",
                    "微信支付通知商户身份不匹配: " + field);
        }
    }

    private String contractStatus(Map<String, String> values) {
        String status = values.getOrDefault(
                "contract_state",
                values.getOrDefault("change_type", ""));
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "0", "ADD", "SIGNED", "ACTIVE" -> "SIGNED";
            case "1", "DELETE", "TERMINATED" -> "TERMINATED";
            default -> status.toUpperCase(Locale.ROOT);
        };
    }

    private Instant parseV2Time(String value) {
        if (value == null || value.isBlank()) return Instant.now();
        try {
            return LocalDateTime.parse(value, V2_TIME)
                    .atZone(CHINA_ZONE)
                    .toInstant();
        } catch (RuntimeException ignored) {
            return Instant.parse(value);
        }
    }

    private int parseInt(
            Map<String, String> values,
            String field) {
        try {
            return Integer.parseInt(required(values, field));
        } catch (NumberFormatException error) {
            throw paymentError(
                    "微信支付数据字段格式无效: " + field,
                    error);
        }
    }

    private String required(
            Map<String, String> values,
            String field) {
        String value = values.get(field);
        if (value == null || value.isBlank()) {
            throw new ApplicationException(
                    "INVALID_WECHAT_PAY_PAYLOAD",
                    "微信支付数据缺少字段: " + field);
        }
        return value;
    }

    private String notificationId(
            String prefix,
            String body) {
        try {
            byte[] digest = java.security.MessageDigest
                    .getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8));
            return prefix + java.util.HexFormat.of()
                    .formatHex(digest)
                    .substring(0, 48);
        } catch (Exception error) {
            throw paymentError("微信支付通知标识生成失败", error);
        }
    }

    private String header(
            Map<String, String> headers,
            String name) {
        Map<String, String> normalized = new HashMap<>();
        headers.forEach((key, value) -> normalized.put(
                key.toLowerCase(Locale.ROOT),
                value));
        String value = normalized.get(name);
        if (value == null || value.isBlank()) {
            throw new ApplicationException(
                    "INVALID_WECHAT_PAY_SIGNATURE",
                    "微信支付通知缺少签名头");
        }
        return value;
    }

    private JsonNode parse(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception error) {
            throw paymentError("微信支付响应格式无效", error);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception error) {
            throw paymentError("微信支付请求序列化失败", error);
        }
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value.isBlank()) {
            throw new ApplicationException(
                    "INVALID_WECHAT_PAY_PAYLOAD",
                    "微信支付数据缺少字段: " + field);
        }
        return value;
    }

    private void requirePaymentConfigured() {
        if (!properties.wechatPay().paymentConfigured()) {
            throw new ApplicationException(
                    "WECHAT_PAY_NOT_CONFIGURED",
                    "微信支付能力尚未配置");
        }
    }

    private ApplicationException paymentError(
            String message,
            Exception error) {
        return new ApplicationException(
                "WECHAT_PAY_FAILED",
                message,
                error);
    }

    private String nonce() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "");
    }
}
