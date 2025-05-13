package com.nan.service.impl;

import com.nan.service.AiModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 通义千问模型服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QianwenModelServiceImpl implements AiModelService {

    private final RestTemplate restTemplate;
    
    @Value("${ai-models.qianwen.api-key:}")
    private String apiKey;
    
    @Value("${ai-models.qianwen.base-url:https://api.uiuihao.com/v1}")
    private String baseUrl;
    
    @Value("${ai-models.qianwen.model:qwen-max}")
    private String modelName;
    
    private static final String MODEL_TYPE = "qianwen";
    
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
            log.info("通义千问API请求URL: {}", url);
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");
            
            // 构建请求体 - 使用OpenAI格式API调用
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            
            // 添加系统提示
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);
            messages.add(systemMessage);
            
            // 添加当前用户消息
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            
            // 检查是否为文档内容，如果是，作为普通文本处理
            if (isDocumentContent(prompt)) {
                log.info("检测到文档内容，作为普通文本处理");
                userMessage.put("content", prompt);
            }
            // 检查是否包含图片URL
            else if (containsImageUrl(prompt)) {
                // 提取图片URL
                String imageUrl = extractImageUrl(prompt);
                
                // 从提示中提取文本部分，移除图片URL和"分析这张图片:"等前缀
                String textContent = prompt.replace(imageUrl, "").trim();
                if (textContent.toLowerCase().startsWith("分析这张图片:") || 
                    textContent.toLowerCase().startsWith("分析这张图片")) {
                    textContent = textContent.substring("分析这张图片:".length()).trim();
                }
                
                log.info("检测到图片URL: {}", imageUrl);
                
                // 处理本地图片URL
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
                            userMessage.put("content", "无法加载图片: " + imageUrl);
                        } else {
                            // 读取文件内容并转换为Base64
                            byte[] fileContent = java.nio.file.Files.readAllBytes(filePath);
                            
                            if (fileContent.length == 0) {
                                log.error("图片文件为空: {}", filePath);
                                userMessage.put("content", "图片文件为空");
                            } else {
                                String base64Image = java.util.Base64.getEncoder().encodeToString(fileContent);
                                String mimeType = getMimeType(filename);
                                
                                // 记录详细调试信息
                                log.info("文件大小: {} 字节", fileContent.length);
                                log.info("MIME类型: {}", mimeType);
                                log.info("Base64长度: {}", base64Image.length());
                                
                                if (base64Image.length() == 0) {
                                    log.error("Base64编码结果为空");
                                    userMessage.put("content", "图片编码失败");
                                } else {
                                    // 构建data URI并确保格式正确
                                    String dataUri = "data:" + mimeType + ";base64," + base64Image;
                                    
                                    // 使用多模态内容格式并指定base64格式的图片
                                    List<Map<String, Object>> contentList = new ArrayList<>();
                                    
                                    // 添加文本部分
                                    Map<String, Object> textPart = new HashMap<>();
                                    textPart.put("type", "text");
                                    textPart.put("text", textContent.isEmpty() ? "这张图片里有什么?" : textContent);
                                    contentList.add(textPart);
                                    
                                    // 添加图片部分 - 使用base64格式
                                    Map<String, Object> imagePart = new HashMap<>();
                                    imagePart.put("type", "image_url");
                                    
                                    Map<String, String> imageUrlMap = new HashMap<>();
                                    imageUrlMap.put("url", dataUri);
                                    
                                    imagePart.put("image_url", imageUrlMap);
                                    contentList.add(imagePart);
                                    
                                    // 设置多模态内容
                                    userMessage.put("content", contentList);
                                    log.info("成功转换本地图片为Base64格式：前50字符 = {}", dataUri.substring(0, Math.min(50, dataUri.length())) + "...");
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error("读取图片文件失败: {}", e.getMessage(), e);
                        userMessage.put("content", "读取图片失败: " + e.getMessage());
                    } catch (IllegalArgumentException e) {
                        log.error("Base64编码失败: {}", e.getMessage(), e);
                        userMessage.put("content", "图片编码失败: " + e.getMessage());
                    } catch (Exception e) {
                        log.error("处理图片时发生未知错误: {}", e.getMessage(), e);
                        userMessage.put("content", "处理图片失败: " + e.getMessage());
                    }
                } else {
                    // 外部URL使用标准多模态格式
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
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);
            
            // 解析响应 - OpenAI格式响应
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
            
            return "通义千问模型暂时无法响应，请稍后再试";
        } catch (Exception e) {
            log.error("通义千问API调用失败", e);
            return "调用通义千问API时发生错误: " + e.getMessage();
        }
    }
    
    @Override
    public void generateStreamResponse(String prompt, Consumer<String> chunkConsumer) {
        try {
            // 构建请求URL
            String url = baseUrl + "/chat/completions";
            log.info("通义千问流式API请求URL: {}", url);
            
            // 调试日志
            log.info("Qianwen收到请求: prompt长度:{}, 前100个字符:{}", 
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
            }
            
            // 正常处理非文档内容
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");
            
            // 构建请求体 - 使用OpenAI格式API调用
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            
            // 添加系统提示
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);
            messages.add(systemMessage);
            
            // 添加当前用户消息
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            
            // 检查是否为文档内容，如果是，作为普通文本处理
            if (isDocumentContent(prompt)) {
                log.info("检测到文档内容，作为普通文本流式处理");
                userMessage.put("content", prompt);
            }
            // 检查是否包含图片URL
            else if (containsImageUrl(prompt)) {
                log.info("检测到图片URL，以多模态格式处理");
                // 提取图片URL
                String imageUrl = extractImageUrl(prompt);
                
                // 从提示中提取文本部分，移除图片URL和"分析这张图片:"等前缀
                String textContent = prompt.replace(imageUrl, "").trim();
                if (textContent.toLowerCase().startsWith("分析这张图片:") || 
                    textContent.toLowerCase().startsWith("分析这张图片")) {
                    textContent = textContent.substring("分析这张图片:".length()).trim();
                }
                
                log.info("检测到图片URL: {}", imageUrl);
                
                // 处理本地图片URL
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
                            userMessage.put("content", "无法加载图片: " + imageUrl);
                        } else {
                            // 读取文件内容并转换为Base64
                            byte[] fileContent = java.nio.file.Files.readAllBytes(filePath);
                            
                            if (fileContent.length == 0) {
                                log.error("图片文件为空: {}", filePath);
                                userMessage.put("content", "图片文件为空");
                            } else {
                                String base64Image = java.util.Base64.getEncoder().encodeToString(fileContent);
                                String mimeType = getMimeType(filename);
                                
                                // 记录详细调试信息
                                log.info("文件大小: {} 字节", fileContent.length);
                                log.info("MIME类型: {}", mimeType);
                                log.info("Base64长度: {}", base64Image.length());
                                
                                if (base64Image.length() == 0) {
                                    log.error("Base64编码结果为空");
                                    userMessage.put("content", "图片编码失败");
                                } else {
                                    // 构建data URI并确保格式正确
                                    String dataUri = "data:" + mimeType + ";base64," + base64Image;
                                    
                                    // 使用多模态内容格式并指定base64格式的图片
                                    List<Map<String, Object>> contentList = new ArrayList<>();
                                    
                                    // 添加文本部分
                                    Map<String, Object> textPart = new HashMap<>();
                                    textPart.put("type", "text");
                                    textPart.put("text", textContent.isEmpty() ? "这张图片里有什么?" : textContent);
                                    contentList.add(textPart);
                                    
                                    // 添加图片部分 - 使用base64格式
                                    Map<String, Object> imagePart = new HashMap<>();
                                    imagePart.put("type", "image_url");
                                    
                                    Map<String, String> imageUrlMap = new HashMap<>();
                                    imageUrlMap.put("url", dataUri);
                                    
                                    imagePart.put("image_url", imageUrlMap);
                                    contentList.add(imagePart);
                                    
                                    // 设置多模态内容
                                    userMessage.put("content", contentList);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("处理本地图片失败: {}", e.getMessage());
                        userMessage.put("content", "处理图片失败: " + e.getMessage());
                    }
                } else {
                    // 外部URL使用标准多模态格式
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
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", true);  // 启用流式输出
            
            // 打印请求体以便调试（不包含敏感信息）
            ObjectMapper debugMapper = new ObjectMapper();
            log.debug("请求体: {}", debugMapper.writeValueAsString(requestBody));
            
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
            log.error("通义千问流式API调用失败", e);
            try {
                chunkConsumer.accept("调用通义千问API时发生错误: " + e.getMessage());
            } catch (IllegalStateException ise) {
                log.warn("Emitter已完成，无法发送错误信息: {}", ise.getMessage());
            }
        }
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
            log.info("Qianwen检测到文档内容: {}", text.length() > 100 ? text.substring(0, 100) + "..." : text);
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

    /**
     * 从文本中提取图片URL
     */
    private String extractImageUrl(String text) {
        if (text == null) return "";
        
        // 尝试匹配标准图片URL - 限制为图片扩展名
        Pattern pattern = Pattern.compile("https?://[^\\s]+(?:\\.jpg|\\.jpeg|\\.png|\\.gif|\\.bmp)");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        // 尝试匹配localhost上传图片URL - 限制为图片扩展名
        pattern = Pattern.compile("http://localhost:\\d+/uploads/[^\\s]+(?:\\.jpg|\\.jpeg|\\.png|\\.gif|\\.bmp)");
        matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            // 将localhost URL转换为Base64图片
            String localUrl = matcher.group();
            
            try {
                // 调用文件代理接口
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("url", localUrl);
                
                HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
                
                // 调用我们的图片代理服务将本地图片转换为Base64
                String proxyUrl = "http://localhost:5000/api/file/proxy-image";
                ResponseEntity<Map> responseEntity = restTemplate.postForEntity(proxyUrl, requestEntity, Map.class);
                
                // 如果成功获取Base64图片
                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> responseBody = responseEntity.getBody();
                    if (responseBody != null && responseBody.containsKey("data")) {
                        Map<String, String> data = (Map<String, String>) responseBody.get("data");
                        String base64Image = data.get("base64_image");
                        String mimeType = data.get("mime_type");
                        
                        // 返回data URI格式的图片
                        return "data:" + mimeType + ";base64," + base64Image;
                    }
                }
                
                log.error("无法通过代理处理本地图片URL: {}, 状态码: {}", localUrl, responseEntity.getStatusCode());
            } catch (Exception e) {
                log.error("图片代理处理失败", e);
            }
            
            // 如果代理失败，仍然返回原始URL，但可能会导致外部API无法访问
            log.warn("使用原始本地URL: {}", localUrl);
            return localUrl;
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
} 