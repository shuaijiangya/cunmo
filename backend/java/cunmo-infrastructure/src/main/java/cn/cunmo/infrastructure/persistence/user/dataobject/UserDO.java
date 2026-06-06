package cn.cunmo.infrastructure.persistence.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * sys_user 数据库映射对象。
 */
@TableName("sys_user")
public class UserDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String nickname;

    @TableField("avatar_url")
    private String avatarUrl;

    private Integer status;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    /** 返回用户主键。 */
    public Long getId() {
        return id;
    }

    /** 设置用户主键。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 返回用户昵称。 */
    public String getNickname() {
        return nickname;
    }

    /** 设置用户昵称。 */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 返回用户头像地址。 */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /** 设置用户头像地址。 */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /** 返回用户状态数据库值。 */
    public Integer getStatus() {
        return status;
    }

    /** 设置用户状态数据库值。 */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /** 返回逻辑删除标记。 */
    public Integer getDeleted() {
        return deleted;
    }

    /** 设置逻辑删除标记。 */
    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
