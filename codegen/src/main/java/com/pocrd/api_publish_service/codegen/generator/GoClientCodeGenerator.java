package com.pocrd.api_publish_service.codegen.generator;

import com.pocrd.api_publish_service.sdk.apidefine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Go 客户端代码生成器
 */
public class GoClientCodeGenerator extends AbstractCodeGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(GoClientCodeGenerator.class);
    
    private String moduleName;
    
    public GoClientCodeGenerator(String moduleName) {
        this.moduleName = moduleName;
    }
    
    @Override
    public String getLanguage() {
        return "go";
    }
    
    @Override
    public void generate(ApiMetadata metadata, String outputDir) {
        try {
            Path outPath = ensureOutputDir(outputDir);
            
            // 生成 go.mod
            generateGoMod(outPath);
            
            // 生成服务接口
            if (metadata.services() != null) {
                for (ServiceDefinition service : metadata.services()) {
                    generateServiceInterface(service, outPath);
                    
                    // 生成实体类（从每个服务中获取）
                    if (service.entities() != null) {
                        for (var entity : service.entities().entrySet()) {
                            generateEntityStruct(entity.getKey(), entity.getValue(), outPath);
                        }
                    }
                }
            }
            
            logger.info("Go client code generation completed");
            
        } catch (IOException e) {
            logger.error("Failed to generate Go client code", e);
            throw new RuntimeException("Failed to generate Go client code", e);
        }
    }
    
    /**
     * 生成 go.mod 文件
     */
    private void generateGoMod(Path outputDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("module ").append(moduleName).append("\n\n");
        sb.append("go 1.21\n");
        
        writeFile(outputDir, "go.mod", sb.toString());
    }
    
    /**
     * 生成服务接口
     */
    private void generateServiceInterface(ServiceDefinition service, Path outputDir) throws IOException {
        String interfaceName = extractSimpleClassName(service.interfaceName());
        
        StringBuilder sb = new StringBuilder();
        sb.append("package client\n\n");
        
        // 导入语句
        sb.append("import (\n");
        sb.append("    \"context\"\n");
        if (service.methods() != null) {
            for (MethodDefinition method : service.methods()) {
                String returnType = method.returnType();
                if (!returnType.startsWith("java.") && !returnType.equals("void")) {
                    sb.append("    \"").append(moduleName).append("/entity\"\n");
                    break;
                }
            }
        }
        sb.append(")\n\n");
        
        // 接口注释
        if (service.description() != null) {
            sb.append("// ").append(service.description()).append("\n");
        }
        
        // 接口声明
        sb.append("type ").append(interfaceName).append("Client interface {\n");
        
        // 方法声明
        if (service.methods() != null) {
            for (MethodDefinition method : service.methods()) {
                generateMethod(sb, method);
            }
        }
        
        sb.append("}\n");
        
        writeFile(outputDir, interfaceName.toLowerCase() + "_client.go", sb.toString());
    }
    
    /**
     * 生成方法定义
     */
    private void generateMethod(StringBuilder sb, MethodDefinition method) {
        // 方法注释
        if (method.description() != null) {
            sb.append("    // ").append(method.description()).append("\n");
        }
        
        // 构建参数列表
        StringBuilder params = new StringBuilder();
        params.append("ctx context.Context");
        
        if (method.parameters() != null) {
            for (ParameterDefinition param : method.parameters()) {
                if ("StreamObserver".equals(param.containerType())) {
                    continue; // 跳过流式参数
                }
                String typeName = mapTypeName(param.type());
                params.append(", ").append(toGoParamName(param.name())).append(" ").append(typeName);
            }
        }
        
        // 返回值
        String returnType;
        if (method.returnType().equals("void")) {
            returnType = "error";
        } else {
            returnType = "(" + mapTypeName(method.returnType()) + ", error)";
        }
        
        sb.append("    ").append(toGoMethodName(method.name())).append("(")
          .append(params).append(") ").append(returnType).append("\n");
    }
    
    /**
     * 生成实体类（Go struct）
     */
    private void generateEntityStruct(String className, EntityDefinition entity, Path outputDir) throws IOException {
        String simpleClassName = extractSimpleClassName(className);
        
        StringBuilder sb = new StringBuilder();
        sb.append("package entity\n\n");
        
        // 结构体注释
        sb.append("// ").append(simpleClassName).append(" represents a ").append(simpleClassName).append("\n");
        
        // 结构体声明
        sb.append("type ").append(simpleClassName).append(" struct {\n");
        
        // 字段
        if (entity.fields() != null) {
            for (FieldDefinition field : entity.fields()) {
                String typeName = mapTypeName(field.type());
                if ("array".equals(field.containerType())) {
                    typeName = "[]" + typeName;
                }
                
                // 字段名首字母大写（导出）
                String fieldName = capitalizeFirst(field.name());
                sb.append("    ").append(fieldName).append(" ").append(typeName);
                
                // JSON 标签
                sb.append(" `json:\"").append(field.name()).append("\"`");
                
                // 字段注释
                if (field.desc() != null) {
                    sb.append(" // ").append(field.desc());
                }
                
                sb.append("\n");
            }
        }
        
        sb.append("}\n");
        
        writeFile(outputDir.resolve("entity"), simpleClassName + ".go", sb.toString());
    }
    
    /**
     * 映射类型名称（Java 到 Go）
     */
    private String mapTypeName(String typeName) {
        if (typeName == null) {
            return "interface{}";
        }
        
        return switch (typeName) {
            case "java.lang.String" -> "string";
            case "java.lang.Integer", "int" -> "int";
            case "java.lang.Long", "long" -> "int64";
            case "java.lang.Boolean", "boolean" -> "bool";
            case "java.lang.Double", "double" -> "float64";
            case "java.lang.Float", "float" -> "float32";
            case "void" -> "";
            default -> {
                // 自定义类型
                yield extractSimpleClassName(typeName);
            }
        };
    }
    
    /**
     * 从全限定名提取简单类名
     */
    private String extractSimpleClassName(String qualifiedName) {
        if (qualifiedName == null) {
            return "Unknown";
        }
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
    
    /**
     * 转换为 Go 参数命名（驼峰转下划线）
     */
    private String toGoParamName(String paramName) {
        // 简单处理：直接返回原样或转为小写
        return paramName;
    }
    
    /**
     * 转换为 Go 方法命名
     */
    private String toGoMethodName(String methodName) {
        // 简单处理：首字母大写
        return capitalizeFirst(methodName);
    }
    
    /**
     * 首字母大写
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
