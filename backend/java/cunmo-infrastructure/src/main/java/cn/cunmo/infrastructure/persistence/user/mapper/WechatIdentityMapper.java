package cn.cunmo.infrastructure.persistence.user.mapper;

import cn.cunmo.infrastructure.persistence.user.dataobject.UserDO;
import cn.cunmo.infrastructure.persistence.user.dataobject.WechatIdentityDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 微信身份表 Mapper。
 */
public interface WechatIdentityMapper
        extends BaseMapper<WechatIdentityDO> {

    /**
     * 按 AppID 和 OpenID 查询关联的有效用户。
     */
    @Select("""
            SELECT u.id, u.nickname, u.avatar_url, u.status
            FROM sys_wechat_identity identity
            JOIN sys_user u ON u.id = identity.user_id
            WHERE identity.appid = #{appId}
              AND identity.openid = #{openId}
              AND u.deleted = 0
            LIMIT 1
            """)
    UserDO selectUserByPrincipal(
            @Param("appId") String appId,
            @Param("openId") String openId);
}
