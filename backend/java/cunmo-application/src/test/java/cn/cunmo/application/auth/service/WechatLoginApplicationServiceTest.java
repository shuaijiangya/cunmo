package cn.cunmo.application.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cunmo.application.auth.command.WechatLoginCommand;
import cn.cunmo.application.auth.port.TokenService;
import cn.cunmo.application.auth.port.TokenService.TokenResult;
import cn.cunmo.application.auth.result.LoginResult;
import cn.cunmo.domain.auth.exception.DomainException;
import cn.cunmo.domain.auth.gateway.WechatGateway;
import cn.cunmo.domain.auth.model.aggregate.User;
import cn.cunmo.domain.auth.model.enums.UserStatus;
import cn.cunmo.domain.auth.model.valueobject.AuthorizationSnapshot;
import cn.cunmo.domain.auth.model.valueobject.UserId;
import cn.cunmo.domain.auth.model.valueobject.WechatPrincipal;
import cn.cunmo.domain.auth.repository.AuthorizationRepository;
import cn.cunmo.domain.auth.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WechatLoginApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-05T08:00:00Z");

    @Mock
    private WechatGateway wechatGateway;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthorizationRepository authorizationRepository;
    @Mock
    private TokenService tokenService;

    private WechatLoginApplicationService service;

    /**
     * 为每个测试创建固定时钟的登录应用服务。
     */
    @BeforeEach
    void setUp() {
        service = new WechatLoginApplicationService(
                wechatGateway,
                userRepository,
                authorizationRepository,
                tokenService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * 验证登录结果会返回去重后的角色、权限和 Token。
     */
    @Test
    void logsInAndReturnsDistinctRolesAndPermissions() {
        WechatPrincipal principal = new WechatPrincipal("wx-app", "openid", null);
        User user = User.reconstitute(UserId.of(42), null, null, UserStatus.ENABLED);
        when(wechatGateway.exchangeCode("temporary-code")).thenReturn(principal);
        when(userRepository.findOrRegisterWechatUser(principal, "USER")).thenReturn(user);
        when(authorizationRepository.findActiveAuthorization(user.id()))
                .thenReturn(new AuthorizationSnapshot(
                        List.of("USER", "USER"),
                        List.of("inventory:item:read", "inventory:item:read")));
        when(tokenService.issue(user.id(), List.of("USER"), List.of("inventory:item:read")))
                .thenReturn(new TokenResult("business-token", 7200));

        LoginResult result = service.login(new WechatLoginCommand("temporary-code"));

        assertEquals("business-token", result.token());
        assertEquals(List.of("USER"), result.roles());
        assertEquals(List.of("inventory:item:read"), result.permissions());
        verify(userRepository).recordSuccessfulLogin(user.id(), NOW);
    }

    /**
     * 验证禁用用户在签发 Token 前被领域规则拒绝。
     */
    @Test
    void rejectsDisabledUserBeforeIssuingToken() {
        WechatPrincipal principal = new WechatPrincipal("wx-app", "openid", null);
        User user = User.reconstitute(UserId.of(42), null, null, UserStatus.DISABLED);
        when(wechatGateway.exchangeCode("temporary-code")).thenReturn(principal);
        when(userRepository.findOrRegisterWechatUser(principal, "USER")).thenReturn(user);

        DomainException error = assertThrows(
                DomainException.class,
                () -> service.login(new WechatLoginCommand("temporary-code")));

        assertEquals("USER_DISABLED", error.code());
    }
}
