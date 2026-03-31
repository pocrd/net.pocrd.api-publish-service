# API Publish Service - Code Generator

根据 Nacos 中的 metadata 信息构建不同语言版本的客户端代码。

## 功能特性

- ✅ 从 Nacos 服务注册中心读取元数据
- ✅ 支持 GZIP 压缩和 Base64 编码的元数据解析
- ✅ 多语言代码生成（Java、Go）
- ✅ 可扩展的代码生成器架构
- ✅ 命令行工具，易于集成到 CI/CD 流程

## 快速开始

### 1. 编译项目

```bash
cd /Users/pocrd/workspace/deploy/api-publish-service
mvn clean package -DskipTests
```

### 2. 运行代码生成器

#### Java 客户端

```bash
java -jar codegen/target/api-publish-service-codegen-1.0.0-jar-with-dependencies.jar \
  --nacos-addr localhost:8848 \
  --service-name api-publish-service \
  --group-name PUBLIC-GROUP \
  --language java \
  --output-dir ./generated-java-client \
  --java-package com.example.client
```

#### Go 客户端

```bash
java -jar codegen/target/api-publish-service-codegen-1.0.0-jar-with-dependencies.jar \
  --nacos-addr localhost:8848 \
  --service-name api-publish-service \
  --group-name PUBLIC-GROUP \
  --language go \
  --output-dir ./generated-go-client \
  --go-module github.com/example/client
```

### 3. 命令行参数说明

| 参数 | 简写 | 必需 | 说明 | 默认值 |
|------|------|------|------|--------|
| --nacos-addr | -n | 是 | Nacos 服务器地址 | - |
| --service-name | -s | 是 | Nacos 中的服务名称 | - |
| --group-name | -g | 否 | Nacos 中的组名 | PUBLIC-GROUP |
| --language | -l | 是 | 目标编程语言 | - |
| --output-dir | -o | 是 | 生成代码的输出目录 | - |
| --java-package | -p | 否 | Java 客户端的包名 | com.example.client |
| --go-module | -m | 否 | Go 客户端的模块名 | github.com/example/client |
| --help | -h | 否 | 显示帮助信息 | - |

## 生成的代码结构

### Java 客户端

```
generated-java-client/
├── com/
│   └── example/
│       ├── client/
│       │   └── GreeterServiceClient.java
│       └── entity/
│           ├── ApiInfo.java
│           └── ServiceInfo.java
```

### Go 客户端

```
generated-go-client/
├── go.mod
├── client/
│   └── greeterservice_client.go
└── entity/
    ├── ApiInfo.go
    └── ServiceInfo.go
```

## 扩展新的语言生成器

要添加新的编程语言支持，需要：

1. 创建新的代码生成器类，继承 `AbstractCodeGenerator`
2. 实现 `getLanguage()` 和 `generate()` 方法
3. 在 `CodeGeneratorFactory` 中注册

### 示例：添加 Python 支持

```java
public class PythonClientCodeGenerator extends AbstractCodeGenerator {
    
    @Override
    public String getLanguage() {
        return "python";
    }
    
    @Override
    public void generate(ApiMetadata metadata, String outputDir) {
        // 实现 Python 代码生成逻辑
    }
}
```

然后在工厂中注册：

```java
CodeGeneratorFactory.registerGenerator("python", new PythonClientCodeGenerator("my_package"));
```

## 元数据格式

服务实例自定义器会将 SDK 注解信息收集并存储到 Nacos 的服务实例元数据中，键为 `api.metadata`。

元数据包含：
- **services**: 服务接口定义列表
  - interfaceName: 接口全限定名
  - apiGroup: API 分组信息
  - description: 服务描述
  - methods: 方法列表
- **entities**: 实体类型映射
  - key: 实体类全名
  - value: 实体定义（字段、类型等）

## 技术栈

- **Nacos Client**: 服务发现与元数据读取
- **Jackson**: JSON 序列化/反序列化
- **Apache Commons CLI**: 命令行参数解析
- **SLF4J + Logback**: 日志框架

## 注意事项

1. 确保 Nacos 服务已经注册并且包含 `api.metadata` 元数据
2. 元数据使用 GZIP 压缩和 Base64 编码存储
3. 生成的代码仅供参考，可能需要根据具体需求进行微调
4. 流式调用（StreamObserver）目前会被跳过，需要手动处理

## 开发指南

### 项目结构

```
codegen/
├── src/
│   ├── main/
│   │   ├── java/com/pocrd/api_publish_service/codegen/
│   │   │   ├── cli/              # 命令行相关
│   │   │   ├── generator/        # 代码生成器
│   │   │   ├── model/            # 数据模型
│   │   │   └── service/          # 服务层
│   │   └── resources/
│   └── test/
│       └── java/
└── pom.xml
```

### 核心组件

1. **NacosMetadataService**: 从 Nacos 获取和解压元数据
2. **ApiMetadataDeserializer**: JSON 反序列化
3. **CodeGenerator**: 代码生成器接口
4. **AbstractCodeGenerator**: 抽象基类，提供通用工具方法
5. **JavaClientCodeGenerator**: Java 代码生成实现
6. **GoClientCodeGenerator**: Go 代码生成实现
7. **CodeGeneratorFactory**: 生成器工厂，管理不同语言的生成器

## 许可证

与主项目保持一致
