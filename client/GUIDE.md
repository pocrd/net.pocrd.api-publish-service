# Client 测试子工程使用指南

## 📋 概述

`client` 子工程是一个专门用于集成测试的模块，支持两种测试模式：

1. **HTTP 模式** - 通过 Higress 网关调用 Dubbo Triple 服务（模拟真实用户请求）
2. **Dubbo RPC 模式** - 直接通过 Dubbo 原生协议调用内部服务（服务间集成测试）

## 🏗️ 项目结构

```
client/
├── pom.xml                              # Maven 配置
├── README.md                            # 使用说明
└── src/test/
    ├── java/com/pocrd/api_publish_service/client/
    │   ├── TestConfigManager.java       # 测试配置管理器
    │   ├── HttpClientUtils.java         # HTTP 客户端工具
    │   └── GreeterServiceTest.java      # 示例测试用例
    └── resources/
        └── logging.properties           # 日志配置
```

## 🚀 快速开始

### 前置条件

1. 确保以下服务正常运行：
   - Nacos Server (端口：8848)
   - Dubbo Service (端口：50051)
   - Higress Gateway (端口：80) - 仅 HTTP 模式需要

2. 编译项目：
   ```bash
   mvn clean compile -pl client -am
   ```

### 测试模式 1: HTTP 模式（通过 Higress）

**适用场景**：测试暴露给外部用户的 HTTP API

```bash
# 运行 HTTP 模式测试
mvn test -pl client -Phttp-test

# 或指定自定义 Higress 地址
mvn test -pl client \
  -Dhigress.url=http://localhost:80 \
  -Dtest.mode=http
```

**调用链路**：
```
Test → HTTP Client → Higress Gateway (80) → Dubbo Triple Service (50051)
```

### 测试模式 2: Dubbo RPC 模式

**适用场景**：测试内部 Dubbo 集群间的 RPC 调用

```bash
# 运行 Dubbo RPC 模式测试
mvn test -pl client -Pdubbo-test

# 或指定自定义 Dubbo 服务地址
mvn test -pl client \
  -Ddubbo.url=tri://localhost:50051 \
  -Dnacos.address=nacos://localhost:8848 \
  -Dtest.mode=dubbo
```

**调用链路**：
```
Test → Dubbo Native RPC → Dubbo Service Cluster
```

## ⚙️ 配置参数

| 参数 | 说明 | 默认值 | 示例 |
|------|------|--------|------|
| `test.mode` | 测试模式 | `http` | `http` / `dubbo` |
| `higress.url` | Higress 网关地址 | `http://localhost:80` | `https://api.example.com` |
| `dubbo.url` | Dubbo 服务地址 | `tri://localhost:50051` | `tri://service:50051` |
| `nacos.address` | Nacos 注册中心地址 | `nacos://localhost:8848` | `nacos://nacos:8848` |

## 📝 编写自定义测试

### HTTP 模式测试示例

```java
@Test
void testCustomHttpEndpoint() throws Exception {
    // 创建 HTTP 客户端
    HttpClientUtils httpClient = new HttpClientUtils("http://localhost:80");
    
    // 调用服务
    String response = httpClient.sayHello("World");
    
    // 验证响应
    assertNotNull(response);
    var jsonNode = httpClient.parseJson(response);
    assertTrue(jsonNode.has("message"));
}
```

### Dubbo RPC 模式测试示例

```java
@Test
void testCustomDubboService() {
    // 获取服务代理
    TestConfigManager config = new TestConfigManager();
    config.init();
    
    var service = config.getGreeterService();
    
    // 调用服务
    var response = service.sayHello("Dubbo World");
    
    // 验证响应
    assertNotNull(response);
    assertTrue(response.getMessage().contains("Dubbo World"));
}
```

## 🔧 高级用法

### 1. 远程环境测试

```bash
# 测试生产环境的 Higress 网关
mvn test -pl client \
  -Dhigress.url=https://api.production.com \
  -Dtest.mode=http

# 测试远程 Dubbo 集群
mvn test -pl client \
  -Ddubbo.url=tri://prod-service:50051 \
  -Dnacos.address=nacos://prod-nacos:8848 \
  -Dtest.mode=dubbo
```

### 2. 环境变量方式配置

```bash
export HIGRESS_URL=http://gateway.example.com
export DUBBO_URL=tri://service:50051
export NACOS_ADDRESS=nacos://nacos:8848

mvn test -pl client
```

### 3. 只运行特定测试

```bash
# 只运行 HTTP 模式测试
mvn test -pl client \
  -Dtest=GreeterServiceTest#testSayHelloHttpMode

# 只运行 Dubbo 模式测试
mvn test -pl client \
  -Dtest=GreeterServiceTest#testSayHelloDubboMode
```

## 📊 依赖说明

### 核心依赖
- **api-publish-service-api**: 服务接口定义（必须）
- **dubbo-rpc-triple**: Dubbo Triple 协议支持
- **nacos-client**: Nacos 注册中心客户端
- **httpclient5**: HTTP 客户端（HTTP 模式）
- **jackson-databind**: JSON 处理
- **spring-context**: Spring 容器支持

### 测试依赖
- **junit-jupiter**: JUnit 5 测试框架
- **spring-test**: Spring 测试支持

## ⚠️ 注意事项

1. **HTTP 模式**需要 Higress 网关正常运行并正确路由到 Dubbo 服务
2. **Dubbo RPC 模式**需要 Nacos 注册中心可用，且服务已正确注册
3. 确保网络连通性（本地测试需要端口可访问）
4. 生产环境测试建议使用独立的测试环境
5. 测试前确认目标服务已经部署并健康

## 🐛 故障排查

### 问题 1: HTTP 模式连接失败

```bash
# 检查 Higress 网关是否运行
docker ps | grep higress

# 测试端口连通性
curl -I http://localhost:80
```

### 问题 2: Dubbo RPC 模式注册失败

```bash
# 检查 Nacos 是否运行
docker ps | grep nacos

# 检查服务是否已注册
curl http://localhost:8848/nacos/v1/ns/service/list

# 查看 Dubbo 服务日志
docker logs api-publish-service
```

### 问题 3: 测试未执行

检查当前运行的测试模式：
```bash
# 查看系统属性
mvn test -pl client -X | grep "test.mode"
```

## 📚 参考资源

- [Dubbo Triple 协议文档](https://dubbo.apache.org/zh/docs/concepts/rpc-protocol/)
- [Higress 网关文档](https://higress.io/docs/)
- [Nacos 官方文档](https://nacos.io/zh-cn/docs/quick-start.html)
- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)

---

**最后更新**: 2026-03-03  
**版本**: v1.0.0
