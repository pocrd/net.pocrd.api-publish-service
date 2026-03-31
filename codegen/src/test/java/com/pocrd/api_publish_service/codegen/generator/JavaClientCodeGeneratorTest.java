package com.pocrd.api_publish_service.codegen.generator;

import com.pocrd.api_publish_service.sdk.apidefine.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java 客户端代码生成器测试
 */
public class JavaClientCodeGeneratorTest {
    
    @Test
    public void testGenerateBasicCode() throws IOException {
        // 准备测试数据
        ApiMetadata metadata = createTestMetadata();
        
        // 创建临时输出目录
        Path tempDir = Files.createTempDirectory("codegen-test");
        
        try {
            // 执行生成
            JavaClientCodeGenerator generator = new JavaClientCodeGenerator("com.test.client");
            generator.generate(metadata, tempDir.toString());
            
            // 验证生成的文件
            Path clientDir = tempDir.resolve("com/test/client");
            Path entityDir = tempDir.resolve("com/test/entity");
            
            assertTrue(Files.exists(clientDir), "Client directory should exist");
            assertTrue(Files.exists(entityDir), "Entity directory should exist");
            
            // 检查服务接口文件
            Path serviceFile = clientDir.resolve("GreeterServiceClient.java");
            assertTrue(Files.exists(serviceFile), "Service interface file should exist");
            
            String content = Files.readString(serviceFile);
            assertTrue(content.contains("public interface GreeterServiceClient"), 
                "Should contain interface declaration");
            assertTrue(content.contains("CompletableFuture<String> greet"), 
                "Should contain greet method");
            
            // 检查实体类文件
            Path entityFile = entityDir.resolve("ApiInfo.java");
            assertTrue(Files.exists(entityFile), "Entity file should exist");
            
            String entityContent = Files.readString(entityFile);
            assertTrue(entityContent.contains("public record ApiInfo"), 
                "Should contain record declaration");
            
        } finally {
            // 清理临时文件
            deleteDirectory(tempDir.toFile());
        }
    }
    
    /**
     * 创建测试用的元数据
     */
    private ApiMetadata createTestMetadata() {
        // 创建方法定义
        MethodDefinition greetMethod = new MethodDefinition(
            "greet",
            "java.lang.String",
            "Simple greeting method",
            null,
            List.of(new ParameterDefinition(
                "name",
                "java.lang.String",
                null,
                true,
                null,
                null,
                "User name",
                "John",
                null,
                "Name parameter"
            ))
        );
        
        // 创建实体定义
        FieldDefinition field1 = new FieldDefinition("id", "java.lang.Long", null, "ID", null);
        FieldDefinition field2 = new FieldDefinition("name", "java.lang.String", null, "Name", null);
        EntityDefinition entity = new EntityDefinition(
            "com.pocrd.api_publish_service.api.entity.ApiInfo",
            "ApiInfo entity",
            true,
            List.of(field1, field2)
        );
        
        // 创建服务定义
        ServiceDefinition service = new ServiceDefinition(
            "com.pocrd.api_publish_service.api.GreeterService",
            null,
            "Greeter Service",
            List.of(greetMethod),
            java.util.Map.of("com.pocrd.api_publish_service.api.entity.ApiInfo", entity),
            null,
            null,
            null
        );
        
        return new ApiMetadata(
            List.of(service)
        );
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(java.io.File dir) {
        if (dir.isDirectory()) {
            for (java.io.File file : dir.listFiles()) {
                deleteDirectory(file);
            }
        }
        dir.delete();
    }
}
