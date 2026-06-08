package cn.cunmo.trigger.http.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.cunmo.application.auth.result.LoginResult;
import cn.cunmo.application.auth.service.LogoutApplicationService;
import cn.cunmo.application.auth.service.WechatLoginApplicationService;
import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.trigger.http.advice.GlobalExceptionHandler;
import cn.cunmo.trigger.http.filter.RequestTraceFilter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class WechatAuthControllerTest {
    @Mock
    private WechatLoginApplicationService loginService;
    @Mock
    private LogoutApplicationService logoutService;

    private MockMvc mockMvc;

    /**
     * 创建包含控制器、异常处理器和链路过滤器的独立 MockMvc。
     */
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WechatAuthController(
                        loginService,
                        logoutService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestTraceFilter())
                .build();
    }

    /**
     * 验证登录响应包含角色、权限和透传的 traceId。
     */
    @Test
    void returnsUserRolesAndPermissions() throws Exception {
        when(loginService.login(any())).thenReturn(new LoginResult(
                "business-token",
                7200,
                42,
                null,
                null,
                false,
                List.of("USER"),
                List.of("inventory:item:read")));

        mockMvc.perform(post("/api/auth/wechat/login")
                        .header(RequestTraceFilter.TRACE_HEADER, "trace-test-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"temporary-code"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        RequestTraceFilter.TRACE_HEADER,
                        "trace-test-123"))
                .andExpect(jsonPath("$.token").value("business-token"))
                .andExpect(jsonPath("$.user.roles[0]").value("USER"))
                .andExpect(jsonPath("$.user.permissions[0]")
                        .value("inventory:item:read"));
    }

    /**
     * 验证空登录凭证被映射为 INVALID_REQUEST。
     */
    @Test
    void rejectsBlankLoginCode() throws Exception {
        mockMvc.perform(post("/api/auth/wechat/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    /**
     * 验证退出当前 Token 返回无响应体。
     */
    @Test
    void logsOutCurrentToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());

        verify(logoutService).logout();
    }

    /**
     * 验证无效登录态退出时返回稳定的 401 协议。
     */
    @Test
    void rejectsLogoutWhenSessionIsInvalid() throws Exception {
        doThrow(new ApplicationException(
                "UNAUTHORIZED",
                "登录状态已失效，请重新登录"))
                .when(logoutService)
                .logout();

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
