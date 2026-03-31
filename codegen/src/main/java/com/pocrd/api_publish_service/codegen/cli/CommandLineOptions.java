package com.pocrd.api_publish_service.codegen.cli;

import org.apache.commons.cli.*;

/**
 * 命令行选项配置
 */
public class CommandLineOptions {
    
    private static final Options options = new Options();
    
    static {
        // Nacos 服务器地址
        Option nacosAddr = Option.builder("n")
            .longOpt("nacos-addr")
            .hasArg()
            .argName("address")
            .desc("Nacos server address (e.g., localhost:8848)")
            .required(true)
            .build();
        options.addOption(nacosAddr);
        
        // 服务名称
        Option serviceName = Option.builder("s")
            .longOpt("service-name")
            .hasArg()
            .argName("name")
            .desc("Service name in Nacos")
            .required(true)
            .build();
        options.addOption(serviceName);
        
        // 组名
        Option groupName = Option.builder("g")
            .longOpt("group-name")
            .hasArg()
            .argName("group")
            .desc("Group name in Nacos (default: PUBLIC-GROUP)")
            .build();
        options.addOption(groupName);
        
        // 目标语言
        Option language = Option.builder("l")
            .longOpt("language")
            .hasArg()
            .argName("lang")
            .desc("Target programming language (java, go)")
            .required(true)
            .build();
        options.addOption(language);
        
        // 输出目录
        Option outputDir = Option.builder("o")
            .longOpt("output-dir")
            .hasArg()
            .argName("directory")
            .desc("Output directory for generated code")
            .required(true)
            .build();
        options.addOption(outputDir);
        
        // Java 包名（可选）
        Option javaPackage = Option.builder("p")
            .longOpt("java-package")
            .hasArg()
            .argName("package")
            .desc("Base package name for Java client (default: com.example.client)")
            .build();
        options.addOption(javaPackage);
        
        // Go 模块名（可选）
        Option goModule = Option.builder("m")
            .longOpt("go-module")
            .hasArg()
            .argName("module")
            .desc("Module name for Go client (default: github.com/example/client)")
            .build();
        options.addOption(goModule);
        
        // 帮助选项
        Option help = Option.builder("h")
            .longOpt("help")
            .desc("Show this help message")
            .build();
        options.addOption(help);
    }
    
    /**
     * 获取命令行选项
     * 
     * @return Options 对象
     */
    public static Options getOptions() {
        return options;
    }
    
    /**
     * 打印帮助信息
     */
    public static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("codegen", options, true);
    }
}
