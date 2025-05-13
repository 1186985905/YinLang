package com.nan.controller;

import com.nan.entity.ApiResult;
import com.nan.entity.ChatSession;
import com.nan.entity.Message;
import com.nan.entity.dto.ChatRequest;
import com.nan.entity.dto.ChatResponse;
import com.nan.service.AiModelFactory;
import com.nan.service.AiModelService;
import com.nan.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * 聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    private final AiModelFactory aiModelFactory;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // 存储会话ID与其最近的请求数据关联
    private final Map<String, ChatRequest> sessionRequests = new ConcurrentHashMap<>();
    
    /**
     * 创建聊天会话
     */
    @PostMapping("/session")
    public ApiResult<ChatSession> createSession(@RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) String modelType) {
        // 临时用户ID
        if (userId == null) {
            userId = 1L;
        }
        return ApiResult.success(chatService.createSession(userId, modelType));
    }
    
    /**
     * 发送聊天消息
     */
    @PostMapping("/message")
    public ApiResult<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        try {
            return ApiResult.success(chatService.sendMessage(request));
        } catch (ResourceAccessException e) {
            log.error("AI模型请求超时", e);
            if (e.getCause() instanceof SocketTimeoutException) {
                return ApiResult.error(HttpStatus.REQUEST_TIMEOUT.value(), "AI模型响应超时，请稍后重试或提交更简短的问题");
            }
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "网络请求错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理消息请求失败", e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "处理请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 流式发送聊天消息 (SSE) - POST 方法保存请求并启动处理
     */
    @PostMapping("/stream")
    public ApiResult<String> startStreamProcessing(@RequestBody ChatRequest request) {
        try {
            // 确保会话存在
            if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
                // 创建新会话
                ChatSession session = chatService.createSession(1L, request.getModelType());
                request.setSessionId(session.getId());
            }
            
            // 保存用户消息
            chatService.saveUserMessage(request);
            
            // 保存请求数据，以便 GET 端点使用
            sessionRequests.put(request.getSessionId(), request);
            
            return ApiResult.success("流式处理已启动，请使用 GET 请求获取流式响应", "success");
        } catch (Exception e) {
            log.error("启动流式处理失败", e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "启动流式处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 流式发送聊天消息 (SSE) - GET 方法获取流式响应
     */
    @GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getStreamResponse(@PathVariable String sessionId) {
        // 配置SSE
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        // 获取会话的请求数据
        ChatRequest request = sessionRequests.get(sessionId);
        if (request == null) {
            try {
                emitter.send(SseEmitter.event()
                        .data("无效的会话或会话已过期")
                        .name("error"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        // 获取对应的AI模型服务
        AiModelService aiService = aiModelFactory.getAiModelService(request.getModelType());
        
        // 在新线程中处理流式响应
        executorService.execute(() -> {
            try {
                StringBuilder fullResponse = new StringBuilder();
                
                // 为所有模型添加上下文
                String promptToUse = request.getMessage();
                // 通用为所有模型添加上下文历史
                promptToUse = addContextToPrompt(request.getSessionId(), request.getMessage());
                
                // 使用流式响应API
                aiService.generateStreamResponse(promptToUse, (chunk) -> {
                    try {
                        // 清理每个数据块中的干扰字符
                        String cleanedChunk = cleanChunk(chunk);
                        
                        // 发送数据块
                        emitter.send(SseEmitter.event()
                                .data(cleanedChunk)
                                .id(UUID.randomUUID().toString())
                                .name("message"));
                        
                        // 累积完整响应
                        fullResponse.append(cleanedChunk);
                    } catch (IOException e) {
                        log.error("发送SSE消息失败", e);
                        emitter.completeWithError(e);
                    }
                });
                
                // 保存AI消息
                Message aiMessage = Message.builder()
                        .sessionId(request.getSessionId())
                        .role("assistant")
                        .content(fullResponse.toString())
                        .modelType(request.getModelType())
                        .modelName(aiService.getModelName())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                
                chatService.saveAiMessage(aiMessage);
                
                // 发送完成事件
                emitter.send(SseEmitter.event()
                        .data("[DONE]"));
                
                // 完成后移除请求数据
                sessionRequests.remove(sessionId);
                
                // 完成
                emitter.complete();
                
            } catch (ResourceAccessException e) {
                log.error("AI模型请求超时", e);
                try {
                    if (e.getCause() instanceof SocketTimeoutException) {
                        emitter.send(SseEmitter.event()
                                .data("AI模型响应超时，请稍后重试或提交更简短的问题")
                                .name("error"));
                    } else {
                        emitter.send(SseEmitter.event()
                                .data("网络请求错误: " + e.getMessage())
                                .name("error"));
                    }
                    emitter.complete();
                } catch (IOException ioException) {
                    emitter.completeWithError(ioException);
                } finally {
                    sessionRequests.remove(sessionId);
                }
            } catch (Exception e) {
                log.error("流式处理消息失败", e);
                emitter.completeWithError(e);
                sessionRequests.remove(sessionId);
            }
        });
        
        // 设置超时和错误处理
        emitter.onCompletion(() -> {
            log.info("SSE完成");
            sessionRequests.remove(sessionId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE超时");
            sessionRequests.remove(sessionId);
        });
        emitter.onError((e) -> {
            log.error("SSE错误", e);
            sessionRequests.remove(sessionId);
        });
        
        return emitter;
    }
    
    /**
     * 获取会话历史消息
     */
    @GetMapping("/messages/{sessionId}")
    public ApiResult<List<Message>> getSessionMessages(@PathVariable String sessionId) {
        return ApiResult.success(chatService.getSessionMessages(sessionId));
    }
    
    /**
     * 获取用户的所有会话
     */
    @GetMapping("/sessions")
    public ApiResult<List<ChatSession>> getUserSessions(@RequestParam(required = false) Long userId) {
        // 临时用户ID
        if (userId == null) {
            userId = 1L;
        }
        return ApiResult.success(chatService.getUserSessions(userId));
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public ApiResult<Boolean> deleteSession(@PathVariable String sessionId) {
        return ApiResult.success(chatService.deleteSession(sessionId));
    }
    
    /**
     * 获取支持的模型类型
     */
    @GetMapping("/models")
    public ApiResult<List<String>> getSupportedModelTypes() {
        return ApiResult.success(aiModelFactory.getSupportedModelTypes());
    }

    /**
     * 全局异常处理
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<String> handleException(Exception e) {
        log.error("全局异常", e);
        return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器错误: " + e.getMessage());
    }

    /**
     * 超时异常处理
     */
    @ExceptionHandler({TimeoutException.class, SocketTimeoutException.class})
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public ApiResult<String> handleTimeoutException(Exception e) {
        log.error("请求超时", e);
        return ApiResult.error(HttpStatus.REQUEST_TIMEOUT.value(), "AI模型响应超时，请稍后重试或提交更简短的问题");
    }

    /**
     * 清理响应块中的干扰字符
     */
    private String cleanChunk(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return "";
        }
        
        // 替换连续多个问号为单个问号
        chunk = chunk.replaceAll("\\?{2,}", "?");
        
        // 替换或清理其他可能的干扰字符
        chunk = chunk.replaceAll("[\\p{Cc}&&[^\n\t\r]]", "");
        
        // 修复断句问题（只有在块结尾含有断句符号时应用）
        chunk = chunk.replaceAll("\\s+([,.!?;:])$", "$1");
        
        // 进一步清理图片URL和相关内容
        if (chunk.contains("http://localhost") && chunk.contains("/uploads/")) {
            // 清理图片URL
            chunk = chunk.replaceAll("http://localhost:\\d+/uploads/[^\\s]+(?:\\.jpg|\\.jpeg|\\.png|\\.gif|\\.bmp)", "[图片]");
            chunk = chunk.replaceAll("分析这张图片.*", "[图片分析请求]");
            log.info("清理了响应块中的图片URL: {}", chunk);
            
            // 清理文档URL
            chunk = chunk.replaceAll("http://localhost:\\d+/uploads/[^\\s]+(?:\\.txt|\\.docx|\\.doc)", "[文档]");
            chunk = chunk.replaceAll("已解析文件:.*", "[文档内容]");
            log.info("清理了响应块中的文档URL: {}", chunk);
        }
        
        return chunk;
    }

    /**
     * 为提示添加会话上下文
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @return 带有上下文的完整提示
     */
    private String addContextToPrompt(String sessionId, String userMessage) {
        // 检查是否图片分析请求 - 不需要添加上下文
        boolean isImageMsg = userMessage.contains("http://localhost") && 
              (userMessage.contains(".jpg") || userMessage.contains(".jpeg") || 
               userMessage.contains(".png") || userMessage.contains(".gif") || 
               userMessage.contains(".bmp"));
        
        // 检查是否文档解析请求 - 不需要添加上下文
        boolean isDocumentMsg = userMessage.startsWith("已解析文件:") || 
              (userMessage.contains("/uploads/") && 
               (userMessage.contains(".txt") || userMessage.contains(".docx") || userMessage.contains(".doc")));
        
        if (isImageMsg) {
            log.info("检测到图片分析请求，跳过添加上下文: {}", 
                    userMessage.length() > 100 ? userMessage.substring(0, 100) + "..." : userMessage);
            return userMessage;
        }
        
        if (isDocumentMsg) {
            log.info("检测到文档解析请求，跳过添加上下文: {}", 
                    userMessage.length() > 100 ? userMessage.substring(0, 100) + "..." : userMessage);
            return userMessage;
        }
        
        // 获取会话消息历史
        List<Message> messages = chatService.getSessionMessages(sessionId);
        if (messages.isEmpty()) {
            return userMessage;
        }
        
        // 构建带有上下文的提示
        StringBuilder contextPrompt = new StringBuilder();
        contextPrompt.append("以下是历史对话记录，请参考后回答用户的问题：\n\n");
        
        // 获取最近的有限数量的消息作为上下文
        int historyLimit = Math.min(messages.size(), 10);
        List<Message> recentMessages = messages.subList(messages.size() - historyLimit, messages.size());
        
        // 添加每条历史消息
        for (Message msg : recentMessages) {
            String role = msg.getRole().equals("user") ? "用户" : "AI";
            String content = msg.getContent();
            
            // 进一步清理图片URL和相关内容
            if (content.contains("http://localhost") && content.contains("/uploads/")) {
                // 清理图片URL
                content = content.replaceAll("http://localhost:\\d+/uploads/[^\\s]+(?:\\.jpg|\\.jpeg|\\.png|\\.gif|\\.bmp)", "[图片]");
                content = content.replaceAll("分析这张图片.*", "[图片分析请求]");
                log.info("清理了历史消息中的图片URL: {}", content);
                
                // 清理文档URL
                content = content.replaceAll("http://localhost:\\d+/uploads/[^\\s]+(?:\\.txt|\\.docx|\\.doc)", "[文档]");
                content = content.replaceAll("已解析文件:.*", "[文档内容]");
                log.info("清理了历史消息中的文档URL: {}", content);
            }
                               
            contextPrompt.append(role).append(": ").append(content).append("\n\n");
        }
        
        // 添加当前用户消息
        contextPrompt.append("用户: ").append(userMessage);
        
        return contextPrompt.toString();
    }

    /**
     * 重命名会话
     */
    @PutMapping("/session/{sessionId}/title")
    public ApiResult<ChatSession> renameSession(@PathVariable String sessionId, @RequestBody Map<String, String> body) {
        try {
            String title = body.get("title");
            if (title == null || title.trim().isEmpty()) {
                return ApiResult.error(HttpStatus.BAD_REQUEST.value(), "标题不能为空");
            }
            
            // 获取会话
            ChatSession session = chatService.findSessionById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("会话不存在: " + sessionId));
            
            // 更新标题
            session.setTitle(title);
            chatService.saveSession(session);
            
            return ApiResult.success(session);
        } catch (IllegalArgumentException e) {
            log.error("重命名会话失败: {}", e.getMessage());
            return ApiResult.error(HttpStatus.NOT_FOUND.value(), e.getMessage());
        } catch (Exception e) {
            log.error("重命名会话失败: {}", sessionId, e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "重命名会话失败: " + e.getMessage());
        }
    }
} 