# Client 测试工程配置说明

## 两种测试模式

### 1. HTTP 模式（通过 Higress 网关）

**适用场景**：
- 测试暴露给外部用户的 HTTP API
- API 验收测试
- 端到端集成测试

**调用链路**：
```
Client Test → HTTP → Higress Gateway (port 80) → Dubbo Triple Service (port 50051)
```

**运行命令**：
```bash
# 使用默认配置（HTTP 模式）
mvn test -pl client

# 或显式指定 HTTP 模式
mvn test -pl client -Phttp-test
```

**环境变量配置**：
```bash
export HIGRESS_URL=http://localhost:80
export TEST_MODE=http
```

---

### 2. Dubbo RPC 模式（内部集群调用）

**适用场景**：
- 测试内部 Dubbo 集群间的 RPC 调用
- 服务间集成测试
- 性能基准测试

**调用链路**：
```
Client Test → Dubbo Native RPC → Dubbo Service Cluster
```

**运行命令**：
```bash
# 使用 Dubbo RPC 模式
mvn test -pl client -Pdubbo-test
```

**环境变量配置**：
```bash
export DUBBO_URL=tri://localhost:50051
export NACOS_ADDRESS=nacos://localhost:8848
export TEST_MODE=dubbo
```

---

## 配置参数说明

| 参数名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `test.mode` | 测试模式 | `http` | `http` / `dubbo` |
| `higress.url` | Higress 网关地址 | `http://localhost:80` | `http://gateway.example.com` |
| `dubbo.url` | Dubbo 服务地址 | `tri://localhost:50051` | `tri://service-host:50051` |
| `nacos.address` | Nacos 注册中心地址 | `nacos://localhost:8848` | `nacos://nacos-server:8848` |

---

## 测试示例

### HTTP 模式测试示例

```bash
# 测试本地 Higress 网关
mvn test -pl client \
  -Dhigress.url=http://localhost:80 \
  -Dtest.mode=http
```

### Dubbo RPC 模式测试示例

```bash
# 测试本地 Dubbo 服务
mvn test -pl client \
  -Ddubbo.url=tri://localhost:50051 \
  -Dnacos.address=nacos://localhost:8848 \
  -Dtest.mode=dubbo
```

### 远程环境测试

```bash
# 测试远程环境的 Higress 网关
mvn test -pl client \
  -Dhigress.url=https://api.example.com \
  -Dtest.mode=http

# 测试远程 Dubbo 集群
mvn test -pl client \
  -Ddubbo.url=tri://remote-service:50051 \
  -Dnacos.address=nacos://remote-nacos:8848 \
  -Dtest.mode=dubbo
```

---

## 依赖说明

### 核心依赖
- **api-publish-service-api**: 服务接口定义（必须）
- **dubbo-rpc-triple**: Dubbo Triple 协议支持
- **nacos-client**: Nacos 注册中心客户端
- **httpclient5**: HTTP 客户端（用于 HTTP 模式）
- **jackson-databind**: JSON 序列化/反序列化
- **spring-context**: Spring 容器支持

### 测试依赖
- **junit-jupiter**: JUnit 5 测试框架
- **spring-test**: Spring 测试支持

---

## 注意事项

1. **HTTP 模式**需要 Higress 网关正常运行
2. **Dubbo RPC 模式**需要 Nacos 注册中心可用
3. 确保网络连通性（本地测试需要端口可访问）
4. 生产环境测试建议使用独立的测试环境
