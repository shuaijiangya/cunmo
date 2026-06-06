package cn.cunmo.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 微信服务端配置。敏感配置必须由环境变量注入。
 */
@Validated
@ConfigurationProperties(prefix = "wechat")
public record WechatProperties(
        @NotBlank String appId,
        @NotBlank String appSecret,
        @NotNull Duration connectTimeout,
        @NotNull Duration responseTimeout) {

    /**
     * 在应用启动阶段校验微信配置，阻止未解析的环境变量进入 HTTP 请求。
     */
    public WechatProperties {
        appId = requireResolved("WECHAT_APP_ID", appId);
        appSecret = requireResolved("WECHAT_APP_SECRET", appSecret);
        if (connectTimeout == null || connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("wechat.connect-timeout 必须大于 0");
        }
        if (responseTimeout == null || responseTimeout.isZero() || responseTimeout.isNegative()) {
            throw new IllegalArgumentException("wechat.response-timeout 必须大于 0");
        }
    }

    /**
     * 校验配置值不为空且不包含 Spring 占位符文本。
     *
     * @param environmentName 对应环境变量名称
     * @param value 配置值
     * @return 已清理的配置值
     */
    private static String requireResolved(String environmentName, String value) {
        if (value == null
                || value.isBlank()
                || value.contains("${")
                || value.contains("}")
                || value.equalsIgnoreCase("replace_me")
                || value.equalsIgnoreCase("wx_your_appid")) {
            throw new IllegalArgumentException(
                    environmentName + " 未配置，请在环境变量或 backend/java/.env 中设置真实值");
        }
        return value.trim();
    }
}
