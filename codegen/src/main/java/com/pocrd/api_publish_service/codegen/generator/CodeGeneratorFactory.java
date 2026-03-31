package com.pocrd.api_publish_service.codegen.generator;

import java.util.HashMap;
import java.util.Map;

/**
 * 代码生成器工厂
 */
public class CodeGeneratorFactory {
    
    private static final Map<String, CodeGenerator> generators = new HashMap<>();
    
    static {
        // 注册默认的 Java 生成器（使用默认包名）
        registerGenerator("java", new JavaClientCodeGenerator("com.example.client"));
        
        // 注册默认的 Go 生成器（使用默认模块名）
        registerGenerator("go", new GoClientCodeGenerator("github.com/example/client"));
    }
    
    /**
     * 获取代码生成器
     * 
     * @param language 语言名称
     * @return 代码生成器实例
     */
    public static CodeGenerator getGenerator(String language) {
        return generators.get(language.toLowerCase());
    }
    
    /**
     * 注册代码生成器
     * 
     * @param language 语言名称
     * @param generator 生成器实例
     */
    public static void registerGenerator(String language, CodeGenerator generator) {
        generators.put(language.toLowerCase(), generator);
    }
    
    /**
     * 获取所有支持的语言
     * 
     * @return 语言列表
     */
    public static String[] getSupportedLanguages() {
        return generators.keySet().toArray(new String[0]);
    }
}
