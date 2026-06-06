package cn.cunmo.infrastructure.persistence.inventory.mapper;

import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventoryVaultDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 魔方域 Mapper。
 */
public interface InventoryVaultMapper extends BaseMapper<InventoryVaultDO> {

    /**
     * 按所有者读取有效魔方域。
     */
    @Select("""
            SELECT id, owner_user_id, vault_name,
                   item_limit_per_space_category, status, version, deleted
            FROM inv_vault
            WHERE owner_user_id = #{userId}
              AND status = 1 AND deleted = 0
            LIMIT 1
            """)
    InventoryVaultDO selectByOwner(@Param("userId") long userId);

    /**
     * 按所有者锁定有效魔方域。
     */
    @Select("""
            SELECT id, owner_user_id, vault_name,
                   item_limit_per_space_category, status, version, deleted
            FROM inv_vault
            WHERE owner_user_id = #{userId}
              AND status = 1 AND deleted = 0
            LIMIT 1
            FOR UPDATE
            """)
    InventoryVaultDO selectByOwnerForUpdate(
            @Param("userId") long userId);
}
