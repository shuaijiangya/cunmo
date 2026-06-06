package cn.cunmo.application.user.service;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.application.user.command.UpdateProfileCommand;
import cn.cunmo.application.user.port.AvatarStorage;
import cn.cunmo.application.user.result.UserProfileResult;
import cn.cunmo.domain.auth.model.aggregate.User;
import cn.cunmo.domain.auth.model.valueobject.UserId;
import cn.cunmo.domain.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * 当前用户资料应用服务。
 */
public class UserProfileApplicationService {
    private static final Logger log =
            LoggerFactory.getLogger(UserProfileApplicationService.class);

    private static final int MAX_AVATAR_BYTES = 2 * 1024 * 1024;

    private final UserRepository userRepository;
    private final AvatarStorage avatarStorage;

    /**
     * 创建用户资料应用服务。
     *
     * @param userRepository 用户聚合仓储
     * @param avatarStorage 头像存储端口
     */
    public UserProfileApplicationService(
            UserRepository userRepository,
            AvatarStorage avatarStorage) {
        this.userRepository = userRepository;
        this.avatarStorage = avatarStorage;
    }

    /**
     * 保存用户主动选择的头像文件。
     *
     * @param userId 当前登录用户主键
     * @param content 文件内容
     * @param contentType 文件媒体类型
     * @return 应用内头像相对地址
     */
    public String uploadAvatar(
            long userId,
            byte[] content,
            String contentType) {
        log.info(
                "event=user_profile stage=avatar_upload_started userId={} contentType={} size={}",
                userId,
                contentType,
                content.length);
        if (content.length == 0 || content.length > MAX_AVATAR_BYTES) {
            throw new ApplicationException(
                    "INVALID_AVATAR",
                    "头像文件不能为空且不能超过2MB");
        }
        if (!"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)
                && !"image/webp".equals(contentType)) {
            throw new ApplicationException(
                    "INVALID_AVATAR",
                    "仅支持 JPG、PNG 或 WebP 头像");
        }
        String path = avatarStorage.store(userId, content, contentType);
        log.info(
                "event=user_profile stage=avatar_upload_completed userId={}",
                userId);
        return path;
    }

    /**
     * 更新昵称与头像地址并返回最新资料。
     *
     * @param command 更新资料命令
     * @return 最新用户资料
     */
    @Transactional
    public UserProfileResult updateProfile(UpdateProfileCommand command) {
        UserId userId = UserId.of(command.userId());
        log.info(
                "event=user_profile stage=profile_update_started userId={}",
                userId.value());
        User user = userRepository.updateProfile(
                userId,
                command.nickname().trim(),
                command.avatarUrl().trim());
        log.info(
                "event=user_profile stage=profile_update_completed userId={} profileCompleted={}",
                user.id().value(),
                user.profileCompleted());
        return new UserProfileResult(
                user.id().value(),
                user.nickname(),
                user.avatarUrl(),
                user.profileCompleted());
    }
}
