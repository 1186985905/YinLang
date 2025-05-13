package com.nan.controller;

import com.nan.entity.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 图片生成控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController {

    private final RestTemplate restTemplate;
    
    @Value("${ai-models.openai.api-key:${OPENAI_API_KEY:}}")
    private String apiKey;
    
    @Value("${ai-models.openai.base-url:${OPENAI_BASE_URL:https://api.uiuihao.com/v1}}")
    private String baseUrl;
    
    /**
     * 生成图片
     * @param request 图片生成请求
     * @return 包含图片URL的响应
     */
    @PostMapping("/generate")
    public ApiResult<Map<String, Object>> generateImage(@RequestBody Map<String, Object> request) {
        try {
            log.info("收到图片生成请求: {}", request);
            
            // 构建请求URL
            String url = baseUrl + "/images/generations";
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 如果请求中没有指定模型，默认使用dall-e-3
            if (!request.containsKey("model")) {
                request.put("model", "dall-e-3");
            }
            
            // 如果请求中没有指定大小，默认使用1024x1024
            if (!request.containsKey("size")) {
                request.put("size", "1024x1024");
            }
            
            // 如果请求中没有指定质量，默认使用standard
            if (!request.containsKey("quality")) {
                request.put("quality", "standard");
            }
            
            // 如果请求中没有指定图片数量，默认为1
            if (!request.containsKey("n")) {
                request.put("n", 1);
            }
            
            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);
            
            // 解析响应
            Map responseBody = responseEntity.getBody();
            log.info("图片生成成功: {}", responseBody);
            
            return ApiResult.success(responseBody);
        } catch (Exception e) {
            log.error("图片生成失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "图片生成失败: " + e.getMessage(), errorResponse);
        }
    }
    
    /**
     * 图片变体生成
     * @param request 图片变体生成请求
     * @return 包含变体图片URL的响应
     */
    @PostMapping("/variations")
    public ApiResult<Map<String, Object>> createImageVariation(@RequestBody Map<String, Object> request) {
        try {
            log.info("收到图片变体生成请求: {}", request);
            
            // 构建请求URL
            String url = baseUrl + "/images/variations";
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            
            // 如果请求中没有指定模型，默认使用dall-e-2
            if (!request.containsKey("model")) {
                request.put("model", "dall-e-2");
            }
            
            // 如果请求中没有指定大小，默认使用1024x1024
            if (!request.containsKey("size")) {
                request.put("size", "1024x1024");
            }
            
            // 如果请求中没有指定图片数量，默认为1
            if (!request.containsKey("n")) {
                request.put("n", 1);
            }
            
            // 发送请求
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);
            
            // 解析响应
            Map responseBody = responseEntity.getBody();
            log.info("图片变体生成成功: {}", responseBody);
            
            return ApiResult.success(responseBody);
        } catch (Exception e) {
            log.error("图片变体生成失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "图片变体生成失败: " + e.getMessage(), errorResponse);
        }
    }
} 