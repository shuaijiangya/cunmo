package cn.cunmo.domain.auth.model.entity;

import cn.cunmo.domain.auth.model.valueobject.UserId;
import cn.cunmo.domain.auth.model.valueobject.WechatPrincipal;

/**
 * 用户聚合之外的微信登录身份实体。
 */
public record WechatIdentity(long id, UserId userId, WechatPrincipal principal) {
}
