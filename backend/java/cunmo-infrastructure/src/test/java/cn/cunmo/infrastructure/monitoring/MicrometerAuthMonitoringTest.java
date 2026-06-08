package cn.cunmo.infrastructure.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MicrometerAuthMonitoringTest {

    /**
     * 验证登录与退出的计数器和耗时指标。
     */
    @Test
    void recordsLoginAndLogoutCountersAndTimers() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerAuthMonitoring monitoring =
                new MicrometerAuthMonitoring(registry);

        monitoring.loginSucceeded(Duration.ofMillis(25));
        monitoring.loginFailed(
                "USER_DISABLED",
                Duration.ofMillis(10));
        monitoring.logoutSucceeded(Duration.ofMillis(5));
        monitoring.logoutFailed(
                "UNAUTHORIZED",
                Duration.ofMillis(3));

        assertEquals(
                1.0,
                registry.counter("cunmo.auth.login.success").count());
        assertEquals(
                1.0,
                registry.counter(
                        "cunmo.auth.login.failure",
                        "error_code",
                        "USER_DISABLED").count());
        assertEquals(
                1,
                registry.timer(
                        "cunmo.auth.login.duration",
                        "outcome",
                        "success").count());
        assertEquals(
                1,
                registry.timer(
                        "cunmo.auth.login.duration",
                        "outcome",
                        "failure").count());
        assertEquals(
                1.0,
                registry.counter("cunmo.auth.logout.success").count());
        assertEquals(
                1.0,
                registry.counter(
                        "cunmo.auth.logout.failure",
                        "error_code",
                        "UNAUTHORIZED").count());
    }

    /**
     * 验证未知错误码不会形成无界指标标签。
     */
    @Test
    void normalizesUnknownErrorCodesToInternalError() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerAuthMonitoring monitoring =
                new MicrometerAuthMonitoring(registry);

        monitoring.loginFailed(
                "password=secret",
                Duration.ofMillis(1));

        assertEquals(
                1.0,
                registry.counter(
                        "cunmo.auth.login.failure",
                        "error_code",
                        "INTERNAL_ERROR").count());
    }
}
