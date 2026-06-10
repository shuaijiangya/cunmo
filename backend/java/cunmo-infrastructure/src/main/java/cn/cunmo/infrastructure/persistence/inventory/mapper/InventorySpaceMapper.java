package cn.cunmo.infrastructure.persistence.inventory.mapper;

import cn.cunmo.infrastructure.persistence.inventory.dataobject.InventorySpaceDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存空间 Mapper。
 */
public interface InventorySpaceMapper extends BaseMapper<InventorySpaceDO> {
    @Select("""
            SELECT COUNT(*)
            FROM inv_space
            WHERE vault_id = #{vaultId}
              AND status = 1 AND deleted = 0
            """)
    int countActiveSpaces(@Param("vaultId") long vaultId);
}
