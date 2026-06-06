package cn.cunmo.api.user.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 当前用户资料更新请求。
 *
 * @param nickname 用户主动填写的昵称
 * @param avatarUrl 已上传成功的头像访问地址
 */
public record UpdateProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 64, message = "昵称不能超过64个字符")
        String nickname,
        @NotBlank(message = "头像不能为空")
        @Size(max = 512, message = "头像地址长度无效")
        String avatarUrl) {
}
