package com.nan.service.impl;

import com.nan.entity.ChatSession;
import com.nan.entity.Message;
import com.nan.entity.dto.ChatRequest;
import com.nan.entity.dto.ChatResponse;
import com.nan.repository.ChatSessionRepository;
import com.nan.repository.MessageRepository;
import com.nan.service.AiModelFactory;
import com.nan.service.AiModelService;
import com.nan.service.ChatService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 聊天服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final AiModelFactory aiModelFactory;
    private final ChatSessionRepository chatSessionRepository;
    private final MessageRepository messageRepository;
    
    @PostConstruct
    public void init() {
        // 初始化AI模型工厂
        aiModelFactory.init();
    }
    
    @Override
    @Transactional
    public ChatSession createSession(Long userId, String modelType) {
        // 生成会话ID
        String sessionId = UUID.randomUUID().toString();
        
        // 创建会话
        ChatSession session = ChatSession.builder()
                .id(sessionId)
                .title("新对话")
                .userId(userId)
                .defaultModelType(modelType != null ? modelType : "openai")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // 保存会话
        return chatSessionRepository.save(session);
    }
    
    @Override
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        // 获取会话
        ChatSession session = chatSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + request.getSessionId()));
        
        // 确定使用的模型类型
        String modelType = request.getModelType();
        if (modelType == null || modelType.isEmpty()) {
            modelType = session.getDefaultModelType();
        }
        
        // 获取模型服务
        AiModelService aiModelService = aiModelFactory.getAiModelService(modelType);
        
        // 保存用户消息
        Message userMessage = Message.builder()
                .sessionId(request.getSessionId())
                .role("user")
                .content(request.getMessage())
                .modelType(modelType)
                .modelName(aiModelService.getModelName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // 添加到会话消息列表
        messageRepository.save(userMessage);
        
        // 生成AI回复 - 添加上下文
        String promptWithContext = addContextToPrompt(request.getSessionId(), request.getMessage());
        String aiReply = aiModelService.generateResponse(promptWithContext);
        
        // 保存AI回复消息
        Message aiMessage = Message.builder()
                .sessionId(request.getSessionId())
                .role("assistant")
                .content(aiReply)
                .modelType(modelType)
                .modelName(aiModelService.getModelName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // 添加到会话消息列表
        messageRepository.save(aiMessage);
        
        // 更新会话标题和时间
        if (session.getTitle().equals("新对话") && !request.getMessage().isEmpty()) {
            session.setTitle(request.getMessage().length() > 20 ? 
                    request.getMessage().substring(0, 20) + "..." : 
                    request.getMessage());
        }
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
        
        // 构建响应
        return ChatResponse.builder()
                .sessionId(request.getSessionId())
                .messageId(aiMessage.getId())
                .content(aiReply)
                .modelType(modelType)
                .modelName(aiModelService.getModelName())
                .responseTime(LocalDateTime.now())
                .build();
    }
    
    @Override
    @Transactional
    public Message saveUserMessage(ChatRequest request) {
        // 获取会话
        ChatSession session = chatSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + request.getSessionId()));
        
        // 确定使用的模型类型
        String modelType = request.getModelType();
        if (modelType == null || modelType.isEmpty()) {
            modelType = session.getDefaultModelType();
        }
        
        // 获取模型服务
        AiModelService aiModelService = aiModelFactory.getAiModelService(modelType);
        
        // 创建用户消息
        Message userMessage = Message.builder()
                .sessionId(request.getSessionId())
                .role("user")
                .content(request.getMessage())
                .modelType(modelType)
                .modelName(aiModelService.getModelName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // 添加到会话消息列表
        Message savedMessage = messageRepository.save(userMessage);
        
        // 更新会话标题和时间
        if (session.getTitle().equals("新对话") && !request.getMessage().isEmpty()) {
            session.setTitle(request.getMessage().length() > 20 ? 
                    request.getMessage().substring(0, 20) + "..." : 
                    request.getMessage());
        }
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);
        
        return savedMessage;
    }
    
    @Override
    @Transactional
    public Message saveAiMessage(Message message) {
        Message savedMessage = messageRepository.save(message);
        
        // 更新会话时间
        ChatSession session = chatSessionRepository.findById(message.getSessionId()).orElse(null);
        if (session != null) {
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);
        }
        
        return savedMessage;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Message> getSessionMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAt(sessionId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatSession> getUserSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
    
    @Override
    @Transactional
    public boolean deleteSession(String sessionId) {
        try {
            // 首先删除会话的所有消息
            messageRepository.deleteBySessionId(sessionId);
            
            // 然后删除会话
            chatSessionRepository.deleteById(sessionId);
            return true;
        } catch (Exception e) {
            log.error("删除会话失败: {}", sessionId, e);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ChatSession> findSessionById(String sessionId) {
        return chatSessionRepository.findById(sessionId);
    }
    
    @Override
    @Transactional
    public ChatSession saveSession(ChatSession session) {
        return chatSessionRepository.save(session);
    }
    
    /**
     * 添加聊天上下文到提示
     */
    private String addContextToPrompt(String sessionId, String userMessage) {
        StringBuilder contextBuilder = new StringBuilder();
        
        // 获取会话历史消息
        List<Message> historyMessages = messageRepository.findBySessionIdOrderByCreatedAt(sessionId);
        
        // 最多使用最近的5条消息作为上下文
        int startIndex = Math.max(0, historyMessages.size() - 10);
        for (int i = startIndex; i < historyMessages.size(); i++) {
            Message msg = historyMessages.get(i);
            contextBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
        }
        
        // 添加当前用户消息
        contextBuilder.append("user: ").append(userMessage);
        
        return contextBuilder.toString();
    }
}