package com.nan.service;

import java.util.function.Consumer;

/**
 * AI模型服务接口
 */
public interface AiModelService {
    
    /**
     * 生成AI响应
     * @param prompt 用户输入
     * @return AI响应文本
     */
    String generateResponse(String prompt);
    
    /**
     * 生成流式AI响应
     * @param prompt 用户输入
     * @param chunkConsumer 文本块消费者回调
     */
    default void generateStreamResponse(String prompt, Consumer<String> chunkConsumer) {
        String response = generateResponse(prompt);
        
        // 默认实现：将完整响应按字符分割并逐个发送
        if (response != null && !response.isEmpty()) {
            for (char c : response.toCharArray()) {
                chunkConsumer.accept(String.valueOf(c));
                try {
                    // 模拟流式输出的延迟
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    /**
     * 清理响应内容中的多余字符
     * @param content 原始响应内容
     * @return 清理后的内容
     */
    default String cleanResponseText(String content) {
        if (content == null) {
            return "";
        }
        
        // 替换连续的多个问号为单个问号
        content = content.replaceAll("\\?{2,}", "?");
        
        // 移除末尾的问号（如果末尾有多个空格+问号的模式）
        content = content.replaceAll("\\s*\\?+\\s*$", "");
        
        // 移除Unicode替换字符和其他特殊字符
        content = content.replaceAll("\uFFFD", "");
        content = content.replaceAll("\\u0000", ""); // NULL字符
        
        // 修复断句问题
        content = content.replaceAll("\\s+([,.!?;:])", "$1");
        
        return content;
    }
    
    /**
     * 获取模型名称
     * @return 模型名称
     */
    String getModelName();
    
    /**
     * 获取模型类型
     * @return 模型类型
     */
    String getModelType();
} 