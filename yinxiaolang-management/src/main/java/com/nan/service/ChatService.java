package com.nan.service;

import com.nan.entity.ChatSession;
import com.nan.entity.Message;
import com.nan.entity.dto.ChatRequest;
import com.nan.entity.dto.ChatResponse;

import java.util.List;
import java.util.Optional;

/**
 * 聊天服务接口
 */
public interface ChatService {
    
    /**
     * 创建聊天会话
     * @param userId 用户ID
     * @param modelType 默认模型类型
     * @return 会话信息
     */
    ChatSession createSession(Long userId, String modelType);
    
    /**
     * 发送聊天消息并获取AI回复
     * @param request 聊天请求
     * @return AI回复
     */
    ChatResponse sendMessage(ChatRequest request);
    
    /**
     * 保存用户消息 (用于流式响应)
     * @param request 聊天请求
     * @return 保存的消息
     */
    Message saveUserMessage(ChatRequest request);
    
    /**
     * 保存AI消息 (用于流式响应)
     * @param message AI消息
     * @return 保存的消息
     */
    Message saveAiMessage(Message message);
    
    /**
     * 获取会话历史消息
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<Message> getSessionMessages(String sessionId);
    
    /**
     * 获取用户的所有会话
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> getUserSessions(Long userId);
    
    /**
     * 删除会话
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean deleteSession(String sessionId);
    
    /**
     * 根据ID查找会话
     * @param sessionId 会话ID
     * @return 会话信息
     */
    Optional<ChatSession> findSessionById(String sessionId);
    
    /**
     * 保存会话信息
     * @param session 会话
     * @return 保存后的会话
     */
    ChatSession saveSession(ChatSession session);
} 