package com.pocrd.api_publish_service.codegen.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 代码生成器抽象基类
 */
public abstract class AbstractCodeGenerator implements CodeGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractCodeGenerator.class);
    
    /**
     * 确保输出目录存在
     */
    protected Path ensureOutputDir(String outputDir) throws IOException {
        Path path = Path.of(outputDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("Created output directory: {}", outputDir);
        }
        return path;
    }
    
    /**
     * 写入文件内容
     */
    protected void writeFile(Path dir, String fileName, String content) throws IOException {
        File file = new File(dir.toFile(), fileName);
        
        // 如果父目录不存在，创建它
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
            logger.info("Generated file: {}", file.getAbsolutePath());
        }
    }
    
    /**
     * 获取包路径（用于 Java 等需要包结构的语言）
     */
    protected String getPackagePath(String packageName) {
        return packageName.replace('.', '/');
    }
}
