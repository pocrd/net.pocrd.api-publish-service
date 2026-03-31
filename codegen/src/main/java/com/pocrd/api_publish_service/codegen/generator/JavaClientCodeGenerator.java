package com.pocrd.api_publish_service.codegen.generator;

import com.pocrd.api_publish_service.sdk.apidefine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Java 客户端代码生成器
 */
public class JavaClientCodeGenerator extends AbstractCodeGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(JavaClientCodeGenerator.class);
    
    private String basePackage;
    
    public JavaClientCodeGenerator(String basePackage) {
        this.basePackage = basePackage;
    }
    
    @Override
    public String getLanguage() {
        return "java";
    }
    
    @Override
    public void generate(ApiMetadata metadata, String outputDir) {
        try {
            Path outPath = ensureOutputDir(outputDir);
            
            // 生成服务接口
            if (metadata.services() != null) {
                for (ServiceDefinition service : metadata.services()) {
                    generateServiceInterface(service, outPath);
                    
                    // 生成实体类（从每个服务中获取）
                    if (service.entities() != null) {
                        for (var entity : service.entities().entrySet()) {
                            generateEntityClass(entity.getKey(), entity.getValue(), outPath);
                        }
                    }
                }
            }
            
            logger.info("Java client code generation completed");
            
        } catch (IOException e) {
            logger.error("Failed to generate Java client code", e);
            throw new RuntimeException("Failed to generate Java client code", e);
        }
    }
    
    /**
     * 生成服务接口
     */
    private void generateServiceInterface(ServiceDefinition service, Path outputDir) throws IOException {
        String interfaceName = extractSimpleClassName(service.interfaceName());
        String packagePath = getPackagePath(basePackage) + "/client";
        Path dir = outputDir.resolve(packagePath);
        
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".client;\n\n");
        
        // 导入语句
        sb.append("import java.util.concurrent.CompletableFuture;\n");
        if (service.methods() != null) {
            for (MethodDefinition method : service.methods()) {
                String returnType = method.returnType();
                if (!returnType.startsWith("java.") && !returnType.equals("void")) {
                    sb.append("import ").append(basePackage).append(".entity.").append(extractSimpleClassName(returnType)).append(";\n");
                }
            }
        }
        sb.append("\n");
        
        // 接口注释
        if (service.description() != null) {
            sb.append("/**\n");
            sb.append(" * ").append(service.description()).append("\n");
            sb.append(" */\n");
        }
        
        // 接口声明
        sb.append("public interface ").append(interfaceName).append("Client {\n\n");
        
        // 方法声明
        if (service.methods() != null) {
            for (MethodDefinition method : service.methods()) {
                generateMethod(sb, method);
            }
        }
        
        sb.append("}\n");
        
        writeFile(dir, interfaceName + "Client.java", sb.toString());
    }
    
    /**
     * 生成方法定义
     */
    private void generateMethod(StringBuilder sb, MethodDefinition method) {
        // 方法注释
        if (method.description() != null) {
            sb.append("    /**\n");
            sb.append("     * ").append(method.description()).append("\n");
            if (method.errorCodes() != null) {
                sb.append("     * \n");
                sb.append("     * @throws Exception when error code: ");
                for (int code : method.errorCodes()) {
                    sb.append(code).append(", ");
                }
                sb.setLength(sb.length() - 2); // 移除最后的逗号和空格
                sb.append("\n");
            }
            sb.append("     */\n");
        }
        
        // 构建参数列表
        StringBuilder params = new StringBuilder();
        if (method.parameters() != null) {
            for (int i = 0; i < method.parameters().size(); i++) {
                ParameterDefinition param = method.parameters().get(i);
                if ("StreamObserver".equals(param.containerType())) {
                    continue; // 跳过流式参数，使用 CompletableFuture 处理
                }
                String typeName = mapTypeName(param.type());
                params.append(typeName).append(" ").append(param.name());
                if (i < method.parameters().size() - 1) {
                    params.append(", ");
                }
            }
        }
        
        // 返回值类型
        String returnType;
        if (method.returnType().equals("void")) {
            returnType = "CompletableFuture<Void>";
        } else {
            returnType = "CompletableFuture<" + mapTypeName(method.returnType()) + ">";
        }
        
        sb.append("    ").append(returnType).append(" ")
          .append(method.name()).append("(").append(params).append(");\n\n");
    }
    
    /**
     * 生成实体类
     */
    private void generateEntityClass(String className, EntityDefinition entity, Path outputDir) throws IOException {
        String simpleClassName = extractSimpleClassName(className);
        String packagePath = getPackagePath(basePackage) + "/entity";
        Path dir = outputDir.resolve(packagePath);
        
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".entity;\n\n");
        
        // 导入语句
        if (entity.fields() != null) {
            for (FieldDefinition field : entity.fields()) {
                String fieldType = field.type();
                if (fieldType != null && !fieldType.startsWith("java.") && !isPrimitiveType(fieldType)) {
                    sb.append("import ").append(basePackage).append(".entity.").append(extractSimpleClassName(fieldType)).append(";\n");
                }
            }
        }
        sb.append("\n");
        
        // 类注释
        sb.append("/**\n");
        sb.append(" * ").append(simpleClassName).append(" entity\n");
        sb.append(" */\n");
        
        // 类声明（使用 record）
        sb.append("public record ").append(simpleClassName).append("(\n");
        
        // 字段
        if (entity.fields() != null) {
            for (int i = 0; i < entity.fields().size(); i++) {
                FieldDefinition field = entity.fields().get(i);
                String typeName = mapTypeName(field.type());
                if ("array".equals(field.containerType())) {
                    typeName = typeName + "[]";
                }
                sb.append("    ").append(typeName).append(" ").append(field.name());
                
                // 字段注释
                if (field.desc() != null) {
                    sb.append(" // ").append(field.desc());
                }
                
                if (i < entity.fields().size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
        }
        
        sb.append(") {}\n");
        
        writeFile(dir, simpleClassName + ".java", sb.toString());
    }
    
    /**
     * 映射类型名称（处理基本类型和复杂类型）
     */
    private String mapTypeName(String typeName) {
        if (typeName == null) {
            return "Object";
        }
        
        // 基本类型映射
        return switch (typeName) {
            case "java.lang.String" -> "String";
            case "java.lang.Integer" -> "Integer";
            case "java.lang.Long" -> "Long";
            case "java.lang.Boolean" -> "Boolean";
            case "java.lang.Double" -> "Double";
            case "java.lang.Float" -> "Float";
            case "int" -> "int";
            case "long" -> "long";
            case "boolean" -> "boolean";
            case "double" -> "double";
            case "float" -> "float";
            case "void" -> "void";
            default -> {
                // 自定义类型，使用简单类名
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
     * 判断是否为基本类型
     */
    private boolean isPrimitiveType(String typeName) {
        return typeName.equals("int") || typeName.equals("long") || 
               typeName.equals("boolean") || typeName.equals("double") || 
               typeName.equals("float") || typeName.equals("void");
    }
}
