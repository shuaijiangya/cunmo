package cn.cunmo.infrastructure.persistence.inventory.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

/**
 * inv_space 数据库映射对象。
 */
@TableName("inv_space")
public class InventorySpaceDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vaultId;
    private String spaceCode;
    private String spaceName;
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
    /** 返回空间编码。 */
    public String getSpaceCode() { return spaceCode; }
    /** 设置空间编码。 */
    public void setSpaceCode(String spaceCode) { this.spaceCode = spaceCode; }
    /** 返回空间名称。 */
    public String getSpaceName() { return spaceName; }
    /** 设置空间名称。 */
    public void setSpaceName(String spaceName) { this.spaceName = spaceName; }
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
