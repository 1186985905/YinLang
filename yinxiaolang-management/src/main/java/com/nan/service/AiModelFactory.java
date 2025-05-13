package com.nan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;

/**
 * AI模型工厂，管理所有可用的AI模型服务
 */
@Component
@RequiredArgsConstructor
public class AiModelFactory {
    
    private final List<AiModelService> aiModelServices;
    private final Map<String, AiModelService> aiModelServiceMap = new ConcurrentHashMap<>();
    
    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        for (AiModelService service : aiModelServices) {
            aiModelServiceMap.put(service.getModelType(), service);
        }
    }
    
    /**
     * 获取AI模型服务
     * @param modelType 模型类型
     * @return AI模型服务
     */
    public AiModelService getAiModelService(String modelType) {
        // 默认使用OpenAI
        if (modelType == null || modelType.isEmpty()) {
            modelType = "openai";
        }
        
        // 修正通义千问的模型类型标识
        if (modelType.equalsIgnoreCase("通义千问")) {
            modelType = "qianwen";
        }
        
        // 修正gpt-4o等OpenAI模型的类型标识
        if (modelType.toLowerCase().startsWith("gpt-")) {
            modelType = "openai";
        }
        
        AiModelService service = aiModelServiceMap.get(modelType);
        if (service == null) {
            throw new IllegalArgumentException("不支持的AI模型类型: " + modelType);
        }
        
        return service;
    }
    
    /**
     * 获取所有支持的模型类型
     * @return 模型类型列表
     */
    public List<String> getSupportedModelTypes() {
        return aiModelServiceMap.keySet().stream().toList();
    }
} 