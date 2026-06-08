package cn.cunmo.application.auth.service;

import cn.cunmo.application.auth.port.AuthMonitoring;
import cn.cunmo.application.auth.port.CurrentUserProvider;
import cn.cunmo.application.auth.port.TokenService;
import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.domain.auth.exception.DomainException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 当前业务 Token 退出用例。
 */
@Service
public class LogoutApplicationService {
    private static final Logger log =
            LoggerFactory.getLogger(LogoutApplicationService.class);

    private final CurrentUserProvider currentUserProvider;
    private final TokenService tokenService;
    private final AuthMonitoring authMonitoring;

    /**
     * 创建当前 Token 退出用例。
     *
     * @param currentUserProvider 当前用户提供器
     * @param tokenService 登录态服务
     * @param authMonitoring 认证监控端口
     */
    public LogoutApplicationService(
            CurrentUserProvider currentUserProvider,
            TokenService tokenService,
            AuthMonitoring authMonitoring) {
        this.currentUserProvider = currentUserProvider;
        this.tokenService = tokenService;
        this.authMonitoring = authMonitoring;
    }

    /**
     * 注销当前请求携带的 Token。
     */
    public void logout() {
        long startedAt = System.nanoTime();
        log.info("event=auth_logout stage=application_started");
        try {
            long userId = currentUserProvider.requireUserId();
            tokenService.logoutCurrentToken();
            authMonitoring.logoutSucceeded(elapsedDuration(startedAt));
            log.info(
                    "event=auth_logout stage=application_completed userId={} elapsedMs={}",
                    userId,
                    elapsedMillis(startedAt));
        } catch (DomainException error) {
            recordFailure(error.code(), error, startedAt);
            throw error;
        } catch (ApplicationException error) {
            recordFailure(error.code(), error, startedAt);
            throw error;
        } catch (RuntimeException error) {
            recordFailure("INTERNAL_ERROR", error, startedAt);
            throw error;
        }
    }

    /**
     * 记录退出失败指标和结构化日志。
     *
     * @param errorCode 稳定的业务错误码
     * @param error 退出过程抛出的异常
     * @param startedAt 退出用例开始的纳秒时间
     */
    private void recordFailure(
            String errorCode,
            RuntimeException error,
            long startedAt) {
        authMonitoring.logoutFailed(
                errorCode,
                elapsedDuration(startedAt));
        log.warn(
                "event=auth_logout stage=application_failed errorCode={} exceptionType={} elapsedMs={}",
                errorCode,
                error.getClass().getSimpleName(),
                elapsedMillis(startedAt));
    }

    /**
     * 将纳秒起始时间转换为耗时对象。
     *
     * @param startedAt 起始纳秒时间
     * @return 已耗费时长
     */
    private static Duration elapsedDuration(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }

    /**
     * 将纳秒起始时间转换为已耗费毫秒数。
     *
     * @param startedAt 起始纳秒时间
     * @return 已耗费毫秒数
     */
    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
