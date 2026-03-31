package com.pocrd.api_publish_service.codegen.generator;

import com.pocrd.api_publish_service.sdk.apidefine.ApiMetadata;

/**
 * 代码生成器接口
 */
public interface CodeGenerator {
    
    /**
     * 获取目标语言名称
     * 
     * @return 语言名称，如 "java", "go", "python"
     */
    String getLanguage();
    
    /**
     * 生成客户端代码
     * 
     * @param metadata API 元数据
     * @param outputDir 输出目录
     */
    void generate(ApiMetadata metadata, String outputDir);
    
    /**
     * 生成服务接口代码
     * 
     * @param serviceMetadata 服务元数据
     * @param outputDir 输出目录
     */
    default void generateService(Object serviceMetadata, String outputDir) {
        // 默认实现，可选
    }
    
    /**
     * 生成实体类代码
     * 
     * @param entityMetadata 实体元数据
     * @param outputDir 输出目录
     */
    default void generateEntity(Object entityMetadata, String outputDir) {
        // 默认实现，可选
    }
}
