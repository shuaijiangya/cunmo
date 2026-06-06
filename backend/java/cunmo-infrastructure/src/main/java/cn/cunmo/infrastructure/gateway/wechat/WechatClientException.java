package cn.cunmo.infrastructure.gateway.wechat;

/**
 * 微信 HTTP 客户端内部异常。
 */
final class WechatClientException extends RuntimeException {
    private final String code;

    /**
     * 创建微信客户端内部异常。
     */
    WechatClientException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 返回微信客户端错误码。
     */
    String code() {
        return code;
    }
}
