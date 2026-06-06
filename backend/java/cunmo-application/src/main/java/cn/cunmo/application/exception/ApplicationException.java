package cn.cunmo.application.exception;

/**
 * 应用用例或外部端口失败时使用的稳定业务异常。
 */
public class ApplicationException extends RuntimeException {
    private final String code;

    /**
     * 创建不包含底层原因的应用异常。
     */
    public ApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 创建并保留底层原因的应用异常。
     */
    public ApplicationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 返回应用错误码。
     */
    public String code() {
        return code;
    }
}
