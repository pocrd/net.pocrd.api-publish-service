package com.pocrd.api_publish_service.sdk.apidefine;

/**
 * 验证错误记录
 * 
 * @param interfaceName 接口全限定名
 * @param methodName 方法名（可为null）
 * @param parameterName 参数名（可为null）
 * @param entityName 实体类名（可为null）
 * @param fieldName 字段名（可为null）
 * @param errorType 错误类型
 * @param errorMessage 错误详情
 */
public record ValidationError(
    String interfaceName,
    String methodName,
    String parameterName,
    String entityName,
    String fieldName,
    ErrorType errorType,
    String errorMessage
) {
    public enum ErrorType {
        MISSING_DESCRIPTION("缺少@Description注解"),
        MISSING_API_PARAMETER("缺少@ApiParameter注解"),
        INVALID_ERROR_CODE("错误码无效"),
        UNSUPPORTED_TYPE("不支持的类型"),
        UNSUPPORTED_CONTAINER("不支持的容器类型"),
        ENTITY_NAME_CONFLICT("实体类名冲突"),
        NON_RECORD_ENTITY("实体类必须使用record类型"),
        INVALID_CODE_DEFINE("codeDefine必须继承AbstractReturnCode"),
        OTHER("其他错误");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 格式化输出错误信息
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorType.getDescription()).append("] ");
        sb.append("接口: ").append(interfaceName);
        
        if (methodName != null && !methodName.isEmpty()) {
            sb.append(" | 方法: ").append(methodName);
        }
        if (parameterName != null && !parameterName.isEmpty()) {
            sb.append(" | 参数: ").append(parameterName);
        }
        if (entityName != null && !entityName.isEmpty()) {
            sb.append(" | 实体: ").append(entityName);
        }
        if (fieldName != null && !fieldName.isEmpty()) {
            sb.append(" | 字段: ").append(fieldName);
        }
        sb.append(" | ").append(errorMessage);
        
        return sb.toString();
    }
}
