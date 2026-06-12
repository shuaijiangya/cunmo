package cn.cunmo.infrastructure.payment.wechat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 微信委托代扣 V2 协议编解码测试。
 */
class WechatPayV2CodecTest {

    /**
     * 验证签名按参数名排序并使用 HMAC-SHA256。
     */
    @Test
    void signsSortedParametersWithHmacSha256() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("mch_id", "10000100");
        values.put("appid", "wx123");
        values.put("nonce_str", "nonce");

        String signature = WechatPayV2Codec.sign(
                values,
                "01234567890123456789012345678901");

        assertEquals(
                "4BC702AE70F60EB8EFABA0D57B0A5A47"
                        + "BCA45DF2B155BBD6DA30DECC34EBECB6",
                signature);
    }

    /**
     * 验证 XML 特殊字符可安全往返。
     */
    @Test
    void roundTripsXmlValues() {
        String xml = WechatPayV2Codec.toXml(Map.of(
                "return_code", "SUCCESS",
                "return_msg", "A&B<OK>"));

        Map<String, String> parsed = WechatPayV2Codec.parseXml(xml);

        assertEquals("SUCCESS", parsed.get("return_code"));
        assertEquals("A&B<OK>", parsed.get("return_msg"));
        assertTrue(xml.contains("<![CDATA["));
    }
}
