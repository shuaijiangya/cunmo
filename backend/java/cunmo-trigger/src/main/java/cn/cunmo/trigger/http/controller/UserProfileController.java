package cn.cunmo.trigger.http.controller;

import cn.cunmo.api.user.model.request.UpdateProfileRequest;
import cn.cunmo.api.user.model.response.AvatarUploadResponse;
import cn.cunmo.api.user.model.response.UserProfileResponse;
import cn.cunmo.application.auth.port.CurrentUserProvider;
import cn.cunmo.application.user.command.UpdateProfileCommand;
import cn.cunmo.application.user.result.UserProfileResult;
import cn.cunmo.application.user.service.UserProfileApplicationService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 当前登录用户资料 HTTP 入口。
 */
@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {
    private static final Logger log =
            LoggerFactory.getLogger(UserProfileController.class);

    private final UserProfileApplicationService profileService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * 创建用户资料控制器。
     *
     * @param profileService 用户资料应用服务
     * @param currentUserProvider 当前用户读取端口
     */
    public UserProfileController(
            UserProfileApplicationService profileService,
            CurrentUserProvider currentUserProvider) {
        this.profileService = profileService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 接收微信 chooseAvatar 返回的临时头像文件。
     *
     * @param file 头像文件
     * @return 可长期访问的头像地址
     * @throws IOException 文件读取失败
     */
    @PostMapping("/avatar")
    public AvatarUploadResponse uploadAvatar(
            @RequestParam("file") MultipartFile file) throws IOException {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=user_profile stage=avatar_http_request_accepted userId={}",
                userId);
        String relativePath = profileService.uploadAvatar(
                userId,
                file.getBytes(),
                file.getContentType());
        String avatarUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(relativePath)
                .toUriString();
        return new AvatarUploadResponse(avatarUrl);
    }

    /**
     * 保存用户主动填写的昵称及已上传头像地址。
     *
     * @param request 用户资料更新请求
     * @return 最新用户资料
     */
    @PutMapping("/profile")
    public UserProfileResponse updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        long userId = currentUserProvider.requireUserId();
        UserProfileResult result = profileService.updateProfile(
                new UpdateProfileCommand(
                        userId,
                        request.nickname(),
                        request.avatarUrl()));
        return new UserProfileResponse(
                result.userId(),
                result.nickname(),
                result.avatarUrl(),
                result.profileCompleted());
    }
}
