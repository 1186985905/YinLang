package com.nan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用API返回结果
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 返回消息
     */
    private String message;
    
    /**
     * 返回数据
     */
    private T data;
    
    /**
     * 成功
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }
    
    /**
     * 成功
     */
    public static <T> ApiResult<T> success(String message, T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * 成功，无数据返回
     */
    public static ApiResult<Void> success(String message) {
        return ApiResult.<Void>builder()
                .code(200)
                .message(message)
                .build();
    }
    
    /**
     * 失败
     */
    public static <T> ApiResult<T> error(Integer code, String message) {
        return ApiResult.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
    
    /**
     * 失败，带数据
     */
    public static <T> ApiResult<T> error(Integer code, String message, T data) {
        return ApiResult.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * 常见失败情况（400错误）
     */
    public static <T> ApiResult<T> fail(String message) {
        return error(400, message);
    }
} 