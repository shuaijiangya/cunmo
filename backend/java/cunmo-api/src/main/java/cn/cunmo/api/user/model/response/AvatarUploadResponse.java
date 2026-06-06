package cn.cunmo.api.user.model.response;

/**
 * 头像上传响应。
 *
 * @param avatarUrl 可持久化的头像访问地址
 */
public record AvatarUploadResponse(String avatarUrl) {
}
