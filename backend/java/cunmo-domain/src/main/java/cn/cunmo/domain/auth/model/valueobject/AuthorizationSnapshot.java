package cn.cunmo.domain.auth.model.valueobject;

import java.util.List;

/**
 * 用户当前有效角色和权限的不可变快照。
 */
public record AuthorizationSnapshot(List<String> roleCodes, List<String> permissionCodes) {

    /**
     * 对角色和权限编码去重、排序并转为不可变集合。
     */
    public AuthorizationSnapshot {
        roleCodes = roleCodes.stream().distinct().sorted().toList();
        permissionCodes = permissionCodes.stream().distinct().sorted().toList();
    }
}
