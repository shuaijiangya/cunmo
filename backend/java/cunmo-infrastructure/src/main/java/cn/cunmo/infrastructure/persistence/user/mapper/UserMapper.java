package cn.cunmo.infrastructure.persistence.user.mapper;

import cn.cunmo.infrastructure.persistence.user.dataobject.UserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 系统用户表 Mapper。
 */
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 更新用户最近登录时间并递增乐观锁版本。
     */
    @Update("""
            UPDATE sys_user
            SET last_login_at = #{loginTime},
                updated_at = CURRENT_TIMESTAMP(3),
                version = version + 1
            WHERE id = #{userId} AND deleted = 0
            """)
    int updateLastLogin(
            @Param("userId") long userId,
            @Param("loginTime") Instant loginTime);

    /**
     * 更新用户昵称和头像地址。
     */
    @Update("""
            UPDATE sys_user
            SET nickname = #{nickname},
                avatar_url = #{avatarUrl},
                updated_at = CURRENT_TIMESTAMP(3),
                version = version + 1
            WHERE id = #{userId} AND deleted = 0
            """)
    int updateProfile(
            @Param("userId") long userId,
            @Param("nickname") String nickname,
            @Param("avatarUrl") String avatarUrl);
}
