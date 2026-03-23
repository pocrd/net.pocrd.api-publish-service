# Dubbo Triple + Nacos 微服务示例

这是一个基于 Dubbo Triple 协议和 Nacos 注册中心的微服务示例项目，使用 JDK 21 和 Spring Boot 构建。

## 项目结构

- **api**: 服务接口定义模块
- **service**: 服务实现模块
- **dao**: 数据访问对象模块
- **client**: 集成测试模块

## 功能特性

- ✅ Dubbo Triple 协议支持（兼容 gRPC）
- ✅ Nacos 服务注册与发现
- ✅ 一元调用（Unary Call）
- ✅ 服务端流式调用（Server Streaming）
- ✅ 双向流式调用（Bidirectional Streaming）
- ✅ 容器化部署支持
- ✅ JDK 21 支持

## 快速开始

### 1. 环境要求

- JDK 21
- Maven 3.6+
- Docker (可选，用于容器化部署)

### 2. 编译项目

```bash
cd /Users/pocrd/workspace/deploy/api-publish-service
mvn clean compile
```

### 3. 运行服务

```bash
# 构建项目
mvn clean package -DskipTests

# 运行服务（本地）
java -jar service/target/api-publish-service-service-1.0.0.jar
```

### 4. 使用 Docker 运行

注意：此服务依赖于外部的 Nacos 服务。在运行此服务前，请确保 Nacos 服务已在运行。

```bash
# 使用部署脚本
./deploy.sh deploy

# 或手动构建并运行
docker-compose up --build
```

注意：此服务依赖于外部的 Nacos 和 Higress 服务。在运行前，请确保它们已在运行并连接到 `higress-net` 网络。

## 服务接口

`GreeterService` 接口提供了以下方法：

1. **greet(String name)** - 一元调用：简单问候
2. **greetStream(String name, StreamObserver<String> observer)** - 服务端流：多次问候

## 配置说明

服务配置位于 `service/src/main/resources/application.yml`：

- 服务器端口：9090
- Triple 协议端口：50051 (Higress 调用，fastjson2 序列化)
- Dubbo 协议端口：50052 (内部调用，Fury 序列化)
- Nacos 注册地址：nacos-server:8848
- 应用名称：api-publish-service

## 扩展性

项目设计考虑了扩展性：

- 模块化的Maven多模块结构
- 清晰的API与实现分离
- 支持多种RPC调用模式
- 便于集成数据库和其他中间件

## 开发指南

1. 在 `api` 模块中定义服务接口
2. 在 `service` 模块中实现服务逻辑
3. 在 `dao` 模块中处理数据访问逻辑
4. 使用 `@DubboService` 注解暴露服务
5. 通过 Nacos 实现服务注册与发现