package com.pocrd.api_publish_service.sdk.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocrd.api_publish_service.sdk.annotation.ApiGroup;
import com.pocrd.api_publish_service.sdk.apidefine.ApiMetadata;
import com.pocrd.api_publish_service.sdk.apidefine.ServiceDefinition;
import com.pocrd.api_publish_service.sdk.apidefine.ValidationResult;
import com.pocrd.api_publish_service.sdk.entity.ApiMetadataExtractException;
import com.pocrd.api_publish_service.sdk.util.ApiMetadataValidator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportInstance;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstanceCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ProviderModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * API 元数据服务配置自定义器
 * 
 * 职责：
 * 1. 为每个接口级别的元数据增加 api.md5.{interface} 属性
 * 2. 通过 MetadataReport 将增强的 FullServiceDefinition 存储到配置中心
 * 
 * 核心设计：单次扫描，多处使用
 * - 扫描接口元数据一次，同时用于计算接口 MD5 和构建各服务的 FullServiceDefinition
 * 
 * 这是一个 SPI 扩展，需要在 META-INF/dubbo 下配置
 */
@Activate
public class ApiMetadataServiceConfigurationCustomizer implements ServiceInstanceCustomizer {

    private static final Logger logger = LoggerFactory.getLogger(ApiMetadataServiceConfigurationCustomizer.class);
    
    private static final String METADATA_KEY_PREFIX = "api.md5.";
    private static final String CUSTOM_METADATA_PARAM_KEY = "api.metadata";
    
    // 使用 API 元数据专用的 ObjectMapper
    private static final ObjectMapper objectMapper = ApiMetadataObjectMapper.create();

    @Override
    public void customize(ServiceInstance serviceInstance, ApplicationModel applicationModel) {
        // 单次扫描收集所有服务的元数据
        List<ServiceDefinition> serviceDefs = scanMetadata(applicationModel);
        
        if (serviceDefs.isEmpty()) {
            logger.info("[METADATA] No business services found (skipping metadata storage)");
            return;
        }
        
        // 为每个服务生成一次 JSON，然后计算 MD5 并存储到 MetadataReport
        MetadataReport metadataReport = getMetadataReport(applicationModel);
        int successCount = 0;
        
        for (ServiceDefinition serviceDef : serviceDefs) {
            // 1. 生成 JSON（只生成一次）
            String jsonContent;
            try {
                ApiMetadata apiMetadata = new ApiMetadata(Collections.singletonList(serviceDef));
                jsonContent = objectMapper.writeValueAsString(apiMetadata);
            } catch (JsonProcessingException e) {
                logger.error("[METADATA] Failed to serialize metadata: {}", e.getMessage());
                continue;
            }
            
            // 2. 计算 MD5 并存入接口元数据
            String interfaceName = serviceDef.interfaceName();
            String md5Key = METADATA_KEY_PREFIX + interfaceName;
            String metadataMd5 = calculateMd5FromJson(jsonContent);
            serviceInstance.getMetadata().put(md5Key, metadataMd5);
            logger.info("[METADATA] API metadata MD5 for {}: {}", interfaceName, metadataMd5);
            
            // 3. 压缩并存储到 MetadataReport
            if (storeEnhancedMetadata(metadataReport, applicationModel, serviceDef, jsonContent, metadataMd5)) {
                successCount++;
            }
        }
        
        logger.info("[METADATA] Successfully stored {}/{} service definitions via MetadataReport", 
            successCount, serviceDefs.size());
    }
    
    /**
     * 单次扫描所有服务的元数据
     * 复用扫描结果，避免多次解析
     */
    private List<ServiceDefinition> scanMetadata(ApplicationModel applicationModel) {
        List<ServiceDefinition> result = new ArrayList<>();
        
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            ModuleServiceRepository serviceRepository = moduleModel.getServiceRepository();
            if (serviceRepository == null) {
                continue;
            }

            for (ProviderModel providerModel : serviceRepository.getExportedServices()) {
                Class<?> interfaceClass = providerModel.getServiceInterfaceClass();
                if (interfaceClass == null) {
                    continue;
                }

                // 只处理标记了@ApiGroup 的业务接口
                if (!interfaceClass.isAnnotationPresent(ApiGroup.class)) {
                    continue;
                }

                // 提取当前接口的完整元数据（包含 entities 和 errorCodes）
                try {
                    ApiMetadataValidator validator = new ApiMetadataValidator();
                    ValidationResult validationResult = validator.validate(interfaceClass);
                    
                    // 检查验证结果，如果有错误则打印日志并跳过该接口
                    if (validationResult.hasErrors()) {
                        logger.warn("[METADATA] 接口验证失败 [{}]: 发现 {} 个错误，跳过该接口", 
                            interfaceClass.getName(), validationResult.getErrorCount());
                        continue; // 跳过该接口，继续处理其他接口
                    }
                    
                    ServiceDefinition baseDef = validationResult.getServiceDefinition();
                    // 从 ProviderModel 获取 version 和 group 配置 (使用 getServiceMetadata() 替代已弃用的 getServiceConfig())
                    String version = providerModel.getServiceMetadata().getVersion();
                    String group = providerModel.getServiceMetadata().getGroup();
                    // 创建包含 version 和 group 的 ServiceDefinition
                    ServiceDefinition serviceDef = new ServiceDefinition(
                        baseDef.interfaceName(),
                        baseDef.apiGroup(),
                        baseDef.description(),
                        baseDef.methods(),
                        baseDef.entities(),
                        baseDef.errorCodes(),
                        version,
                        group
                    );
                    result.add(serviceDef);
                } catch (ApiMetadataExtractException e) {
                    logger.warn("[METADATA] 提取元数据失败 [{}]: {}", interfaceClass.getName(), e.getMessage());
                    // 跳过该接口，继续处理其他接口
                }
            }
        }
        
        return result;
    }
    
    /**
     * 存储增强的元数据到 MetadataReport
     */
    private boolean storeEnhancedMetadata(MetadataReport metadataReport, ApplicationModel applicationModel, 
                                          ServiceDefinition serviceDef, String jsonContent, String metadataMd5) {
        if (metadataReport == null) {
            return false;
        }
        
        try {
            // 从 ServiceDefinition 获取接口信息
            String interfaceName = serviceDef.interfaceName();
            Class<?> interfaceClass = Class.forName(interfaceName);
            
            // 使用 Dubbo 的 ServiceDefinitionBuilder 构建基础 FullServiceDefinition
            FullServiceDefinition fullServiceDef = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass);
            
            // 从 ServiceDefinition 获取 version 和 group（已从 ProviderModel 传递过来）
            String version = serviceDef.version();
            String group = serviceDef.group();
            
            // 构建 MetadataIdentifier
            // 使用 interfaceName 作为 serviceInterface
            MetadataIdentifier identifier = new MetadataIdentifier(
                interfaceName,
                version != null ? version + "-" + metadataMd5 : "-" + metadataMd5,
                group != null ? group : "",
                "provider",
                applicationModel.getApplicationName()
            );
            
            // 直接压缩已生成的 JSON
            String compressedContent = compressAndEncode(jsonContent);
            
            // 存入 FullServiceDefinition 的 parameters
            Map<String, String> parameters = fullServiceDef.getParameters();
            if (parameters == null) {
                parameters = new HashMap<>();
                fullServiceDef.setParameters(parameters);
            }
            parameters.put(CUSTOM_METADATA_PARAM_KEY, compressedContent);
            
            // 存储到 MetadataReport
            metadataReport.storeProviderMetadata(identifier, fullServiceDef);
            
            logger.info("[METADATA] Stored enhanced metadata for: {} (size: {} bytes)", 
                interfaceName, compressedContent.length());
            return true;
            
        } catch (Exception e) {
            logger.error("[METADATA] Failed to store metadata for service: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取 MetadataReport 实例
     */
    private MetadataReport getMetadataReport(ApplicationModel applicationModel) {
        try {
            MetadataReportInstance reportInstance = applicationModel.getBeanFactory().getBean(MetadataReportInstance.class);
            if (reportInstance != null && reportInstance.isInitialized()) {
                return reportInstance.getMetadataReport(null);
            }
        } catch (Exception e) {
            logger.warn("[METADATA] Error getting MetadataReport from BeanFactory: {}", e.getMessage());
        }
        
        try {
            ExtensionLoader<MetadataReport> loader = applicationModel.getExtensionLoader(MetadataReport.class);
            if (loader != null) {
                return loader.getDefaultExtension();
            }
        } catch (Exception e) {
            logger.warn("[METADATA] Error getting MetadataReport from ExtensionLoader: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 使用 GZIP 压缩并 Base64 编码
     */
    private String compressAndEncode(String data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(data.getBytes(StandardCharsets.UTF_8));
            }
            byte[] compressed = baos.toByteArray();
            return Base64.getEncoder().encodeToString(compressed);
        } catch (IOException e) {
            logger.warn("[METADATA] Failed to compress metadata: {}", e.getMessage());
            return data;
        }
    }
    
    /**
     * 从 JSON 字符串计算 MD5 值
     */
    private String calculateMd5FromJson(String jsonContent) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(jsonContent.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 8 && i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("[METADATA] MD5 algorithm not available: {}", e.getMessage());
            return "error";
        }
    }
}
