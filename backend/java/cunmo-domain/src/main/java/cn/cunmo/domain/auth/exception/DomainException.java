package cn.cunmo.domain.auth.exception;

/**
 * 领域规则被破坏时抛出的稳定业务异常。
 */
public class DomainException extends RuntimeException {
    private final String code;

    /**
     * 创建带稳定错误码的领域异常。
     *
     * @param code 领域错误码
     * @param message 可向调用方返回的错误说明
     */
    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 返回领域错误码。
     *
     * @return 稳定领域错误码
     */
    public String code() {
        return code;
    }
}
