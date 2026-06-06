package cn.cunmo.infrastructure.persistence.inventory.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

/**
 * inv_item 数据库映射对象。
 */
@TableName("inv_item")
public class InventoryItemDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vaultId;
    private Long spaceId;
    private Long categoryId;
    private String itemName;
    private String smallCategory;
    private String detailLocation;
    private Integer quantity;
    private Integer minimumQuantity;
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
    /** 返回空间主键。 */
    public Long getSpaceId() { return spaceId; }
    /** 设置空间主键。 */
    public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
    /** 返回分类主键。 */
    public Long getCategoryId() { return categoryId; }
    /** 设置分类主键。 */
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    /** 返回物品名称。 */
    public String getItemName() { return itemName; }
    /** 设置物品名称。 */
    public void setItemName(String itemName) { this.itemName = itemName; }
    /** 返回细分类。 */
    public String getSmallCategory() { return smallCategory; }
    /** 设置细分类。 */
    public void setSmallCategory(String smallCategory) { this.smallCategory = smallCategory; }
    /** 返回精准位置。 */
    public String getDetailLocation() { return detailLocation; }
    /** 设置精准位置。 */
    public void setDetailLocation(String detailLocation) { this.detailLocation = detailLocation; }
    /** 返回数量。 */
    public Integer getQuantity() { return quantity; }
    /** 设置数量。 */
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    /** 返回预警线。 */
    public Integer getMinimumQuantity() { return minimumQuantity; }
    /** 设置预警线。 */
    public void setMinimumQuantity(Integer value) { this.minimumQuantity = value; }
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
