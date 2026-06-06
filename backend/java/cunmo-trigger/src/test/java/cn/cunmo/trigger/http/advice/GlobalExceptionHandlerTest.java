package cn.cunmo.trigger.http.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.cunmo.api.auth.model.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局 HTTP 异常映射测试。
 */
class GlobalExceptionHandlerTest {

    /**
     * 验证非法整数参数返回四百而不是服务端错误。
     */
    @Test
    void rejectsMalformedIntegerParameterAsBadRequest()
            throws NoSuchMethodException {
        MethodParameter parameter = new MethodParameter(
                ParameterTarget.class.getDeclaredMethod(
                        "accept",
                        Integer.class),
                0);
        MethodArgumentTypeMismatchException error =
                new MethodArgumentTypeMismatchException(
                        "10unionselect1,2,3,4,5,6,7,8,9,10--",
                        Integer.class,
                        "size",
                        parameter,
                        new NumberFormatException("invalid integer"));

        ResponseEntity<ErrorResponse> response =
                new GlobalExceptionHandler()
                        .handleInvalidParameter(error);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "INVALID_REQUEST_PARAMETER",
                response.getBody().code());
        assertEquals(
                "请求参数格式错误",
                response.getBody().message());
    }

    /**
     * 提供方法参数元数据。
     */
    private static final class ParameterTarget {

        /**
         * 接收整数参数。
         */
        @SuppressWarnings("unused")
        private void accept(Integer size) {
        }
    }
}
