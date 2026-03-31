FROM eclipse-temurin:21

WORKDIR /app

# 复制编译后的类文件和依赖库
COPY service/target/classes ./classes
COPY service/target/lib ./lib

# 暴露 Dubbo 协议端口
EXPOSE 50053
EXPOSE 50054

# 使用 classpath 方式启动
ENTRYPOINT ["java", "-cp", "classes:lib/*", "com.pocrd.api_publish_service.service.ApiPublishServiceApplication"]
