package cn.cunmo.infrastructure.payment.wechat;

import cn.cunmo.application.exception.ApplicationException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * 微信支付 V2 XML 与 HMAC-SHA256 编解码器。
 */
final class WechatPayV2Codec {

    private WechatPayV2Codec() {
    }

    /**
     * 按微信支付 V2 规则生成大写 HMAC-SHA256 签名。
     */
    static String sign(Map<String, String> values, String apiV2Key) {
        try {
            StringBuilder canonical = new StringBuilder();
            new TreeMap<>(values).forEach((key, value) -> {
                if (!"sign".equals(key)
                        && value != null
                        && !value.isBlank()) {
                    if (!canonical.isEmpty()) canonical.append('&');
                    canonical.append(key).append('=').append(value);
                }
            });
            canonical.append("&key=").append(apiV2Key);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    apiV2Key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return java.util.HexFormat.of()
                    .withUpperCase()
                    .formatHex(mac.doFinal(
                            canonical.toString()
                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new ApplicationException(
                    "WECHAT_PAY_FAILED",
                    "微信委托代扣签名失败",
                    error);
        }
    }

    /**
     * 校验微信支付 V2 回调签名。
     */
    static void verify(Map<String, String> values, String apiV2Key) {
        String actual = values.get("sign");
        String expected = sign(values, apiV2Key);
        if (actual == null
                || !java.security.MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.US_ASCII),
                        actual.toUpperCase()
                                .getBytes(StandardCharsets.US_ASCII))) {
            throw new ApplicationException(
                    "INVALID_WECHAT_PAY_SIGNATURE",
                    "微信委托代扣通知签名无效");
        }
    }

    /**
     * 将参数编码为微信支付 V2 XML。
     */
    static String toXml(Map<String, String> values) {
        StringBuilder xml = new StringBuilder("<xml>");
        values.forEach((key, value) -> xml
                .append('<').append(key).append('>')
                .append("<![CDATA[")
                .append(value == null
                        ? ""
                        : value.replace("]]>", "]]]]><![CDATA[>"))
                .append("]]>")
                .append("</").append(key).append('>'));
        return xml.append("</xml>").toString();
    }

    /**
     * 安全解析微信支付 V2 XML，禁用外部实体和 DTD。
     */
    static Map<String, String> parseXml(String xml) {
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true);
            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false);
            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false);
            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    "");
            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    "");
            Element root = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)))
                    .getDocumentElement();
            Map<String, String> values = new LinkedHashMap<>();
            for (Node child = root.getFirstChild();
                    child != null;
                    child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    values.put(
                            child.getNodeName(),
                            child.getTextContent());
                }
            }
            return values;
        } catch (Exception error) {
            throw new ApplicationException(
                    "INVALID_WECHAT_PAY_PAYLOAD",
                    "微信委托代扣 XML 无法解析",
                    error);
        }
    }

    /**
     * 生成用于小程序跳转 extraData 的安全副本。
     */
    static Map<String, String> copyWithSignature(
            Map<String, String> values,
            String apiV2Key) {
        Map<String, String> signed = new LinkedHashMap<>(values);
        signed.put("sign", sign(signed, apiV2Key));
        return Map.copyOf(signed);
    }
}
