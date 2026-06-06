package cn.cunmo.domain.auth.model.enums;

/**
 * 系统用户状态。
 */
public enum UserStatus {
    DISABLED(0),
    ENABLED(1);

    private final int value;

    /**
     * 绑定数据库状态值。
     */
    UserStatus(int value) {
        this.value = value;
    }

    /**
     * 返回数据库状态值。
     */
    public int value() {
        return value;
    }

    /**
     * 将数据库状态值转换为领域枚举。
     *
     * @param value 数据库存储值
     * @return 对应用户状态
     */
    public static UserStatus fromValue(int value) {
        return value == ENABLED.value ? ENABLED : DISABLED;
    }
}
