package com.pocrd.api_publish_service.client;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 客户端工具类（用于通过 Higress 网关调用服务）
 */
public class HttpClientUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    
    public HttpClientUtils(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    /**
     * 调用 GreeterServiceHttpExport 的 greet 方法
     * 
     * 【重要说明】Dubbo Triple 单参数 String 接口的调用方式
     * 当接口只有一个参数且类型为 String 时，使用 POST + JSON body 会有歧义：
     *   - 发送 {"name":"World"} 时，Dubbo 会把整个 JSON 字符串赋给 name 参数
     *   - 结果 name = "{\"name\":\"World\"}" 而不是 "World"
     * 
     * 解决方案：使用 URL 查询参数（推荐）：/greet?name=World
     * 
     * @param name 参数名称
     * @return 响应结果
     */
    public String greet(String name) throws Exception {
        // 使用 URL 查询参数传递参数（解决单参数 String 接口的歧义问题）
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String url = baseUrl + "/api/com.pocrd.api_publish_service.api.GreeterServiceHttpExport/greet?name=" + encodedName;
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            
            return httpClient.execute(get, response -> {
                int code = response.getCode();
                if (code >= 200 && code < 300) {
                    return EntityUtils.toString(response.getEntity());
                } else {
                    throw new RuntimeException("HTTP 请求失败：" + code);
                }
            });
        }
    }
    
    /**
     * 解析响应为 JSON 对象
     */
    public JsonNode parseJson(String json) throws Exception {
        return objectMapper.readTree(json);
    }
}
