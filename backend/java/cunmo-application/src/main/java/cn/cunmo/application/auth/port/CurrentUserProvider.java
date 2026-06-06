package cn.cunmo.application.auth.port;

/**
 * 当前登录用户身份读取端口。
 */
public interface CurrentUserProvider {

    /**
     * 返回当前登录用户主键，未登录时抛出应用异常。
     *
     * @return 当前用户主键
     */
    long requireUserId();
}
