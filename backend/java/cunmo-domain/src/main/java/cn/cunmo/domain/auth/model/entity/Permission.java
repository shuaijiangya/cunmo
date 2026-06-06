package cn.cunmo.domain.auth.model.entity;

import cn.cunmo.domain.auth.model.enums.PermissionType;

/**
 * 权限资源领域实体。
 */
public record Permission(
        long id,
        String code,
        String name,
        PermissionType type,
        boolean enabled) {
}
