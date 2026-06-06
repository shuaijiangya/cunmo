package cn.cunmo.infrastructure.gateway.wechat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 微信 code2Session 原始响应。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WechatCodeSessionResponse(
        String openid,
        String unionid,
        @JsonProperty("session_key") String sessionKey,
        Integer errcode,
        String errmsg) {

    /**
     * 判断微信响应是否包含有效 OpenID 且无业务错误。
     */
    public boolean successful() {
        return openid != null
                && !openid.isBlank()
                && (errcode == null || errcode == 0);
    }
}
