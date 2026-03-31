package com.pocrd.api_publish_service.codegen;

import com.pocrd.api_publish_service.codegen.cli.CommandLineOptions;
import com.pocrd.api_publish_service.codegen.generator.CodeGenerator;
import com.pocrd.api_publish_service.codegen.generator.CodeGeneratorFactory;
import com.pocrd.api_publish_service.codegen.generator.JavaClientCodeGenerator;
import com.pocrd.api_publish_service.codegen.generator.GoClientCodeGenerator;
import com.pocrd.api_publish_service.sdk.apidefine.ApiMetadata;
import com.pocrd.api_publish_service.codegen.service.NacosMetadataService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代码生成器主程序入口
 */
public class CodeGeneratorApp {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeGeneratorApp.class);
    
    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        
        try {
            // 解析命令行参数
            CommandLine cmd = parser.parse(CommandLineOptions.getOptions(), args);
            
            // 检查是否需要帮助
            if (cmd.hasOption("help")) {
                CommandLineOptions.printHelp();
                return;
            }
            
            // 获取参数
            String nacosAddr = cmd.getOptionValue("nacos-addr");
            String serviceName = cmd.getOptionValue("service-name");
            String groupName = cmd.getOptionValue("group-name", "PUBLIC-GROUP");
            String language = cmd.getOptionValue("language");
            String outputDir = cmd.getOptionValue("output-dir");
            
            logger.info("Starting code generation...");
            logger.info("Nacos Address: {}", nacosAddr);
            logger.info("Service Name: {}", serviceName);
            logger.info("Group Name: {}", groupName);
            logger.info("Target Language: {}", language);
            logger.info("Output Directory: {}", outputDir);
            
            // 创建或获取代码生成器
            CodeGenerator generator = createGenerator(language, cmd);
            
            if (generator == null) {
                System.err.println("Unsupported language: " + language);
                System.err.println("Supported languages: " + String.join(", ", CodeGeneratorFactory.getSupportedLanguages()));
                System.exit(1);
            }
            
            // 从 Nacos 获取元数据
            logger.info("Fetching metadata from Nacos...");
            try (NacosMetadataService nacosService = new NacosMetadataService(nacosAddr)) {
                ApiMetadata metadata = nacosService.getServiceMetadata(serviceName, groupName);
                
                if (metadata == null) {
                    System.err.println("No API metadata found for service: " + serviceName);
                    System.exit(1);
                }
                
                int entityCount = 0;
                if (metadata.services() != null) {
                    for (var service : metadata.services()) {
                        if (service.entities() != null) {
                            entityCount += service.entities().size();
                        }
                    }
                }
                logger.info("Successfully fetched metadata. Services: {}, Entities: {}", 
                    metadata.services() != null ? metadata.services().size() : 0,
                    entityCount);
                
                // 生成代码
                logger.info("Generating {} code...", language);
                generator.generate(metadata, outputDir);
                
                logger.info("Code generation completed successfully!");
                System.out.println("✓ Code generation completed successfully!");
                System.out.println("Output directory: " + outputDir);
            }
            
        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            CommandLineOptions.printHelp();
            System.exit(1);
        } catch (Exception e) {
            logger.error("Failed to generate code", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * 创建代码生成器
     */
    private static CodeGenerator createGenerator(String language, CommandLine cmd) {
        return switch (language.toLowerCase()) {
            case "java" -> {
                String javaPackage = cmd.getOptionValue("java-package", "com.example.client");
                yield new JavaClientCodeGenerator(javaPackage);
            }
            case "go" -> {
                String goModule = cmd.getOptionValue("go-module", "github.com/example/client");
                yield new GoClientCodeGenerator(goModule);
            }
            default -> CodeGeneratorFactory.getGenerator(language);
        };
    }
}
