package cn.cunmo.application.auth.service;

import cn.cunmo.application.auth.command.WechatLoginCommand;
import cn.cunmo.application.auth.port.AuthMonitoring;
import cn.cunmo.application.auth.port.TokenService;
import cn.cunmo.application.auth.port.TokenService.TokenResult;
import cn.cunmo.application.auth.result.LoginResult;
import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.domain.auth.exception.DomainException;
import cn.cunmo.domain.auth.gateway.WechatGateway;
import cn.cunmo.domain.auth.model.aggregate.User;
import cn.cunmo.domain.auth.model.valueobject.AuthorizationSnapshot;
import cn.cunmo.domain.auth.model.valueobject.WechatPrincipal;
import cn.cunmo.domain.auth.repository.AuthorizationRepository;
import cn.cunmo.domain.auth.repository.UserRepository;
import cn.cunmo.domain.auth.service.UserRegistrationService;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 微信登录应用服务。
 *
 * <p>微信网络调用在数据库注册操作之前完成，避免在数据库事务中等待外部服务。</p>
 */
@Service
public class WechatLoginApplicationService {
    private static final Logger log =
            LoggerFactory.getLogger(WechatLoginApplicationService.class);

    private final WechatGateway wechatGateway;
    private final UserRepository userRepository;
    private final UserRegistrationService registrationService;
    private final AuthorizationRepository authorizationRepository;
    private final TokenService tokenService;
    private final AuthMonitoring authMonitoring;
    private final Clock clock;

    /**
     * 组装微信登录用例依赖。
     *
     * @param wechatGateway 微信身份网关
     * @param userRepository 用户仓储
     * @param authorizationRepository 授权仓储
     * @param tokenService 登录态服务
     * @param authMonitoring 认证监控端口
     * @param clock 业务时钟
     */
    public WechatLoginApplicationService(
            WechatGateway wechatGateway,
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository,
            TokenService tokenService,
            AuthMonitoring authMonitoring,
            Clock clock) {
        this.wechatGateway = wechatGateway;
        this.userRepository = userRepository;
        this.registrationService = new UserRegistrationService(userRepository);
        this.authorizationRepository = authorizationRepository;
        this.tokenService = tokenService;
        this.authMonitoring = authMonitoring;
        this.clock = clock;
    }

    /**
     * 编排微信身份交换、用户注册、授权查询和登录态签发。
     *
     * @param command 微信登录命令
     * @return 登录结果
     */
    public LoginResult login(WechatLoginCommand command) {
        long startedAt = System.nanoTime();
        log.info("event=wechat_login stage=application_started");
        try {
            LoginResult result = executeLogin(command, startedAt);
            authMonitoring.loginSucceeded(elapsedDuration(startedAt));
            return result;
        } catch (DomainException error) {
            authMonitoring.loginFailed(
                    error.code(),
                    elapsedDuration(startedAt));
            throw error;
        } catch (ApplicationException error) {
            authMonitoring.loginFailed(
                    error.code(),
                    elapsedDuration(startedAt));
            throw error;
        } catch (RuntimeException error) {
            authMonitoring.loginFailed(
                    "INTERNAL_ERROR",
                    elapsedDuration(startedAt));
            throw error;
        }
    }

    /**
     * 执行微信身份交换、用户加载、授权查询和 Token 签发。
     *
     * @param command 微信登录命令
     * @param startedAt 登录用例开始的纳秒时间
     * @return 登录结果
     */
    private LoginResult executeLogin(
            WechatLoginCommand command,
            long startedAt) {
        long stageStartedAt = System.nanoTime();
        WechatPrincipal principal = wechatGateway.exchangeCode(command.code());
        log.info(
                "event=wechat_login stage=wechat_identity_exchanged elapsedMs={}",
                elapsedMillis(stageStartedAt));

        stageStartedAt = System.nanoTime();
        User user = registrationService.findOrRegister(principal);
        log.info(
                "event=wechat_login stage=user_loaded userId={} elapsedMs={}",
                user.id().value(),
                elapsedMillis(stageStartedAt));

        user.assertCanLogin();
        log.debug(
                "event=wechat_login stage=user_status_verified userId={}",
                user.id().value());

        stageStartedAt = System.nanoTime();
        userRepository.recordSuccessfulLogin(user.id(), clock.instant());
        log.debug(
                "event=wechat_login stage=last_login_recorded userId={} elapsedMs={}",
                user.id().value(),
                elapsedMillis(stageStartedAt));

        stageStartedAt = System.nanoTime();
        AuthorizationSnapshot authorization =
                authorizationRepository.findActiveAuthorization(user.id());
        log.info(
                "event=wechat_login stage=authorization_loaded userId={} roleCount={} permissionCount={} elapsedMs={}",
                user.id().value(),
                authorization.roleCodes().size(),
                authorization.permissionCodes().size(),
                elapsedMillis(stageStartedAt));

        stageStartedAt = System.nanoTime();
        TokenResult token = tokenService.issue(
                user.id(),
                authorization.roleCodes(),
                authorization.permissionCodes());
        log.info(
                "event=wechat_login stage=token_issued userId={} expiresIn={} elapsedMs={}",
                user.id().value(),
                token.expiresIn(),
                elapsedMillis(stageStartedAt));

        LoginResult result = new LoginResult(
                token.token(),
                token.expiresIn(),
                user.id().value(),
                user.nickname(),
                user.avatarUrl(),
                user.profileCompleted(),
                authorization.roleCodes(),
                authorization.permissionCodes());
        log.info(
                "event=wechat_login stage=application_completed userId={} elapsedMs={}",
                user.id().value(),
                elapsedMillis(startedAt));
        return result;
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

    /**
     * 将纳秒起始时间转换为耗时对象。
     *
     * @param startedAt 起始纳秒时间
     * @return 已耗费时长
     */
    private static Duration elapsedDuration(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }
}
