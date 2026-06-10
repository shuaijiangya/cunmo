package cn.cunmo.application.membership.port;

/**
 * 会员页面非敏感展示配置。
 */
public interface MembershipSettings {
    boolean customerServiceEnabled();

    String enterpriseWechatQrUrl();

    String phone();

    int renewalMaxAttempts();
}
