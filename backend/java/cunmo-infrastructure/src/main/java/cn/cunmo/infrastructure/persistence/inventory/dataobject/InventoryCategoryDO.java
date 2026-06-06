package cn.cunmo.infrastructure.persistence.inventory.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

/**
 * inv_category 数据库映射对象。
 */
@TableName("inv_category")
public class InventoryCategoryDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vaultId;
    private String categoryCode;
    private String categoryName;
    private Integer sortOrder;
    private Integer status;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;

    /** 返回主键。 */
    public Long getId() { return id; }
    /** 设置主键。 */
    public void setId(Long id) { this.id = id; }
    /** 返回域主键。 */
    public Long getVaultId() { return vaultId; }
    /** 设置域主键。 */
    public void setVaultId(Long vaultId) { this.vaultId = vaultId; }
    /** 返回分类编码。 */
    public String getCategoryCode() { return categoryCode; }
    /** 设置分类编码。 */
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    /** 返回分类名称。 */
    public String getCategoryName() { return categoryName; }
    /** 设置分类名称。 */
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    /** 返回排序。 */
    public Integer getSortOrder() { return sortOrder; }
    /** 设置排序。 */
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    /** 返回状态。 */
    public Integer getStatus() { return status; }
    /** 设置状态。 */
    public void setStatus(Integer status) { this.status = status; }
    /** 返回版本。 */
    public Integer getVersion() { return version; }
    /** 设置版本。 */
    public void setVersion(Integer version) { this.version = version; }
    /** 返回删除标记。 */
    public Integer getDeleted() { return deleted; }
    /** 设置删除标记。 */
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
