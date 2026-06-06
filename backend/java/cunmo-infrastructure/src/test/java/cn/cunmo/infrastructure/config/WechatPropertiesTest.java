package cn.cunmo.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * 微信配置校验测试。
 */
class WechatPropertiesTest {

    /**
     * 验证未解析的 Spring 占位符会在启动配置阶段被拒绝。
     */
    @Test
    void rejectsUnresolvedEnvironmentPlaceholder() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new WechatProperties(
                        "${WECHAT_APP_ID}",
                        "secret",
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(5)));

        assertEquals(
                "WECHAT_APP_ID 未配置，请在环境变量或 backend/java/.env 中设置真实值",
                error.getMessage());
    }

    /**
     * 验证合法微信配置能够完成绑定。
     */
    @Test
    void acceptsResolvedWechatConfiguration() {
        WechatProperties properties = new WechatProperties(
                "wx-app-id",
                "app-secret",
                Duration.ofSeconds(3),
                Duration.ofSeconds(5));

        assertEquals("wx-app-id", properties.appId());
    }

    /**
     * 验证示例文件中的占位值不能被当作真实 AppSecret 使用。
     */
    @Test
    void rejectsExampleSecretPlaceholder() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WechatProperties(
                        "wx-app-id",
                        "replace_me",
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(5)));
    }
}
