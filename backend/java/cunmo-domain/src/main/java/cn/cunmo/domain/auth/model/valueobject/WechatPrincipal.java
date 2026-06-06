package cn.cunmo.domain.auth.model.valueobject;

/**
 * 微信身份值对象。openid 只在服务端领域边界内流转。
 */
public record WechatPrincipal(String appId, String openId, String unionId) {

    /**
     * 校验微信身份中的 AppID 和 OpenID。
     */
    public WechatPrincipal {
        if (appId == null || appId.isBlank()) {
            throw new IllegalArgumentException("微信 AppID 不能为空");
        }
        if (openId == null || openId.isBlank()) {
            throw new IllegalArgumentException("微信 OpenID 不能为空");
        }
    }
}
