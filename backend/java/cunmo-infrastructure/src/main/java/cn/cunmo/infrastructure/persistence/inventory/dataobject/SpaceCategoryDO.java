package cn.cunmo.infrastructure.persistence.inventory.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * inv_space_category 数据库映射对象。
 */
@TableName("inv_space_category")
public class SpaceCategoryDO {
    private Long vaultId;
    private Long spaceId;
    private Long categoryId;
    private Integer sortOrder;

    /** 返回域主键。 */
    public Long getVaultId() { return vaultId; }
    /** 设置域主键。 */
    public void setVaultId(Long vaultId) { this.vaultId = vaultId; }
    /** 返回空间主键。 */
    public Long getSpaceId() { return spaceId; }
    /** 设置空间主键。 */
    public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
    /** 返回分类主键。 */
    public Long getCategoryId() { return categoryId; }
    /** 设置分类主键。 */
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    /** 返回排序。 */
    public Integer getSortOrder() { return sortOrder; }
    /** 设置排序。 */
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
