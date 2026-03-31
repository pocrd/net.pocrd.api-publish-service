package com.pocrd.api_publish_service.sdk.entity;

/**
 * API 元数据提取异常
 */
public class ApiMetadataExtractException extends RuntimeException {
    
    public ApiMetadataExtractException(String message) {
        super(message);
    }
    
    public ApiMetadataExtractException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ApiMetadataExtractException(String interfaceName, String fieldName, String message) {
        super(String.format("[%s.%s] %s", interfaceName, fieldName, message));
    }
    
    public ApiMetadataExtractException(String interfaceName, String methodName, String paramName, String message) {
        super(String.format("[%s.%s(%s)] %s", interfaceName, methodName, paramName, message));
    }
}
