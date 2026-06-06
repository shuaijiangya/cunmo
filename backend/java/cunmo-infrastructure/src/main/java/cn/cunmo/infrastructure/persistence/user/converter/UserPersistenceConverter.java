package cn.cunmo.infrastructure.persistence.user.converter;

import cn.cunmo.domain.auth.model.aggregate.User;
import cn.cunmo.domain.auth.model.enums.UserStatus;
import cn.cunmo.domain.auth.model.valueobject.UserId;
import cn.cunmo.infrastructure.persistence.user.dataobject.UserDO;

/**
 * 用户聚合与数据库对象转换器。
 */
public final class UserPersistenceConverter {
    /**
     * 禁止实例化无状态转换器。
     */
    private UserPersistenceConverter() {
    }

    /**
     * 将用户数据库对象转换为领域聚合。
     */
    public static User toDomain(UserDO data) {
        return User.reconstitute(
                UserId.of(data.getId()),
                data.getNickname(),
                data.getAvatarUrl(),
                UserStatus.fromValue(data.getStatus()));
    }
}
