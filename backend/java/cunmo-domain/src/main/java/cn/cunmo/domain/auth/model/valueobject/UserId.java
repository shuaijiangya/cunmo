package cn.cunmo.domain.auth.model.valueobject;

/**
 * 用户聚合标识。
 */
public record UserId(Long value) {

    /**
     * 校验已分配的用户主键必须为正数。
     */
    public UserId {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException("用户主键必须为正数");
        }
    }

    /**
     * 创建已分配的用户标识。
     */
    public static UserId of(long value) {
        return new UserId(value);
    }

    /**
     * 创建尚未分配数据库主键的用户标识。
     */
    public static UserId unassigned() {
        return new UserId(null);
    }

    /**
     * 判断用户标识是否已分配主键。
     */
    public boolean assigned() {
        return value != null;
    }
}
