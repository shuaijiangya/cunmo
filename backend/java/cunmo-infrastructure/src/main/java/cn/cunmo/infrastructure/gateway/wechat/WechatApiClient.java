package cn.cunmo.infrastructure.gateway.wechat;

import cn.cunmo.infrastructure.config.WechatProperties;
import cn.cunmo.infrastructure.gateway.wechat.dto.WechatCodeSessionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Exceptions;

/**
 * 微信 code2Session HTTP 客户端。
 */
@Component
public class WechatApiClient {
    private static final String CODE_SESSION_ENDPOINT =
            "https://api.weixin.qq.com/sns/jscode2session";

    private static final Logger log =
            LoggerFactory.getLogger(WechatApiClient.class);

    private final WebClient webClient;
    private final WechatProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 创建微信 API 客户端。
     */
    public WechatApiClient(
            WebClient wechatWebClient,
            WechatProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = wechatWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 请求微信 code2Session 接口并转换超时和网络异常。
     *
     * @param code 一次性微信登录凭证
     * @return 微信原始会话响应
     */
    public WechatCodeSessionResponse exchange(String code) {
        long startedAt = System.nanoTime();
        log.debug("event=wechat_login stage=wechat_request_started");
        try {
            URI requestUri = buildCodeSessionUri(code);
            WechatCodeSessionResponse response = webClient.get()
                    .uri(requestUri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(this::parseResponseBody)
                    .timeout(properties.responseTimeout())
                    .block();
            log.debug(
                    "event=wechat_login stage=wechat_request_completed upstreamErrorCode={} elapsedMs={}",
                    response == null ? null : response.errcode(),
                    elapsedMillis(startedAt));
            return response;
        } catch (RuntimeException error) {
            Throwable cause = Exceptions.unwrap(error);
            if (cause instanceof WechatClientException clientException) {
                throw clientException;
            }
            if (cause instanceof TimeoutException) {
                log.warn(
                        "event=wechat_login stage=wechat_request_failed errorCode=WECHAT_TIMEOUT elapsedMs={}",
                        elapsedMillis(startedAt));
                throw new WechatClientException(
                        "WECHAT_TIMEOUT",
                        "微信服务请求超时",
                        error);
            }
            log.warn(
                    "event=wechat_login stage=wechat_request_failed errorCode=WECHAT_UNAVAILABLE elapsedMs={} exceptionType={}",
                    elapsedMillis(startedAt),
                    cause.getClass().getSimpleName());
            throw new WechatClientException(
                    "WECHAT_UNAVAILABLE",
                    "微信服务暂时不可用",
                    error);
        }
    }

    /**
     * 将微信响应文本解析为会话对象，兼容微信返回 text/plain 响应头的情况。
     *
     * @param responseBody 微信接口原始响应文本
     * @return 微信会话响应
     */
    WechatCodeSessionResponse parseResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new WechatClientException(
                    "WECHAT_UNAVAILABLE",
                    "微信服务返回空响应",
                    null);
        }
        try {
            return objectMapper.readValue(
                    responseBody,
                    WechatCodeSessionResponse.class);
        } catch (JsonProcessingException error) {
            log.warn(
                    "event=wechat_login stage=wechat_response_parse_failed responseLength={}",
                    responseBody.length());
            throw new WechatClientException(
                    "WECHAT_UNAVAILABLE",
                    "微信服务返回了无法识别的响应",
                    error);
        }
    }

    /**
     * 按微信官方 code2Session 参数构建已编码 URI，避免配置值被当作模板变量展开。
     *
     * @param code 一次性微信登录凭证
     * @return 可直接交给 WebClient 的 URI
     */
    URI buildCodeSessionUri(String code) {
        return UriComponentsBuilder
                .fromUriString(CODE_SESSION_ENDPOINT)
                .queryParam("appid", properties.appId())
                .queryParam("secret", properties.appSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .encode()
                .toUri();
    }

    /**
     * 计算外部请求已耗费毫秒数。
     */
    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
