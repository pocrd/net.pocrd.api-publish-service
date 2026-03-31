package com.pocrd.api_publish_service.codegen.service;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.model.v2.ErrorCode;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.pocrd.api_publish_service.sdk.apidefine.ApiMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

/**
 * Nacos 服务发现与元数据读取工具
 */
public class NacosMetadataService implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(NacosMetadataService.class);
    
    private static final String METADATA_KEY = "api.metadata";
    private static final String CONFIG_CENTER_BASE_URL = "http://%s/nacos/v1/cs/configs?dataId=%s&group=%s";
    
    private final NamingService namingService;
    private final String nacosServerAddr;
    private final HttpClient httpClient;
    
    public NacosMetadataService(String serverAddr) throws NacosException {
        this.nacosServerAddr = serverAddr;
        this.namingService = NacosFactory.createNamingService(serverAddr);
        this.httpClient = HttpClient.newHttpClient();
    }
    
    /**
     * 从 Nacos 获取指定服务的元数据
     * 
     * @param serviceName 服务名称
     * @param groupName 组名
     * @return API 元数据，如果不存在则返回 null
     */
    public ApiMetadata getServiceMetadata(String serviceName, String groupName) {
        try {
            List<Instance> instances = namingService.selectInstances(serviceName, groupName, true);
            
            if (instances == null || instances.isEmpty()) {
                logger.warn("No healthy instances found for service: {} in group: {}", serviceName, groupName);
                return null;
            }
            
            // 策略变更：直接从 Nacos 配置中心读取完整的 metadata
            // 因为 Dubbo 会将服务元数据自动存储到配置中心
            String dataId = buildConfigDataId(serviceName, groupName);
            String configUrl = String.format(CONFIG_CENTER_BASE_URL, nacosServerAddr, dataId, "dubbo");
            
            logger.info("Fetching metadata from Nacos Config Center: {}", configUrl);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(configUrl))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 && response.body() != null && !response.body().isEmpty()) {
                // 配置中心的 metadata 是原始 JSON，不需要解压
                return ApiMetadataDeserializer.deserialize(response.body());
            } else {
                logger.warn("No API metadata found in config center for service: {}. Response code: {}, Body: {}", 
                    serviceName, response.statusCode(), response.body());
                return null;
            }
            
        } catch (NacosException e) {
            logger.error("Failed to get instances from Nacos for service: {}", serviceName, e);
            throw new RuntimeException("Failed to get instances from Nacos", e);
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to fetch metadata from Nacos config center for service: {}", serviceName, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to fetch metadata from Nacos config center", e);
        }
    }
    
    /**
     * 构建配置中心的 dataId
     * 格式：{interfaceName}:{version}:{group}:provider:{applicationName}
     */
    private String buildConfigDataId(String serviceName, String groupName) {
        // 这里需要根据实际的服务接口来构建 dataId
        // 简化处理：假设只有一个服务接口
        // 实际应该通过服务发现获取所有接口，然后选择第一个
        return String.format("%s.%s:1.0.0:%s:provider:%s",
            "com.pocrd.api_publish_service.api.GreeterServiceHttpExport",
            "GreeterServiceHttpExport",
            groupName.toLowerCase(),
            serviceName);
    }
    
    /**
     * 获取所有服务的元数据
     */
    public List<String> getAllServices() throws NacosException {
        // Nacos 没有直接获取所有服务的 API，需要预先知道服务名
        // 这里返回空列表，实际使用时应该通过配置指定服务名
        return List.of();
    }
    
    /**
     * 关闭 Nacos 连接
     */
    public void close() throws NacosException {
        if (namingService != null) {
            namingService.shutDown();
        }
    }
    
    /**
     * 解码 Base64 并解压缩 GZIP
     */
    private String decodeAndDecompress(String encodedData) throws IOException {
        byte[] decoded = Base64.getDecoder().decode(encodedData);
        
        try (GZIPInputStream gzip = new GZIPInputStream(new java.io.ByteArrayInputStream(decoded))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
