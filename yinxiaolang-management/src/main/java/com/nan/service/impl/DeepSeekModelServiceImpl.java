package com.nan.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.nan.service.AiModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DeepSeek模型服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekModelServiceImpl implements AiModelService {

    private final RestTemplate restTemplate;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private AiModelService qianwenModelService;

    @Value("${ai-models.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai-models.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${ai-models.deepseek.model:deepseek-chat}")
    private String modelName;

    private static final String MODEL_TYPE = "deepseek";

    private static final String SYSTEM_PROMPT = """
            你是茵浪AI助手，一个功能强大、友好的AI助手。
            你的任务是以简洁、专业、友好的方式回答用户的问题。
            请用中文回答用户的问题。
            重要提示：不要在回答末尾使用多余的标点符号，特别是不要使用连续的问号。
            """;

    @Override
    public String generateResponse(String prompt) {
        try {
            // 获取Qianwen模型服务（延迟初始化，避免循环依赖）
            if (qianwenModelService == null) {
                qianwenModelService = applicationContext.getBean("qianwenModelServiceImpl", AiModelService.class);
            }
            
            // 检查是否包含图片URL，如果包含，则使用Qianwen模型处理
            if (containsImageUrl(prompt)) {
                log.info("检测到图片URL，切换到Qianwen模型处理图片识别请求");
                return qianwenModelService.generateResponse(prompt);
            }
            
            // 检查是否为文档内容，如果是，作为普通文本处理
            if (isDocumentContent(prompt)) {
                log.info("检测到文档内容，作为普通文本处理");
                // 继续使用DeepSeek处理，不需要特殊处理
            }
            
            // 原始DeepSeek模型实现 - 处理纯文本
            // 构建请求URL
            String url = baseUrl + "/v1/chat/completions";
            log.info("DeepSeek请求URL: {}", url);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);

            // 添加系统提示
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);
            messages.add(systemMessage);

            // 添加当前用户消息
            Map<String, String> currentUserMessage = new HashMap<>();
            currentUserMessage.put("role", "user");
            currentUserMessage.put("content", prompt);
            messages.add(currentUserMessage);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            log.info("DeepSeek请求参数: {}", requestBody);

            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);

            // 解析响应
            Map responseBody = responseEntity.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> messageResponse = (Map<String, String>) choice.get("message");
                    String content = messageResponse.get("content");
                    // 清理多余字符
                    return cleanResponseText(content);
                }
            }

            return "DeepSeek模型暂时无法响应，请稍后再试";
        } catch (Exception e) {
            log.error("DeepSeek API调用失败", e);
            return "调用DeepSeek API时发生错误: " + e.getMessage();
        }
    }

    // 用于清理响应文本的方法
    public String cleanResponseText(String content) {
        if (content == null) return "";
        // 这里可以添加清理文本的逻辑
        return content.trim();
    }

    /**
     * 检查文本是否包含图片URL
     */
    private boolean containsImageUrl(String text) {
        if (text == null) return false;
        
        // 检查是否是文档解析内容，如果是则返回false
        if (text.startsWith("已解析文件:")) {
            return false;
        }
        
        // 匹配标准图片URL和localhost上传图片URL - 限制为图片扩展名
        return text.matches("(?s).*https?://[^\\s]+(?:\\.jpg|\\.jpeg|\\.png|\\.gif|\\.bmp).*") ||
               text.matches("(?s).*http://localhost:\\d+/uploads/[^\\s]+(?:\\.jpg|\\.jpeg|\\.png|\\.gif|\\.bmp).*");
    }

    /**
     * 检查内容是否为文档内容
     * @param text 要检查的文本
     * @return 是否为文档内容
     */
    private boolean isDocumentContent(String text) {
        boolean isDocument = text != null && 
               (text.startsWith("已解析文件:") || 
                (text.contains("/uploads/") && 
                 (text.contains(".txt") || text.contains(".docx") || text.contains(".doc"))));
        
        // 添加日志以便调试
        if (isDocument) {
            log.info("DeepSeek检测到文档内容: {}", text.length() > 100 ? text.substring(0, 100) + "..." : text);
        }
        
        return isDocument;
    }

    /**
     * 从文档内容中提取文件名
     * @param text 文档内容
     * @return 文件名
     */
    private String extractDocumentFilename(String text) {
        if (text.startsWith("已解析文件:")) {
            // 从"已解析文件:"中提取文件名
            String[] parts = text.split(":", 2);
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                // 进一步提取文件名部分
                String content = parts[1].trim();
                String filename = content;
                
                // 如果包含URL，尝试从URL中提取文件名
                if (content.contains("/uploads/")) {
                    Pattern pattern = Pattern.compile("/uploads/([^/]+\\.(txt|docx|doc))");
                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        filename = matcher.group(1);
                    }
                }
                
                log.info("从文档内容中提取到文件名: {}", filename);
                return filename;
            }
        } else if (text.contains("/uploads/")) {
            // 从URL中提取文件名
            Pattern pattern = Pattern.compile("/uploads/([^/]+\\.(txt|docx|doc))");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String filename = matcher.group(1);
                log.info("从URL中提取到文件名: {}", filename);
                return filename;
            }
        }
        
        return "";
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String getModelType() {
        return MODEL_TYPE;
    }

    @Override
    public void generateStreamResponse(String prompt, Consumer<String> chunkConsumer) {
        try {
            // 获取Qianwen模型服务（延迟初始化，避免循环依赖）
            if (qianwenModelService == null) {
                qianwenModelService = applicationContext.getBean("qianwenModelServiceImpl", AiModelService.class);
            }
            
            // 调试日志
            log.info("DeepSeek收到请求: prompt长度:{}, 前100个字符:{}", 
                prompt.length(), 
                prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt);
            
            // 检查是否为文档内容
            if (isDocumentContent(prompt)) {
                log.info("检测到文档内容，提取并返回文件名信息");
                
                // 从文档内容中提取文件名
                String filename = extractDocumentFilename(prompt);
                if (!filename.isEmpty()) {
                    try {
                        // 处理文档内容 - 简化对话输出返回文件名信息
                        chunkConsumer.accept("我已收到并解析文件：" + filename + "。请问您有什么问题？");
                        return;
                    } catch (IllegalStateException e) {
                        log.warn("Emitter已完成，无法发送文档响应: {}", e.getMessage());
                        return;
                    }
                }
                
                log.info("检测到文档内容，作为普通文本流式处理");
                // 继续使用DeepSeek处理，不需要特殊处理
            }
            // 检查是否包含图片URL，如果包含，则使用Qianwen模型处理
            else if (containsImageUrl(prompt)) {
                log.info("检测到图片URL，切换到Qianwen模型流式处理图片识别请求");
                qianwenModelService.generateStreamResponse(prompt, chunkConsumer);
                return;
            } else {
                log.info("普通文本请求，使用DeepSeek模型处理");
            }
            
            // 原始DeepSeek流式处理实现
            // 构建请求URL
            String url = baseUrl + "/v1/chat/completions";
            log.info("DeepSeek流式API请求URL: {}", url);
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            
            // 构建消息数组
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);
            messages.add(systemMessage);
            
            // 添加当前用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", true);  // 启用流式输出
            
            // 创建HTTP请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // 添加一个标志，检查是否已经结束了emitter
            final boolean[] isCompleted = {false};
            
            // 定义响应提取器
            restTemplate.execute(url, HttpMethod.POST, req -> {
                req.getHeaders().putAll(requestEntity.getHeaders());
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(req.getBody(), requestEntity.getBody());
            }, res -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.getBody(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (isCompleted[0]) {
                            log.warn("Emitter已完成，停止处理流响应");
                            break;
                        }
                        
                        if (line.isEmpty()) continue;
                        if (!line.startsWith("data:")) continue;
                        if (line.equals("data: [DONE]")) break;
                        
                        // 解析数据行
                        String jsonData = line.substring(5).trim();
                        try {
                            // 使用Jackson解析JSON
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode rootNode = mapper.readTree(jsonData);
                            JsonNode choices = rootNode.path("choices");
                            
                            if (!choices.isEmpty()) {
                                JsonNode choice = choices.get(0);
                                JsonNode delta = choice.path("delta");
                                if (delta.has("content")) {
                                    String content = delta.path("content").asText();
                                    try {
                                        chunkConsumer.accept(content);
                                    } catch (IllegalStateException ise) {
                                        log.warn("Emitter已完成，无法发送更多内容: {}", ise.getMessage());
                                        isCompleted[0] = true;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("解析流响应失败: {}", e.getMessage());
                            try {
                                chunkConsumer.accept("\n解析响应出错: " + e.getMessage());
                            } catch (IllegalStateException ise) {
                                log.warn("Emitter已完成，无法发送错误信息: {}", ise.getMessage());
                                isCompleted[0] = true;
                                break;
                            }
                        }
                    }
                    return null;
                } catch (Exception e) {
                    log.error("处理流响应时发生错误", e);
                    try {
                        chunkConsumer.accept("\n处理响应时出错: " + e.getMessage());
                    } catch (IllegalStateException ise) {
                        log.warn("Emitter已完成，无法发送错误信息: {}", ise.getMessage());
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("DeepSeek流式API调用失败", e);
            try {
                chunkConsumer.accept("调用DeepSeek API时发生错误: " + e.getMessage());
            } catch (IllegalStateException ise) {
                log.warn("Emitter已完成，无法发送错误信息: {}", ise.getMessage());
            }
        }
    }
} 