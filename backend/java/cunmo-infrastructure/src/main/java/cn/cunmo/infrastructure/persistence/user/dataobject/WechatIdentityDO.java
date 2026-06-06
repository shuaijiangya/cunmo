package cn.cunmo.infrastructure.persistence.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * sys_wechat_identity 数据库映射对象。
 */
@TableName("sys_wechat_identity")
public class WechatIdentityDO {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String appid;
    private String openid;
    private String unionid;

    /** 返回微信身份主键。 */
    public Long getId() {
        return id;
    }

    /** 设置微信身份主键。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 返回关联用户主键。 */
    public Long getUserId() {
        return userId;
    }

    /** 设置关联用户主键。 */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** 返回小程序 AppID。 */
    public String getAppid() {
        return appid;
    }

    /** 设置小程序 AppID。 */
    public void setAppid(String appid) {
        this.appid = appid;
    }

    /** 返回微信 OpenID。 */
    public String getOpenid() {
        return openid;
    }

    /** 设置微信 OpenID。 */
    public void setOpenid(String openid) {
        this.openid = openid;
    }

    /** 返回微信 UnionID。 */
    public String getUnionid() {
        return unionid;
    }

    /** 设置微信 UnionID。 */
    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }
}
