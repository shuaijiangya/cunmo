package cn.cunmo.application.auth.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cunmo.application.auth.port.AuthMonitoring;
import cn.cunmo.application.auth.port.CurrentUserProvider;
import cn.cunmo.application.auth.port.TokenService;
import cn.cunmo.application.exception.ApplicationException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutApplicationServiceTest {
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private TokenService tokenService;
    @Mock
    private AuthMonitoring authMonitoring;

    private LogoutApplicationService service;

    /**
     * 为每个测试创建退出应用服务。
     */
    @BeforeEach
    void setUp() {
        service = new LogoutApplicationService(
                currentUserProvider,
                tokenService,
                authMonitoring);
    }

    /**
     * 验证退出前先读取当前用户，并只注销当前 Token。
     */
    @Test
    void readsCurrentUserBeforeLoggingOutCurrentToken() {
        when(currentUserProvider.requireUserId()).thenReturn(42L);

        service.logout();

        InOrder order = inOrder(currentUserProvider, tokenService);
        order.verify(currentUserProvider).requireUserId();
        order.verify(tokenService).logoutCurrentToken();
        verify(authMonitoring).logoutSucceeded(any(Duration.class));
    }

    /**
     * 验证无效会话记录稳定的未授权失败指标。
     */
    @Test
    void recordsUnauthorizedWhenCurrentSessionIsInvalid() {
        ApplicationException error = new ApplicationException(
                "UNAUTHORIZED",
                "登录状态已失效，请重新登录");
        when(currentUserProvider.requireUserId()).thenThrow(error);

        assertThrows(ApplicationException.class, service::logout);

        verify(authMonitoring).logoutFailed(
                eq("UNAUTHORIZED"),
                any(Duration.class));
    }
}
