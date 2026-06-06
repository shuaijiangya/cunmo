package cn.cunmo.infrastructure.persistence.inventory.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

/**
 * inv_vault 数据库映射对象。
 */
@TableName("inv_vault")
public class InventoryVaultDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerUserId;
    private String vaultName;
    private Integer itemLimitPerSpaceCategory;
    private Integer status;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;

    /** 返回主键。 */
    public Long getId() { return id; }
    /** 设置主键。 */
    public void setId(Long id) { this.id = id; }
    /** 返回所有者。 */
    public Long getOwnerUserId() { return ownerUserId; }
    /** 设置所有者。 */
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    /** 返回域名称。 */
    public String getVaultName() { return vaultName; }
    /** 设置域名称。 */
    public void setVaultName(String vaultName) { this.vaultName = vaultName; }
    /** 返回组合容量。 */
    public Integer getItemLimitPerSpaceCategory() { return itemLimitPerSpaceCategory; }
    /** 设置组合容量。 */
    public void setItemLimitPerSpaceCategory(Integer value) { this.itemLimitPerSpaceCategory = value; }
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
