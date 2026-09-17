package com.xxxtxm.common.result;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 统一响应体
 * <p>所有 API 返回数据均使用此类包装</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    /** 状态码：200=成功 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 数据载荷 */
    private T data;

    // ──────────── 成功响应 ────────────

    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    /** 无数据的成功响应（仅消息） */
    public static R<Void> okMsg(String message) {
        return new R<>(200, message, null);
    }

    // ──────────── 失败响应 ────────────

    public static <T> R<T> fail(String message) {
        return new R<>(50000, message, null);
    }

    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }

    // ──────────── 常用错误码 ────────────

    /** 参数校验失败 */
    public static <T> R<T> badRequest(String message) {
        return new R<>(40000, message, null);
    }

    /** 未登录 */
    public static <T> R<T> unauthorized() {
        return new R<>(40100, "请先登录", null);
    }

    /** 无权限 */
    public static <T> R<T> forbidden() {
        return new R<>(40300, "权限不足", null);
    }

    /** 资源不存在 */
    public static <T> R<T> notFound(String message) {
        return new R<>(40400, message, null);
    }
}
