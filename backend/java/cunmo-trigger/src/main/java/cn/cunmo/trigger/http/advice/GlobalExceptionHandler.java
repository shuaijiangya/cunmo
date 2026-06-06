package cn.cunmo.trigger.http.advice;

import cn.cunmo.api.auth.model.response.ErrorResponse;
import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.domain.auth.exception.DomainException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 将领域和应用异常转换为稳定的 HTTP 错误协议。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<String, HttpStatus> STATUS_BY_CODE = Map.ofEntries(
            Map.entry("INVALID_CODE", HttpStatus.UNAUTHORIZED),
            Map.entry("WECHAT_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT),
            Map.entry("WECHAT_UNAVAILABLE", HttpStatus.BAD_GATEWAY),
            Map.entry("WECHAT_RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS),
            Map.entry("WECHAT_RISK_CONTROL", HttpStatus.FORBIDDEN),
            Map.entry("UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
            Map.entry("USER_DISABLED", HttpStatus.FORBIDDEN),
            Map.entry("USER_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("INVENTORY_VAULT_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("SPACE_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("ITEM_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("INSUFFICIENT_STOCK", HttpStatus.CONFLICT),
            Map.entry("INVENTORY_CONFLICT", HttpStatus.CONFLICT),
            Map.entry("CATEGORY_CAPACITY_EXCEEDED", HttpStatus.CONFLICT),
            Map.entry("CATEGORY_UNBIND_BLOCKED", HttpStatus.CONFLICT),
            Map.entry("CATEGORY_NOT_BOUND", HttpStatus.BAD_REQUEST),
            Map.entry("DUPLICATE_SPACE", HttpStatus.CONFLICT),
            Map.entry("DUPLICATE_CATEGORY", HttpStatus.CONFLICT),
            Map.entry("INVALID_AVATAR", HttpStatus.BAD_REQUEST),
            Map.entry(
                    "AVATAR_STORAGE_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR),
            Map.entry(
                    "DEFAULT_ROLE_MISSING",
                    HttpStatus.INTERNAL_SERVER_ERROR),
            Map.entry("DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR));

    /**
     * 将应用异常映射为稳定错误响应。
     */
    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ErrorResponse> handleApplication(
            ApplicationException error) {
        log.warn(
                "event=application_request stage=application_failed errorCode={} exceptionType={}",
                error.code(),
                error.getClass().getSimpleName());
        return response(error.code(), error.getMessage());
    }

    /**
     * 将领域规则异常映射为稳定错误响应。
     */
    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> handleDomain(DomainException error) {
        log.warn(
                "event=application_request stage=domain_rejected errorCode={} exceptionType={}",
                error.code(),
                error.getClass().getSimpleName());
        return response(error.code(), error.getMessage());
    }

    /**
     * 将参数校验失败统一映射为 INVALID_REQUEST。
     */
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        IllegalArgumentException.class
    })
    ResponseEntity<ErrorResponse> handleInvalidRequest(Exception error) {
        log.warn(
                "event=application_request stage=request_validation_failed errorCode=INVALID_REQUEST exceptionType={}",
                error.getClass().getSimpleName());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", "请求参数错误"));
    }

    /**
     * 将超出上传限制的头像映射为稳定业务错误。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handleUploadTooLarge(
            MaxUploadSizeExceededException error) {
        log.warn(
                "event=user_profile stage=avatar_rejected errorCode=INVALID_AVATAR exceptionType={}",
                error.getClass().getSimpleName());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        "INVALID_AVATAR",
                        "头像文件不能超过2MB"));
    }

    /**
     * 捕获未分类异常并隐藏内部实现细节。
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception error) {
        log.error(
                "event=application_request stage=unexpected_failure errorCode=INTERNAL_ERROR",
                error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "服务内部错误"));
    }

    /**
     * 按业务错误码选择 HTTP 状态并创建响应。
     */
    private ResponseEntity<ErrorResponse> response(
            String code,
            String message) {
        HttpStatus status =
                STATUS_BY_CODE.getOrDefault(code, HttpStatus.BAD_REQUEST);
        log.debug(
                "event=application_request stage=error_response_built errorCode={} httpStatus={}",
                code,
                status.value());
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message));
    }
}
