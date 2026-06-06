package cn.cunmo.infrastructure.persistence.user.mapper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.junit.jupiter.api.Test;

/**
 * MyBatis-Plus 基础 Mapper 接入契约测试。
 */
class MapperBaseMapperContractTest {

    /**
     * 验证用户单表 Mapper 复用 MyBatis-Plus 通用 CRUD。
     */
    @Test
    void userMapperExtendsBaseMapper() {
        assertTrue(BaseMapper.class.isAssignableFrom(UserMapper.class));
    }

    /**
     * 验证微信身份单表 Mapper 复用 MyBatis-Plus 通用 CRUD。
     */
    @Test
    void wechatIdentityMapperExtendsBaseMapper() {
        assertTrue(BaseMapper.class.isAssignableFrom(
                WechatIdentityMapper.class));
    }
}
