package cn.cunmo.infrastructure.gateway.wechat;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.domain.auth.gateway.WechatGateway;
import cn.cunmo.domain.auth.model.valueobject.WechatPrincipal;
import cn.cunmo.infrastructure.config.WechatProperties;
import cn.cunmo.infrastructure.gateway.wechat.dto.WechatCodeSessionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 微信身份交换网关实现。
 */
@Component
public class WechatGatewayImpl implements WechatGateway {
    private static final Logger log =
            LoggerFactory.getLogger(WechatGatewayImpl.class);

    private final WechatApiClient client;
    private final WechatProperties properties;

    /**
     * 创建微信领域网关适配器。
     */
    public WechatGatewayImpl(
            WechatApiClient client,
            WechatProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * 将微信响应转换为领域身份，并隐藏 session_key 等基础设施字段。
     */
    @Override
    public WechatPrincipal exchangeCode(String code) {
        try {
            WechatCodeSessionResponse response = client.exchange(code);
            if (response == null) {
                throw new ApplicationException(
                        "WECHAT_UNAVAILABLE",
                        "微信服务暂时不可用");
            }
            if (!response.successful()) {
                String errorCode = mapWechatErrorCode(response.errcode());
                log.warn(
                        "event=wechat_login stage=wechat_response_rejected errorCode={} upstreamErrorCode={}",
                        errorCode,
                        response.errcode());
                throw new ApplicationException(
                        errorCode,
                        mapWechatErrorMessage(errorCode));
            }
            log.debug(
                    "event=wechat_login stage=wechat_response_verified");
            return new WechatPrincipal(
                    properties.appId(),
                    response.openid(),
                    response.unionid());
        } catch (WechatClientException error) {
            log.warn(
                    "event=wechat_login stage=wechat_gateway_failed errorCode={}",
                    error.code());
            throw new ApplicationException(
                    error.code(),
                    error.getMessage(),
                    error);
        }
    }

    /**
     * 将微信官方错误码转换为稳定业务错误码。
     *
     * @param upstreamErrorCode 微信 code2Session 错误码
     * @return 业务错误码
     */
    private String mapWechatErrorCode(Integer upstreamErrorCode) {
        if (upstreamErrorCode == null || upstreamErrorCode == -1) {
            return "WECHAT_UNAVAILABLE";
        }
        return switch (upstreamErrorCode) {
            case 40029 -> "INVALID_CODE";
            case 45011 -> "WECHAT_RATE_LIMITED";
            case 40226 -> "WECHAT_RISK_CONTROL";
            default -> "WECHAT_UNAVAILABLE";
        };
    }

    /**
     * 将业务错误码转换为安全的用户提示。
     *
     * @param errorCode 业务错误码
     * @return 不包含微信敏感响应的提示
     */
    private String mapWechatErrorMessage(String errorCode) {
        return switch (errorCode) {
            case "INVALID_CODE" -> "登录凭证无效或已过期";
            case "WECHAT_RATE_LIMITED" -> "登录请求过于频繁，请稍后重试";
            case "WECHAT_RISK_CONTROL" -> "当前登录请求未通过微信安全校验";
            default -> "微信服务暂时不可用";
        };
    }
}
