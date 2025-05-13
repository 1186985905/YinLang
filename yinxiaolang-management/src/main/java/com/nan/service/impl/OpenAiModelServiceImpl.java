package com.nan.service.impl;

import com.nan.service.AiModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * OpenAI模型服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiModelServiceImpl implements AiModelService {

    private final RestTemplate restTemplate;
    
    @Value("${ai-models.openai.api-key:${OPENAI_API_KEY:}}")
    private String apiKey;
    
    @Value("${ai-models.openai.base-url:${OPENAI_BASE_URL:https://api.uiuihao.com/v1}}")
    private String baseUrl;
    
    @Value("${ai-models.openai.model:${OPENAI_MODEL:gpt-4o-all}}")
    private String modelName;
    
    private static final String MODEL_TYPE = "openai";
    
    private static final String SYSTEM_PROMPT = """
            你是茵浪AI助手，一个功能强大、友好的AI助手。
            你的任务是以简洁、专业、友好的方式回答用户的问题。
            请用中文回答用户的问题。
            重要提示：不要在回答末尾使用多余的标点符号，特别是不要使用连续的问号。
            """;

    @Override
    public String generateResponse(String prompt) {
        try {
            // 构建请求URL
            String url = baseUrl + "/chat/completions";
            log.info("OpenAI API请求URL: {}", url);
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 构建消息
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // 添加系统提示
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);
            messages.add(systemMessage);
            
            // 添加当前用户消息，检查是否包含图片URL
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            
            // 检查是否包含图片URL
            if (containsImageUrl(prompt)) {
                // 提取图片URL
                String imageUrl = extractImageUrl(prompt);
                
                // 从提示中提取文本部分，移除图片URL和"分析这张图片:"等前缀
                String textContent = prompt.replace(imageUrl, "").trim();
                if (textContent.toLowerCase().startsWith("分析这张图片:") || 
                    textContent.toLowerCase().startsWith("分析这张图片")) {
                    textContent = textContent.substring("分析这张图片:".length()).trim();
                }
                
                log.info("检测到图片URL: {}", imageUrl);
                
                // 准备多模态内容列表
                List<Map<String, Object>> contentList = new ArrayList<>();
                
                // 添加文本部分
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("type", "text");
                textPart.put("text", textContent.isEmpty() ? "这张图片里有什么?" : textContent);
                contentList.add(textPart);
                
                // 处理本地图片URL
                if (imageUrl.contains("localhost")) {
                    // 从URL中提取文件名
                    String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                    // 构建文件路径
                    String uploadPath = "uploads"; // 与FileController中保持一致
                    Path filePath = Paths.get(uploadPath, filename);
                    
                    log.info("尝试读取本地图片: {}, 文件存在: {}", filePath.toAbsolutePath(), Files.exists(filePath));
                    
                    // 检查文件是否存在
                    if (!Files.exists(filePath)) {
                        log.error("本地图片文件不存在: {}", filePath);
                        return "无法加载图片: " + imageUrl;
                    }
                    
                    // 读取文件内容并转换为Base64
                    byte[] fileContent = Files.readAllBytes(filePath);
                    String base64Image = Base64.getEncoder().encodeToString(fileContent);
                    String mimeType = getMimeType(filename);
                    
                    // 构建data URI
                    String dataUri = "data:" + mimeType + ";base64," + base64Image;
                    
                    // 添加图片部分 - 使用base64格式
                    Map<String, Object> imagePart = new HashMap<>();
                    imagePart.put("type", "image_url");
                    
                    Map<String, String> imageUrlMap = new HashMap<>();
                    imageUrlMap.put("url", dataUri);
                    
                    imagePart.put("image_url", imageUrlMap);
                    contentList.add(imagePart);
                    
                    // 设置多模态内容
                    userMessage.put("content", contentList);
                } else {
                    // 添加外部图片URL
                    Map<String, Object> imagePart = new HashMap<>();
                    imagePart.put("type", "image_url");
                    
                    Map<String, String> imageUrlMap = new HashMap<>();
                    imageUrlMap.put("url", imageUrl);
                    
                    imagePart.put("image_url", imageUrlMap);
                    contentList.add(imagePart);
                    
                    // 设置多模态内容
                    userMessage.put("content", contentList);
                }
            } else {
                // 普通文本消息
            userMessage.put("content", prompt);
            }
            
            messages.add(userMessage);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            // 记录请求体信息（但不包含图片数据）
            ObjectMapper debugMapper = new ObjectMapper();
            Map<String, Object> logRequestBody = new HashMap<>(requestBody);
            if (logRequestBody.containsKey("messages")) {
                List<Map<String, Object>> logMessages = new ArrayList<>();
                for (Map<String, Object> msg : (List<Map<String, Object>>) logRequestBody.get("messages")) {
                    Map<String, Object> logMsg = new HashMap<>(msg);
                    if (logMsg.get("role").equals("user") && logMsg.get("content") instanceof List) {
                        logMsg.put("content", "[内含图片数据，日志中省略]");
                    }
                    logMessages.add(logMsg);
                }
                logRequestBody.put("messages", logMessages);
            }
            log.info("请求体: {}", debugMapper.writeValueAsString(logRequestBody));
            
            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);
            
            // 解析响应
            Map responseBody = responseEntity.getBody();
            if (responseBody != null && responseBody.containsKey("choices") && ((List) responseBody.get("choices")).size() > 0) {
                Map choice = (Map) ((List) responseBody.get("choices")).get(0);
                if (choice.containsKey("message")) {
                    Map message = (Map) choice.get("message");
                    if (message.containsKey("content")) {
                        String content = (String) message.get("content");
                        return cleanResponseText(content);
                    }
                }
            }
            
            return "无法生成响应，请稍后再试";
        } catch (Exception e) {
            log.error("OpenAI API请求失败", e);
            return "请求模型API失败: " + e.getMessage();
        }
    }
    
    @Override
    public void generateStreamResponse(String prompt, Consumer<String> chunkConsumer) {
        try {
            // 构建请求URL
            String url = baseUrl + "/chat/completions";
            log.info("OpenAI流式API请求URL: {}, 模型: {}", url, modelName);
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 检查这是否是一个图片分析请求
            boolean isImageAnalysisRequest = prompt.contains("http://localhost") && 
                                           prompt.contains("/uploads/") && 
                                           (prompt.contains(".jpg") || 
                                            prompt.contains(".jpeg") || 
                                            prompt.contains(".png") || 
                                            prompt.contains(".gif") || 
                                            prompt.contains(".bmp"));
            
            if (isImageAnalysisRequest) {
                log.info("检测到图片分析请求");
                
                // 提取图片URL
                String imageUrl = extractImageUrl(prompt);
                if (imageUrl.isEmpty()) {
                    log.error("无法从提示中提取图片URL: {}", prompt);
                    chunkConsumer.accept("无法识别图片URL，请重新上传");
                    return;
                }
                
                log.info("成功提取图片URL: {}", imageUrl);
                
                // 从提示中提取文本部分
                String textContent = prompt.replace(imageUrl, "").trim();
                if (textContent.toLowerCase().startsWith("分析这张图片:") || 
                    textContent.toLowerCase().startsWith("分析这张图片")) {
                    textContent = textContent.substring("分析这张图片:".length()).trim();
                }
                
                // 如果是这样包装的图片分析请求，进一步清理
                if (textContent.startsWith("这是一个图片分析请求")) {
                    textContent = "这张图片里有什么?";
                }
                
                // 处理本地图片
                if (imageUrl.contains("localhost")) {
                    try {
                        // 从URL中提取文件名
                        String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                        // 构建文件路径
                        String uploadPath = "uploads"; // 与FileController中保持一致
                        java.nio.file.Path filePath = java.nio.file.Paths.get(uploadPath, filename);
                        
                        log.info("尝试读取本地图片: {}, 文件存在: {}", filePath.toAbsolutePath(), java.nio.file.Files.exists(filePath));
                        
                        if (!java.nio.file.Files.exists(filePath)) {
                            log.error("本地图片文件不存在: {}", filePath);
                            chunkConsumer.accept("图片文件不存在，请重新上传");
                            return;
                        }
                        
                        try {
                            // 读取文件内容并检查大小
                            byte[] fileContent = java.nio.file.Files.readAllBytes(filePath);
                            
                            if (fileContent.length == 0) {
                                log.error("图片文件为空: {}", filePath);
                                chunkConsumer.accept("图片文件为空，请重新上传");
                                return;
                            }
                            
                            // 转换为Base64
                            String base64Image = java.util.Base64.getEncoder().encodeToString(fileContent);
                            String mimeType = getMimeType(filename);
                            
                            log.info("文件大小: {} 字节, MIME类型: {}, Base64长度: {}", 
                                    fileContent.length, mimeType, base64Image.length());
                            
                            if (base64Image.isEmpty()) {
                                log.error("Base64编码结果为空");
                                chunkConsumer.accept("图片编码失败，请重新上传");
                                return;
                            }
                            
                            // 构造多模态内容请求
                            List<Map<String, Object>> messages = new ArrayList<>();
                            
                            // 添加系统消息
                            Map<String, Object> systemMessage = new HashMap<>();
                            systemMessage.put("role", "system");
                            systemMessage.put("content", SYSTEM_PROMPT);
                            messages.add(systemMessage);
                            
                            // 添加用户消息（包含图片）
                            Map<String, Object> userMessage = new HashMap<>();
                            userMessage.put("role", "user");
                            
                            // 创建多模态内容
                            List<Map<String, Object>> contentList = new ArrayList<>();
                            
                            // 添加文本部分
                            Map<String, Object> textPart = new HashMap<>();
                            textPart.put("type", "text");
                            textPart.put("text", textContent.isEmpty() ? "这张图片里有什么?" : textContent);
                            contentList.add(textPart);
                            
                            // 添加图片部分
                            Map<String, Object> imagePart = new HashMap<>();
                            imagePart.put("type", "image_url");
                            
                            Map<String, String> imageUrlMap = new HashMap<>();
                            String dataUri = "data:" + mimeType + ";base64," + base64Image;
                            imageUrlMap.put("url", dataUri);
                            
                            imagePart.put("image_url", imageUrlMap);
                            contentList.add(imagePart);
                            
                            userMessage.put("content", contentList);
                            messages.add(userMessage);
                            
                            // 构建请求体
                            Map<String, Object> requestBody = new HashMap<>();
                            requestBody.put("model", modelName);
                            requestBody.put("messages", messages);
                            requestBody.put("temperature", 0.7);
                            requestBody.put("max_tokens", 2000);
                            requestBody.put("stream", true);  // 启用流式输出
                            
                            // 记录请求体 (不包含base64数据)
                            ObjectMapper debugMapper = new ObjectMapper();
                            Map<String, Object> logRequestBody = new HashMap<>(requestBody);
                            if (logRequestBody.containsKey("messages")) {
                                List<Map<String, Object>> logMessages = new ArrayList<>();
                                for (Map<String, Object> msg : (List<Map<String, Object>>) logRequestBody.get("messages")) {
                                    Map<String, Object> logMsg = new HashMap<>(msg);
                                    if (logMsg.get("role").equals("user")) {
                                        logMsg.put("content", "[内含图片数据，日志中省略]");
                                    }
                                    logMessages.add(logMsg);
                                }
                                logRequestBody.put("messages", logMessages);
                            }
                            log.info("图片分析请求体: {}", debugMapper.writeValueAsString(logRequestBody));
                            
                            // 创建HTTP请求实体
                            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
                            
                            // 定义响应提取器
                            restTemplate.execute(url, HttpMethod.POST, req -> {
                                req.getHeaders().putAll(requestEntity.getHeaders());
                                ObjectMapper objectMapper = new ObjectMapper();
                                objectMapper.writeValue(req.getBody(), requestEntity.getBody());
                            }, res -> {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.getBody(), "UTF-8"))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
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
                                                    chunkConsumer.accept(content);
                                                }
                                            }
                                        } catch (Exception e) {
                                            log.error("解析流式响应失败", e);
                                        }
                                    }
                                } catch (IOException e) {
                                    log.error("读取流式响应失败", e);
                                }
                                return null;
                            });
                            
                            return; // 成功处理了图片请求，直接返回
                            
                        } catch (IOException e) {
                            log.error("读取或处理图片文件失败", e);
                            chunkConsumer.accept("处理图片文件时出错: " + e.getMessage());
                            return;
                        }
                    } catch (Exception e) {
                        log.error("处理本地图片URL失败", e);
                        chunkConsumer.accept("处理图片URL时出错: " + e.getMessage());
                        return;
                    }
                } else {
                    log.info("非本地图片URL，直接使用原始URL");
                }
            }
            
            // 以下是非图片处理的原有逻辑 (如果是图片请求，上面的代码会先处理并返回)
            // 解析用户消息，检查是否为图像生成指令或图像分析指令
            if ((prompt.toLowerCase().contains("画") || 
                prompt.toLowerCase().contains("生成图片") || 
                prompt.toLowerCase().contains("创建图像") ||
                prompt.toLowerCase().contains("绘制") ||
                prompt.toLowerCase().contains("生成一张")) &&
                !prompt.toLowerCase().contains("分析这张图片") &&
                !prompt.toLowerCase().contains("分析图片") &&
                !prompt.toLowerCase().contains("识别图片") &&
                !prompt.toLowerCase().contains("描述图片") &&
                !prompt.toLowerCase().contains("看这张图")) {
                
                generateImageAndReturnUrl(prompt, chunkConsumer);
                return;
            }
            
            // 构建消息
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // 添加系统提示
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);
            messages.add(systemMessage);
            
            // 添加当前用户消息，检查是否包含图片URL
            Map<String, Object> currentUserMessage = new HashMap<>();
            currentUserMessage.put("role", "user");
            
            // 检查是否包含图片URL
            if (containsImageUrl(prompt)) {
                // 提取图片URL
                String imageUrl = extractImageUrl(prompt);
                
                // 从提示中提取文本部分，移除图片URL和"分析这张图片:"等前缀
                String textContent = prompt.replace(imageUrl, "").trim();
                if (textContent.toLowerCase().startsWith("分析这张图片:") || 
                    textContent.toLowerCase().startsWith("分析这张图片")) {
                    textContent = textContent.substring("分析这张图片:".length()).trim();
                }
                
                log.info("检测到图片URL: {}", imageUrl);
                
                // 准备多模态内容列表
                List<Map<String, Object>> contentList = new ArrayList<>();
                
                // 添加文本部分
                Map<String, Object> textPart = new HashMap<>();
                textPart.put("type", "text");
                textPart.put("text", textContent.isEmpty() ? "这张图片里有什么?" : textContent);
                contentList.add(textPart);
                
                // 处理本地图片URL，进行Base64转换
                if (imageUrl.contains("localhost")) {
                    try {
                        // 从URL中提取文件名
                        String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                        // 构建文件路径
                        String uploadPath = "uploads"; // 与FileController中保持一致
                        java.nio.file.Path filePath = java.nio.file.Paths.get(uploadPath, filename);
                        
                        log.info("尝试读取本地图片: {}, 文件存在: {}", filePath.toAbsolutePath(), java.nio.file.Files.exists(filePath));
                        
                        // 检查文件是否存在
                        if (!java.nio.file.Files.exists(filePath)) {
                            log.error("本地图片文件不存在: {}", filePath);
                            currentUserMessage.put("content", "无法加载图片: " + imageUrl);
                        } else {
                            try {
                                // 读取文件内容并转换为Base64
                                byte[] fileContent = java.nio.file.Files.readAllBytes(filePath);
                                
                                if (fileContent.length == 0) {
                                    log.error("图片文件为空: {}", filePath);
                                    currentUserMessage.put("content", "图片文件为空");
                                    return; // 避免继续处理
                                }
                                
                                String base64Image = java.util.Base64.getEncoder().encodeToString(fileContent);
                                String mimeType = getMimeType(filename);
                                
                                // 记录详细调试信息
                                log.info("文件大小: {} 字节", fileContent.length);
                                log.info("MIME类型: {}", mimeType);
                                log.info("Base64长度: {}", base64Image.length());
                                
                                if (base64Image.length() == 0) {
                                    log.error("Base64编码结果为空");
                                    currentUserMessage.put("content", "图片编码失败");
                                    return; // 避免继续处理
                                }
                                
                                // 构建data URI
                                String dataUri = "data:" + mimeType + ";base64," + base64Image;
                                
                                // 添加图片部分 - 使用base64格式
                                Map<String, Object> imagePart = new HashMap<>();
                                imagePart.put("type", "image_url");
                                
                                Map<String, String> imageUrlMap = new HashMap<>();
                                imageUrlMap.put("url", dataUri);
                                
                                imagePart.put("image_url", imageUrlMap);
                                contentList.add(imagePart);
                                
                                // 设置多模态内容
                                currentUserMessage.put("content", contentList);
                                log.info("成功转换本地图片为Base64格式：前50字符 = {}", dataUri.substring(0, Math.min(50, dataUri.length())) + "...");
                            } catch (IOException e) {
                                log.error("读取图片文件失败: {}", e.getMessage(), e);
                                currentUserMessage.put("content", "读取图片失败: " + e.getMessage());
                                return; // 避免继续处理
                            } catch (IllegalArgumentException e) {
                                log.error("Base64编码失败: {}", e.getMessage(), e);
                                currentUserMessage.put("content", "图片编码失败: " + e.getMessage());
                                return; // 避免继续处理
                            } catch (Exception e) {
                                log.error("处理图片时发生未知错误: {}", e.getMessage(), e);
                                currentUserMessage.put("content", "处理图片失败: " + e.getMessage());
                                return; // 避免继续处理
                            }
                        }
                    } catch (Exception e) {
                        log.error("处理本地图片失败", e);
                        currentUserMessage.put("content", "处理图片失败: " + e.getMessage());
                        return; // 避免继续处理
                    }
                } else {
                    // 外部URL直接使用
                    Map<String, Object> imagePart = new HashMap<>();
                    imagePart.put("type", "image_url");
                    
                    Map<String, String> imageUrlMap = new HashMap<>();
                    imageUrlMap.put("url", imageUrl);
                    
                    imagePart.put("image_url", imageUrlMap);
                    contentList.add(imagePart);
                    
                    // 设置多模态内容
                    currentUserMessage.put("content", contentList);
                }
            } else {
                // 普通文本消息
                currentUserMessage.put("content", prompt);
            }
            
            messages.add(currentUserMessage);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", true);  // 启用流式输出
            
            // 添加调试日志 - 打印请求体
            try {
                ObjectMapper debugMapper = new ObjectMapper();
                log.info("OpenAI请求体: {}", debugMapper.writeValueAsString(requestBody));
            } catch (Exception e) {
                log.error("序列化请求体失败", e);
            }
            
            // 创建HTTP请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // 定义响应提取器
            restTemplate.execute(url, HttpMethod.POST, req -> {
                req.getHeaders().putAll(requestEntity.getHeaders());
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(req.getBody(), requestEntity.getBody());
            }, res -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.getBody(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
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
                                    chunkConsumer.accept(content);
                                }
                            }
                        } catch (Exception e) {
                            log.error("解析流式响应失败", e);
                        }
                    }
                } catch (IOException e) {
                    log.error("读取流式响应失败", e);
                }
                return null;
            });
        } catch (Exception e) {
            log.error("OpenAI流式API调用失败", e);
            // 如果流式API失败，回退到非流式API
            String response = generateResponse(prompt);
            
            // 模拟流式输出
            for (char c : response.toCharArray()) {
                chunkConsumer.accept(String.valueOf(c));
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String getModelType() {
        return MODEL_TYPE;
    }

    /**
     * 生成图片并返回URL
     * @param prompt 用户提示
     * @param chunkConsumer 流式响应消费者
     */
    private void generateImageAndReturnUrl(String prompt, Consumer<String> chunkConsumer) {
        try {
            // 构建请求URL
            String url = baseUrl + "/images/generations";
            
            // 提取用户实际的图片生成指令，不处理上下文信息
            String imagePrompt = prompt;
            
            // 限制提示词长度，防止请求体过大
            if (imagePrompt.length() > 300) {
                imagePrompt = imagePrompt.substring(0, 300);
            }
            
            log.info("图片生成提示词: {}", imagePrompt);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "dall-e-3");  // 使用dall-e-3模型生成图片
            requestBody.put("prompt", imagePrompt);
            requestBody.put("size", "1024x1024");  // 生成1024x1024分辨率的图片
            requestBody.put("quality", "standard");
            requestBody.put("n", 1);
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            log.info("发送图片生成请求: {}", requestBody);
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);
            log.info("收到图片生成响应状态: {}", responseEntity.getStatusCode());
            
            // 解析响应
            Map responseBody = responseEntity.getBody();
            if (responseBody != null && responseBody.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                if (!data.isEmpty()) {
                    String imageUrl = (String) data.get(0).get("url");
                    String imageId = "img-" + System.currentTimeMillis(); // 生成唯一ID用于前端DOM操作
                    
                    // 使用ObjectMapper创建正确的JSON字符串
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> jsonMap = new HashMap<>();
                    jsonMap.put("type", "image");
                    jsonMap.put("url", imageUrl);
                    jsonMap.put("id", imageId);
                    String jsonResponse = objectMapper.writeValueAsString(jsonMap);
                    
                    // 一次性发送完整的JSON响应
                    chunkConsumer.accept(jsonResponse);
                    return;
                }
            }
            
            // 处理失败情况
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("type", "error");
            errorMap.put("message", "图片生成失败，请稍后再试");
            String errorMessage = objectMapper.writeValueAsString(errorMap);
            chunkConsumer.accept(errorMessage);
        } catch (Exception e) {
            log.error("图片生成失败", e);
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("type", "error");
                errorMap.put("message", "生成图片时出错: " + e.getMessage());
                String errorMessage = objectMapper.writeValueAsString(errorMap);
                chunkConsumer.accept(errorMessage);
            } catch (Exception ex) {
                // 如果JSON序列化失败，发送简单的错误消息
                chunkConsumer.accept("{\"type\":\"error\",\"message\":\"生成图片失败\"}");
            }
        }
    }

    /**
     * 根据文件名获取MIME类型
     */
    private String getMimeType(String filename) {
        if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".gif")) {
            return "image/gif";
        } else if (filename.endsWith(".bmp")) {
            return "image/bmp";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * 检查消息是否包含图片URL
     */
    private boolean containsImageUrl(String content) {
        // 检查是否包含本地上传图片URL
        boolean containsLocalUrl = content.contains("http://localhost") && 
                                  (content.contains("/uploads/") || content.contains("/download/")) && 
                                  (content.contains(".jpg") || 
                                   content.contains(".jpeg") || 
                                   content.contains(".png") || 
                                   content.contains(".gif") || 
                                   content.contains(".bmp"));
        
        // 检查是否包含外部图片URL
        boolean containsExternalUrl = (content.contains("http://") || content.contains("https://")) && 
                                     (content.contains(".jpg") || 
                                      content.contains(".jpeg") || 
                                      content.contains(".png") || 
                                      content.contains(".gif") || 
                                      content.contains(".bmp"));
        
        return containsLocalUrl || containsExternalUrl;
    }

    /**
     * 从消息中提取图片URL
     */
    private String extractImageUrl(String content) {
        // 提取本地上传图片URL
        Pattern localPattern = Pattern.compile("(http://localhost:[0-9]+(/uploads/|/download/)[^\\s]+\\.(jpg|jpeg|png|gif|bmp))", Pattern.CASE_INSENSITIVE);
        Matcher localMatcher = localPattern.matcher(content);
        if (localMatcher.find()) {
            return localMatcher.group(1);
        }
        
        // 提取外部图片URL
        Pattern externalPattern = Pattern.compile("(https?://[^\\s]+\\.(jpg|jpeg|png|gif|bmp))", Pattern.CASE_INSENSITIVE);
        Matcher externalMatcher = externalPattern.matcher(content);
        if (externalMatcher.find()) {
            return externalMatcher.group(1);
        }
        
        return "";
    }
} 