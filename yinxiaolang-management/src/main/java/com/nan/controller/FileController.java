package com.nan.controller;

import com.nan.entity.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${file.upload.path:uploads}")
    private String uploadPath;
    
    @Value("${file.access.url:http://localhost:5000/uploads}")
    private String accessUrl;
    
    @Value("${ai-models.openai.api-key:${OPENAI_API_KEY:}}")
    private String openaiApiKey;

    /**
     * 上传文件并返回可访问的URL
     */
    @PostMapping("/upload")
    public ApiResult<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            log.info("接收文件上传请求: 文件名={}, 大小={}KB, 类型={}", 
                file.getOriginalFilename(), 
                file.getSize() / 1024, 
                file.getContentType());
            
            // 确保上传目录存在
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                log.info("创建上传目录: {}", uploadDir.getAbsolutePath());
                uploadDir.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + extension;
            
            log.info("保存文件: 原始名称={}, 新文件名={}, 目标路径={}", 
                originalFilename, 
                newFilename, 
                Paths.get(uploadPath, newFilename).toAbsolutePath());
            
            // 保存文件
            Path filePath = Paths.get(uploadPath, newFilename);
            Files.write(filePath, file.getBytes());
            
            // 验证文件是否成功保存
            if (!Files.exists(filePath)) {
                log.error("文件保存失败: 文件不存在: {}", filePath.toAbsolutePath());
                return ApiResult.error(500, "文件保存失败: 文件不存在");
            }
            
            // 检查文件大小是否正确
            long savedSize = Files.size(filePath);
            if (savedSize != file.getSize()) {
                log.warn("文件大小不匹配: 期望={}, 实际={}", file.getSize(), savedSize);
            }
            
            // 构建可访问的URL
            String fileUrl = accessUrl + "/" + newFilename;
            log.info("文件上传成功: URL={}", fileUrl);
            
            // 返回结果
            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", originalFilename);
            
            return ApiResult.success(result);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ApiResult.error(500, "文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文件上传过程中发生未知错误", e);
            return ApiResult.error(500, "文件上传发生未知错误: " + e.getMessage());
        }
    }
    
    /**
     * 上传并解析文档内容（支持.txt和.docx文件）
     */
    @PostMapping("/parse-document")
    public ApiResult<Map<String, Object>> parseDocument(@RequestParam("file") MultipartFile file) {
        try {
            // 确保上传目录存在
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 获取文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            String newFilename = UUID.randomUUID().toString() + extension;
            
            // 保存文件
            Path filePath = Paths.get(uploadPath, newFilename);
            Files.write(filePath, file.getBytes());
            
            // 提取文档内容
            String content = "";
            if (extension.equals(".txt")) {
                // 处理文本文件
                content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            } else if (extension.equals(".docx")) {
                // 处理Word文档
                try (FileInputStream fis = new FileInputStream(filePath.toFile());
                     XWPFDocument document = new XWPFDocument(fis);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    content = extractor.getText();
                }
            } else {
                return ApiResult.error(400, "不支持的文件类型，请上传.txt或.docx文件");
            }
            
            // 构建可访问的URL
            String fileUrl = accessUrl + "/" + newFilename;
            
            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", originalFilename);
            result.put("content", content);
            result.put("extension", extension);
            
            return ApiResult.success(result);
        } catch (IOException e) {
            log.error("文档解析失败", e);
            return ApiResult.error(500, "文档解析失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文档处理异常", e);
            return ApiResult.error(500, "文档处理异常: " + e.getMessage());
        }
    }
    
    /**
     * 将本地图片转换为Base64编码，通过代理服务器传递，解决无法被外部AI服务访问的问题
     */
    @PostMapping("/proxy-image")
    public ApiResult<Map<String, String>> proxyImage(@RequestBody Map<String, String> request) {
        try {
            String localUrl = request.get("url");
            log.info("接收图片代理请求: {}", localUrl);
            
            // 检查是否为本地URL
            if (localUrl != null && localUrl.startsWith("http://localhost")) {
                // 从URL中提取文件名
                String filename = localUrl.substring(localUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(uploadPath, filename);
                
                // 读取文件内容并转为Base64
                if (Files.exists(filePath)) {
                    byte[] fileContent = Files.readAllBytes(filePath);
                    String base64Image = Base64.getEncoder().encodeToString(fileContent);
                    
                    // 返回Base64编码的图片内容
                    Map<String, String> result = new HashMap<>();
                    result.put("base64_image", base64Image);
                    result.put("mime_type", getMimeType(filename));
                    return ApiResult.success(result);
                } else {
                    log.error("图片文件不存在: {}", filePath);
                    return ApiResult.error(404, "图片文件不存在");
                }
            } else {
                log.error("无效的本地URL: {}", localUrl);
                return ApiResult.error(400, "无效的图片URL");
            }
        } catch (Exception e) {
            log.error("图片代理处理失败", e);
            return ApiResult.error(500, "图片代理处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据文件URL获取文档内容
     */
    @PostMapping("/get-document-content")
    public ApiResult<Map<String, Object>> getDocumentContent(@RequestBody Map<String, String> request) {
        try {
            String localUrl = request.get("url");
            log.info("获取文档内容请求: {}", localUrl);
            
            // 检查是否为本地URL
            if (localUrl != null && localUrl.startsWith("http://localhost")) {
                // 从URL中提取文件名
                String filename = localUrl.substring(localUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(uploadPath, filename);
                
                if (!Files.exists(filePath)) {
                    log.error("文档文件不存在: {}", filePath);
                    return ApiResult.error(404, "文档文件不存在");
                }
                
                // 根据文件扩展名解析内容
                String content = "";
                String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
                
                if (extension.equals(".txt")) {
                    // 处理文本文件
                    content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                } else if (extension.equals(".docx")) {
                    // 处理Word文档
                    try (FileInputStream fis = new FileInputStream(filePath.toFile());
                         XWPFDocument document = new XWPFDocument(fis);
                         XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                        content = extractor.getText();
                    }
                } else {
                    return ApiResult.error(400, "不支持的文件类型，目前仅支持.txt或.docx文件");
                }
                
                // 返回文档内容
                Map<String, Object> result = new HashMap<>();
                result.put("content", content);
                result.put("filename", filename);
                result.put("extension", extension);
                
                return ApiResult.success(result);
            } else {
                log.error("无效的本地URL: {}", localUrl);
                return ApiResult.error(400, "无效的文档URL");
            }
        } catch (Exception e) {
            log.error("获取文档内容失败", e);
            return ApiResult.error(500, "获取文档内容失败: " + e.getMessage());
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
        } else if (filename.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (filename.endsWith(".txt")) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }
    
    /**
     * 配置静态资源访问
     */
    @Configuration
    public static class UploadConfig implements WebMvcConfigurer {
        
        @Value("${file.upload.path:uploads}")
        private String uploadPath;
        
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/uploads/**")
                   .addResourceLocations("file:" + uploadPath + "/");
        }
    }
} 