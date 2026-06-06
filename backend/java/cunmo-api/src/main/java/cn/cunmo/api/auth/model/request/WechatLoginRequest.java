package cn.cunmo.api.auth.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 微信小程序登录请求。
 *
 * @param code wx.login 返回的一次性临时凭证
 */
public record WechatLoginRequest(
        @NotBlank(message = "登录凭证不能为空")
        @Size(max = 256, message = "登录凭证长度无效")
        String code) {
}
