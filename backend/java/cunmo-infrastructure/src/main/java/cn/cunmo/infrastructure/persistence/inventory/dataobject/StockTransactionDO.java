package cn.cunmo.infrastructure.persistence.inventory.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

/**
 * inv_stock_transaction 数据库映射对象。
 */
@TableName("inv_stock_transaction")
public class StockTransactionDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vaultId;
    private Long itemId;
    private String transactionType;
    private Integer quantityDelta;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private String description;
    private String itemNameSnapshot;
    private String spaceSnapshot;
    private String categorySnapshot;
    private String detailLocationSnapshot;
    private String sourceSpaceSnapshot;
    private String sourceCategorySnapshot;
    private String sourceDetailLocationSnapshot;
    private String targetSpaceSnapshot;
    private String targetCategorySnapshot;
    private String targetDetailLocationSnapshot;
    private Long operatorUserId;
    private Instant occurredAt;

    /** 返回主键。 */
    public Long getId() { return id; }
    /** 设置主键。 */
    public void setId(Long id) { this.id = id; }
    /** 返回域主键。 */
    public Long getVaultId() { return vaultId; }
    /** 设置域主键。 */
    public void setVaultId(Long vaultId) { this.vaultId = vaultId; }
    /** 返回物品主键。 */
    public Long getItemId() { return itemId; }
    /** 设置物品主键。 */
    public void setItemId(Long itemId) { this.itemId = itemId; }
    /** 返回流水类型。 */
    public String getTransactionType() { return transactionType; }
    /** 设置流水类型。 */
    public void setTransactionType(String value) { this.transactionType = value; }
    /** 返回变化量。 */
    public Integer getQuantityDelta() { return quantityDelta; }
    /** 设置变化量。 */
    public void setQuantityDelta(Integer value) { this.quantityDelta = value; }
    /** 返回变化前数量。 */
    public Integer getQuantityBefore() { return quantityBefore; }
    /** 设置变化前数量。 */
    public void setQuantityBefore(Integer value) { this.quantityBefore = value; }
    /** 返回变化后数量。 */
    public Integer getQuantityAfter() { return quantityAfter; }
    /** 设置变化后数量。 */
    public void setQuantityAfter(Integer value) { this.quantityAfter = value; }
    /** 返回说明。 */
    public String getDescription() { return description; }
    /** 设置说明。 */
    public void setDescription(String description) { this.description = description; }
    /** 返回物品名称快照。 */
    public String getItemNameSnapshot() { return itemNameSnapshot; }
    /** 设置物品名称快照。 */
    public void setItemNameSnapshot(String value) { this.itemNameSnapshot = value; }
    /** 返回空间快照。 */
    public String getSpaceSnapshot() { return spaceSnapshot; }
    /** 设置空间快照。 */
    public void setSpaceSnapshot(String value) { this.spaceSnapshot = value; }
    /** 返回分类快照。 */
    public String getCategorySnapshot() { return categorySnapshot; }
    /** 设置分类快照。 */
    public void setCategorySnapshot(String value) { this.categorySnapshot = value; }
    /** 返回精准位置快照。 */
    public String getDetailLocationSnapshot() { return detailLocationSnapshot; }
    /** 设置精准位置快照。 */
    public void setDetailLocationSnapshot(String value) { this.detailLocationSnapshot = value; }
    /** 返回来源空间快照。 */
    public String getSourceSpaceSnapshot() { return sourceSpaceSnapshot; }
    /** 设置来源空间快照。 */
    public void setSourceSpaceSnapshot(String value) { this.sourceSpaceSnapshot = value; }
    /** 返回来源分类快照。 */
    public String getSourceCategorySnapshot() { return sourceCategorySnapshot; }
    /** 设置来源分类快照。 */
    public void setSourceCategorySnapshot(String value) { this.sourceCategorySnapshot = value; }
    /** 返回来源精准位置快照。 */
    public String getSourceDetailLocationSnapshot() { return sourceDetailLocationSnapshot; }
    /** 设置来源精准位置快照。 */
    public void setSourceDetailLocationSnapshot(String value) { this.sourceDetailLocationSnapshot = value; }
    /** 返回目标空间快照。 */
    public String getTargetSpaceSnapshot() { return targetSpaceSnapshot; }
    /** 设置目标空间快照。 */
    public void setTargetSpaceSnapshot(String value) { this.targetSpaceSnapshot = value; }
    /** 返回目标分类快照。 */
    public String getTargetCategorySnapshot() { return targetCategorySnapshot; }
    /** 设置目标分类快照。 */
    public void setTargetCategorySnapshot(String value) { this.targetCategorySnapshot = value; }
    /** 返回目标精准位置快照。 */
    public String getTargetDetailLocationSnapshot() { return targetDetailLocationSnapshot; }
    /** 设置目标精准位置快照。 */
    public void setTargetDetailLocationSnapshot(String value) { this.targetDetailLocationSnapshot = value; }
    /** 返回操作用户。 */
    public Long getOperatorUserId() { return operatorUserId; }
    /** 设置操作用户。 */
    public void setOperatorUserId(Long value) { this.operatorUserId = value; }
    /** 返回发生时间。 */
    public Instant getOccurredAt() { return occurredAt; }
    /** 设置发生时间。 */
    public void setOccurredAt(Instant value) { this.occurredAt = value; }
}
