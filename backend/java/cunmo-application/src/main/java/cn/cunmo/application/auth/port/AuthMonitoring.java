package cn.cunmo.application.auth.port;

import java.time.Duration;

/**
 * 认证用例的低基数业务监控端口。
 */
public interface AuthMonitoring {

    /**
     * 记录一次成功登录及其端到端耗时。
     *
     * @param duration 登录端到端耗时
     */
    void loginSucceeded(Duration duration);

    /**
     * 记录一次失败登录、稳定错误码及其端到端耗时。
     *
     * @param errorCode 稳定的业务错误码
     * @param duration 登录端到端耗时
     */
    void loginFailed(String errorCode, Duration duration);

    /**
     * 记录一次成功退出及其端到端耗时。
     *
     * @param duration 退出端到端耗时
     */
    void logoutSucceeded(Duration duration);

    /**
     * 记录一次失败退出、稳定错误码及其端到端耗时。
     *
     * @param errorCode 稳定的业务错误码
     * @param duration 退出端到端耗时
     */
    void logoutFailed(String errorCode, Duration duration);
}
