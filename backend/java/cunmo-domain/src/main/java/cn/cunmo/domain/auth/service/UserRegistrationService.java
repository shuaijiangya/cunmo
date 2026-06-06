package cn.cunmo.domain.auth.service;

import cn.cunmo.domain.auth.model.aggregate.User;
import cn.cunmo.domain.auth.model.valueobject.WechatPrincipal;
import cn.cunmo.domain.auth.repository.UserRepository;

/**
 * 封装微信用户注册所需的默认角色领域策略。
 */
public final class UserRegistrationService {
    public static final String DEFAULT_ROLE_CODE = "USER";

    private final UserRepository userRepository;

    /**
     * 创建用户注册领域服务。
     */
    public UserRegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 查找微信用户，不存在时绑定默认普通用户角色完成注册。
     */
    public User findOrRegister(WechatPrincipal principal) {
        return userRepository.findOrRegisterWechatUser(principal, DEFAULT_ROLE_CODE);
    }
}
