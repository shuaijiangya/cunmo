package cn.cunmo.domain.auth.model.aggregate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.cunmo.domain.auth.exception.DomainException;
import cn.cunmo.domain.auth.model.enums.UserStatus;
import cn.cunmo.domain.auth.model.valueobject.UserId;
import org.junit.jupiter.api.Test;

class UserTest {

    /**
     * 验证微信新用户默认启用且资料未完善。
     */
    @Test
    void newWechatUserIsEnabledAndProfileIsIncomplete() {
        User user = User.createWechatUser();

        assertDoesNotThrow(user::assertCanLogin);
        assertFalse(user.profileCompleted());
    }

    /**
     * 验证昵称和头像同时存在时资料状态完整。
     */
    @Test
    void completeProfileRequiresNicknameAndAvatar() {
        User user = User.reconstitute(
                UserId.of(10L), "存魔用户", "https://cdn.example/avatar.png", UserStatus.ENABLED);

        assertTrue(user.profileCompleted());
    }

    /**
     * 验证禁用用户无法通过登录状态校验。
     */
    @Test
    void disabledUserCannotLogin() {
        User user = User.reconstitute(
                UserId.of(10L), null, null, UserStatus.DISABLED);

        DomainException error = assertThrows(DomainException.class, user::assertCanLogin);
        assertTrue(error.code().equals("USER_DISABLED"));
    }
}
