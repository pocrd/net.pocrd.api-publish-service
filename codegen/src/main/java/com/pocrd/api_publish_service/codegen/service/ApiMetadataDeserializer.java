package com.pocrd.api_publish_service.codegen.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocrd.api_publish_service.sdk.apidefine.ApiMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API 元数据反序列化工具
 */
public class ApiMetadataDeserializer {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiMetadataDeserializer.class);
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 将 JSON 字符串反序列化为 ApiMetadata 对象
     * 
     * @param json JSON 字符串
     * @return ApiMetadata 对象
     */
    public static ApiMetadata deserialize(String json) {
        try {
            return objectMapper.readValue(json, ApiMetadata.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize API metadata", e);
            throw new RuntimeException("Failed to deserialize API metadata", e);
        }
    }
}
