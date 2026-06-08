package cn.cunmo.trigger.http.controller;

import cn.cunmo.api.auth.model.request.WechatLoginRequest;
import cn.cunmo.api.auth.model.response.LoginResponse;
import cn.cunmo.application.auth.command.WechatLoginCommand;
import cn.cunmo.application.auth.service.LogoutApplicationService;
import cn.cunmo.application.auth.service.WechatLoginApplicationService;
import cn.cunmo.trigger.http.converter.AuthHttpConverter;
import jakarta.validation.Valid;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信登录 HTTP 入口。
 */
@RestController
@RequestMapping("/api/auth")
public class WechatAuthController {
    private static final Logger log =
            LoggerFactory.getLogger(WechatAuthController.class);

    private final WechatLoginApplicationService loginService;
    private final LogoutApplicationService logoutService;

    /**
     * 创建微信登录控制器。
     *
     * @param loginService 微信登录应用服务
     * @param logoutService 当前 Token 退出应用服务
     */
    public WechatAuthController(
            WechatLoginApplicationService loginService,
            LogoutApplicationService logoutService) {
        this.loginService = loginService;
        this.logoutService = logoutService;
    }

    /**
     * 接收微信登录凭证并返回业务登录态。
     *
     * @param request 微信登录请求
     * @return 业务登录态
     */
    @PostMapping("/wechat/login")
    public LoginResponse login(
            @Valid @RequestBody WechatLoginRequest request) {
        long startedAt = System.nanoTime();
        log.info("event=wechat_login stage=http_request_accepted");
        LoginResponse response = AuthHttpConverter.toResponse(
                loginService.login(new WechatLoginCommand(request.code())));
        log.info(
                "event=wechat_login stage=http_response_ready userId={} elapsedMs={}",
                response.user().id(),
                TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startedAt));
        return response;
    }

    /**
     * 注销当前请求携带的业务 Token。
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        logoutService.logout();
    }
}
