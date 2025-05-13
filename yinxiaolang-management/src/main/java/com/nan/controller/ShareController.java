package com.nan.controller;

import com.nan.entity.ApiResult;
import com.nan.entity.ChatSession;
import com.nan.entity.Message;
import com.nan.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * 共享功能控制器
 */
@Slf4j
@Controller
@RequestMapping("/share")
@RequiredArgsConstructor
public class ShareController {
    
    private final ChatService chatService;
    
    /**
     * 获取共享会话页面 - 将所有share/**请求转发到前端路由处理
     */
    @GetMapping("/**")
    public ModelAndView getSharedChat() {
        log.info("接收到共享链接请求，转发到前端处理");
        return new ModelAndView("forward:/index.html");
    }
    
    /**
     * 获取共享会话的消息 - API接口
     */
    @GetMapping("/api/{shareId}")
    @ResponseBody
    public ApiResult<List<Message>> getSharedChatMessages(@PathVariable String shareId) {
        try {
            log.info("获取共享会话消息: {}", shareId);
            // 这里使用shareId作为sessionId获取会话消息
            List<Message> messages = chatService.getSessionMessages(shareId);
            if (messages.isEmpty()) {
                log.warn("未找到共享会话消息: {}", shareId);
                return ApiResult.error(HttpStatus.NOT_FOUND.value(), "未找到共享会话消息");
            }
            log.info("成功获取共享会话消息, 消息数: {}", messages.size());
            return ApiResult.success(messages);
        } catch (Exception e) {
            log.error("获取共享会话消息失败: {}", shareId, e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取共享会话消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取共享会话信息 - API接口
     */
    @GetMapping("/api/session/{shareId}")
    @ResponseBody
    public ApiResult<ChatSession> getSharedChatSession(@PathVariable String shareId) {
        try {
            log.info("获取共享会话信息: {}", shareId);
            // 获取会话信息
            ChatSession session = chatService.findSessionById(shareId)
                    .orElseThrow(() -> new IllegalArgumentException("共享会话不存在: " + shareId));
            
            log.info("成功获取共享会话信息: {}", session.getTitle());
            return ApiResult.success(session);
        } catch (IllegalArgumentException e) {
            log.error("获取共享会话信息失败: {}", e.getMessage());
            return ApiResult.error(HttpStatus.NOT_FOUND.value(), e.getMessage());
        } catch (Exception e) {
            log.error("获取共享会话信息失败: {}", shareId, e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取共享会话信息失败: " + e.getMessage());
        }
    }
} 