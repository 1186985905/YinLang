package com.nan.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户消息内容
     */
    private String message;
    
    /**
     * AI模型类型: openai, deepseek, qianwen等
     */
    private String modelType;
    
    /**
     * 模型名称(可选)
     */
    private String modelName;
} 