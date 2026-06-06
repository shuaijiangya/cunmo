package cn.cunmo.domain.auth.gateway;

import cn.cunmo.domain.auth.model.valueobject.WechatPrincipal;

/**
 * 微信身份交换网关。
 */
public interface WechatGateway {
    /**
     * 使用一次性登录凭证交换微信身份。
     *
     * @param code wx.login 返回的临时凭证
     * @return 已验证的微信身份
     */
    WechatPrincipal exchangeCode(String code);
}
