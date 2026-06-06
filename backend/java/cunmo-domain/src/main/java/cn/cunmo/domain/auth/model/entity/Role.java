package cn.cunmo.domain.auth.model.entity;

import cn.cunmo.domain.auth.model.enums.DataScope;

/**
 * 角色领域实体。
 */
public record Role(
        long id,
        String code,
        String name,
        DataScope dataScope,
        boolean enabled) {
}
