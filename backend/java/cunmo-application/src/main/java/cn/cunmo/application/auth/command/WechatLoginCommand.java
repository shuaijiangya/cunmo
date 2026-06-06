package cn.cunmo.application.auth.command;

/**
 * 微信登录应用命令。
 */
public record WechatLoginCommand(String code) {
    /**
     * 清理并校验微信一次性登录凭证。
     */
    public WechatLoginCommand {
        if (code == null || code.isBlank() || code.length() > 256) {
            throw new IllegalArgumentException("登录凭证无效");
        }
        code = code.trim();
    }
}
