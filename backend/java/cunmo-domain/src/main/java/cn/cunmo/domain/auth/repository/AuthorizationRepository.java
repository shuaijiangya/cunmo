package cn.cunmo.domain.auth.repository;

import cn.cunmo.domain.auth.model.valueobject.AuthorizationSnapshot;
import cn.cunmo.domain.auth.model.valueobject.UserId;

/**
 * 用户角色和权限查询端口。
 */
public interface AuthorizationRepository {
    /**
     * 查询用户当前生效的角色与权限。
     *
     * @param userId 用户标识
     * @return 授权快照
     */
    AuthorizationSnapshot findActiveAuthorization(UserId userId);
}
