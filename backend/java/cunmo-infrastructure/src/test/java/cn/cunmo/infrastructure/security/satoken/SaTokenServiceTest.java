package cn.cunmo.infrastructure.security.satoken;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class SaTokenServiceTest {

    /**
     * 验证退出只调用当前 Token 注销操作。
     */
    @Test
    void logsOutOnlyThroughTheCurrentTokenOperation() {
        Runnable currentTokenLogout = mock(Runnable.class);
        SaTokenService service = new SaTokenService(currentTokenLogout);

        service.logoutCurrentToken();

        verify(currentTokenLogout).run();
    }
}
