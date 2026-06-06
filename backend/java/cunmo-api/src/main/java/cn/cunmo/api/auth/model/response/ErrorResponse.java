package cn.cunmo.api.auth.model.response;

/**
 * API 稳定错误结构。
 */
public record ErrorResponse(String code, String message) {
}
