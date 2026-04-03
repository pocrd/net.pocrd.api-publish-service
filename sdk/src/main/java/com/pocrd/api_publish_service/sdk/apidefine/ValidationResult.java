package com.pocrd.api_publish_service.sdk.apidefine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证结果
 */
public class ValidationResult {
    private static final Logger logger = LoggerFactory.getLogger(ValidationResult.class);

    private final List<ValidationError> errors = new ArrayList<>();
    private ServiceDefinition serviceDefinition;
    
    /**
     * 添加错误记录
     */
    public void addError(ValidationError error) {
        errors.add(error);
    }
    
    /**
     * 添加错误记录（便捷方法）
     */
    public void addError(String interfaceName, String methodName, String parameterName,
                         String entityName, String fieldName, 
                         ValidationError.ErrorType errorType, String errorMessage) {
        errors.add(new ValidationError(interfaceName, methodName, parameterName, 
                                       entityName, fieldName, errorType, errorMessage));
    }
    
    /**
     * 是否有错误
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * 获取错误数量
     */
    public int getErrorCount() {
        return errors.size();
    }
    
    /**
     * 获取所有错误
     */
    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * 设置服务定义
     */
    public void setServiceDefinition(ServiceDefinition serviceDefinition) {
        this.serviceDefinition = serviceDefinition;
    }
    
    /**
     * 获取服务定义
     */
    public ServiceDefinition getServiceDefinition() {
        return serviceDefinition;
    }
    
    /**
     * 打印所有错误信息
     */
    public void printErrors() {
        if (errors.isEmpty()) {
            logger.info("✓ 验证通过，未发现错误");
            return;
        }

        logger.error("✗ 发现 {} 个错误:", errors.size());

        int index = 1;
        for (ValidationError error : errors) {
            logger.error("{}. {}", index, error.format());
            index++;
        }
    }
    
    /**
     * 获取格式化的错误报告
     */
    public String getErrorReport() {
        if (errors.isEmpty()) {
            return "验证通过，未发现错误";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("发现 ").append(errors.size()).append(" 个错误:\n\n");
        
        int index = 1;
        for (ValidationError error : errors) {
            sb.append(index).append(". ").append(error.format()).append("\n");
            index++;
        }
        
        return sb.toString();
    }
}
