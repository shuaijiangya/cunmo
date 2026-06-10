package cn.cunmo.infrastructure.persistence.inventory.mapper;

import cn.cunmo.infrastructure.persistence.inventory.dataobject.SpaceCategoryDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 空间分类关联 Mapper。
 */
public interface SpaceCategoryMapper {

    /**
     * 新增空间分类绑定。
     */
    @Insert("""
            INSERT INTO inv_space_category (
                vault_id, space_id, category_id, sort_order
            ) VALUES (
                #{vaultId}, #{spaceId}, #{categoryId}, #{sortOrder}
            )
            """)
    int insertBinding(SpaceCategoryDO binding);

    /**
     * 判断空间分类绑定是否存在。
     */
    @Select("""
            SELECT COUNT(*)
            FROM inv_space_category
            WHERE vault_id = #{vaultId}
              AND space_id = #{spaceId}
              AND category_id = #{categoryId}
            """)
    int countBinding(
            @Param("vaultId") long vaultId,
            @Param("spaceId") long spaceId,
            @Param("categoryId") long categoryId);

    /**
     * 查询分类当前绑定空间主键。
     */
    @Select("""
            SELECT space_id
            FROM inv_space_category
            WHERE vault_id = #{vaultId} AND category_id = #{categoryId}
            """)
    List<Long> selectSpaceIds(
            @Param("vaultId") long vaultId,
            @Param("categoryId") long categoryId);

    @Select("""
            SELECT COUNT(*)
            FROM inv_space_category binding
            JOIN inv_category category
              ON category.id = binding.category_id
             AND category.vault_id = binding.vault_id
             AND category.status = 1 AND category.deleted = 0
            WHERE binding.vault_id = #{vaultId}
              AND binding.space_id = #{spaceId}
            """)
    int countActiveCategories(
            @Param("vaultId") long vaultId,
            @Param("spaceId") long spaceId);

    /**
     * 删除指定空间分类绑定。
     */
    @Delete("""
            DELETE FROM inv_space_category
            WHERE vault_id = #{vaultId}
              AND category_id = #{categoryId}
              AND space_id = #{spaceId}
            """)
    int deleteBinding(
            @Param("vaultId") long vaultId,
            @Param("categoryId") long categoryId,
            @Param("spaceId") long spaceId);
}
