package cn.cunmo.infrastructure.gateway.wechat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import cn.cunmo.infrastructure.config.WechatProperties;
import cn.cunmo.infrastructure.gateway.wechat.dto.WechatCodeSessionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 微信 code2Session 客户端测试。
 */
class WechatApiClientTest {

    /**
     * 验证请求使用微信官方绝对地址，并且不包含未展开的 URI 模板变量。
     */
    @Test
    void buildsEncodedOfficialCodeSessionUri() {
        WechatProperties properties = new WechatProperties(
                "wx-app-id",
                "app-secret",
                Duration.ofSeconds(3),
                Duration.ofSeconds(5));
        WechatApiClient client = new WechatApiClient(
                WebClient.builder().build(),
                properties,
                new ObjectMapper());

        URI uri = client.buildCodeSessionUri("temporary-code");

        assertEquals("https", uri.getScheme());
        assertEquals("api.weixin.qq.com", uri.getHost());
        assertEquals("/sns/jscode2session", uri.getPath());
        assertFalse(uri.toString().contains("${"));
        assertEquals(
                "appid=wx-app-id&secret=app-secret&js_code=temporary-code&grant_type=authorization_code",
                uri.getRawQuery());
    }

    /**
     * 验证微信以 text/plain 返回 JSON 时仍可正确解析会话信息。
     */
    @Test
    void exchangesJsonResponseReturnedAsPlainText() {
        WechatProperties properties = new WechatProperties(
                "wx-app-id",
                "app-secret",
                Duration.ofSeconds(3),
                Duration.ofSeconds(5));
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(
                                        HttpHeaders.CONTENT_TYPE,
                                        "text/plain;charset=UTF-8")
                                .body(
                                        """
                                        {
                                          "openid": "openid-123",
                                          "session_key": "session-key-456"
                                        }
                                        """)
                                .build()))
                .build();
        WechatApiClient client = new WechatApiClient(
                webClient,
                properties,
                new ObjectMapper());

        WechatCodeSessionResponse response = client.exchange("temporary-code");

        assertEquals("openid-123", response.openid());
        assertEquals("session-key-456", response.sessionKey());
    }
}
