package cn.cunmo.infrastructure.monitoring;

import cn.cunmo.application.auth.port.AuthMonitoring;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 使用 Micrometer 记录认证业务指标。
 */
@Component
public class MicrometerAuthMonitoring implements AuthMonitoring {
    private static final String ERROR_CODE = "error_code";
    private static final String OUTCOME = "outcome";
    private static final Set<String> KNOWN_ERROR_CODES = Set.of(
            "INVALID_CODE",
            "WECHAT_TIMEOUT",
            "WECHAT_UNAVAILABLE",
            "WECHAT_RATE_LIMITED",
            "WECHAT_RISK_CONTROL",
            "UNAUTHORIZED",
            "USER_DISABLED",
            "DEFAULT_ROLE_MISSING",
            "DATABASE_ERROR",
            "INTERNAL_ERROR");

    private final MeterRegistry registry;

    /**
     * 创建认证指标适配器。
     *
     * @param registry Micrometer 指标注册表
     */
    public MicrometerAuthMonitoring(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 记录成功登录次数和耗时。
     *
     * @param duration 登录端到端耗时
     */
    @Override
    public void loginSucceeded(Duration duration) {
        registry.counter("cunmo.auth.login.success").increment();
        registry.timer(
                "cunmo.auth.login.duration",
                OUTCOME,
                "success").record(duration);
    }

    /**
     * 记录失败登录次数、错误码和耗时。
     *
     * @param errorCode 稳定的业务错误码
     * @param duration 登录端到端耗时
     */
    @Override
    public void loginFailed(String errorCode, Duration duration) {
        registry.counter(
                "cunmo.auth.login.failure",
                ERROR_CODE,
                normalize(errorCode)).increment();
        registry.timer(
                "cunmo.auth.login.duration",
                OUTCOME,
                "failure").record(duration);
    }

    /**
     * 记录成功退出次数和耗时。
     *
     * @param duration 退出端到端耗时
     */
    @Override
    public void logoutSucceeded(Duration duration) {
        registry.counter("cunmo.auth.logout.success").increment();
        registry.timer(
                "cunmo.auth.logout.duration",
                OUTCOME,
                "success").record(duration);
    }

    /**
     * 记录失败退出次数、错误码和耗时。
     *
     * @param errorCode 稳定的业务错误码
     * @param duration 退出端到端耗时
     */
    @Override
    public void logoutFailed(String errorCode, Duration duration) {
        registry.counter(
                "cunmo.auth.logout.failure",
                ERROR_CODE,
                normalize(errorCode)).increment();
        registry.timer(
                "cunmo.auth.logout.duration",
                OUTCOME,
                "failure").record(duration);
    }

    /**
     * 将未知错误码收敛为低基数内部错误码。
     *
     * @param errorCode 原始错误码
     * @return 可用于指标标签的稳定错误码
     */
    private static String normalize(String errorCode) {
        return KNOWN_ERROR_CODES.contains(errorCode)
                ? errorCode
                : "INTERNAL_ERROR";
    }
}
