package com.nan.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 消息ID
     */
    private Long messageId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * AI模型类型
     */
    private String modelType;
    
    /**
     * 模型名称
     */
    private String modelName;
    
    /**
     * 响应时间
     */
    private LocalDateTime responseTime;
} 