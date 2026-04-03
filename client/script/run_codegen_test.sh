#!/bin/bash

# =============================================================================
# Codegen SDK 测试脚本
# 使用生成的 Java SDK 调用 Dubbo 服务接口
# 支持 HTTP 和 HTTPS + mTLS 模式
# =============================================================================

set -e

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 默认配置
DEFAULT_MODE="local"
GATEWAY_URL="https://api.caringfamily.cn"
CERT_DIR="${PROJECT_ROOT}/../../higress/certs/files/bagua"

# CA 证书路径
CA_CERT="${CERT_DIR}/bagua.crt"
# 客户端证书路径
CLIENT_CERT="${CERT_DIR}/testFactory/devices/device001/device001-fullchain.crt"
# 客户端私钥路径
CLIENT_KEY="${CERT_DIR}/testFactory/devices/device001/device001.key"

# 测试类
DEFAULT_TEST_CLASS="com.pocrd.api_publish_service.client.AllApiMethodsTest"
CODEGEN_SDK_TEST_CLASS="com.pocrd.api_publish_service.client.CodegenSdkTest"
ALL_TEST_CLASSES="com.pocrd.api_publish_service.client.CodegenSdkTest,com.pocrd.api_publish_service.client.AllApiMethodsTest"

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项] [测试方法]

选项:
    -h, --help          显示帮助信息
    -u, --url URL       设置网关 URL (默认: https://api.caringfamily.cn)
    -r, --remote        使用 remote 模式 (直接连接远程服务器)
    -c, --cert          使用 mTLS 模式 (需要证书)
    -i, --insecure      使用 HTTP 模式 (不使用证书)
    -m, --method NAME   指定测试方法名 (如: testGreet, testGreet2)
    -t, --test-class    指定测试类 (默认: AllApiMethodsTest, 可选: CodegenSdkTest)
    -d, --debug         开启调试模式，输出详细的调试信息
    --local             使用 local 模式 (默认): 发往本地 30443 端口

模式说明:
    local   本地模式 (默认)，请求通过 resolve 发往本地 30443 端口
    remote  远程模式，请求直接发往远程服务器 443 端口

示例:
    # 运行所有测试 (local 模式，HTTPS + mTLS)
    $0

    # 使用 remote 模式
    $0 -r

    # 运行指定测试方法 (AllApiMethodsTest 中)
    $0 -m testValidApiTestService_TestNestedEntity

    # 运行 CodegenSdkTest 中的指定方法
    $0 -t CodegenSdkTest -m testGreet2

    # 使用 HTTP 模式
    $0 -i

    # 使用自定义网关地址
    $0 -u http://localhost:30080 -i

    # 开启调试模式
    $0 -d

EOF
}

# 解析参数
USE_CERTS=true
MODE="${DEFAULT_MODE}"
TEST_METHOD=""
TEST_CLASS="${DEFAULT_TEST_CLASS}"
DEBUG_MODE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -u|--url)
            GATEWAY_URL="$2"
            shift 2
            ;;
        -c|--cert)
            USE_CERTS=true
            shift
            ;;
        -i|--insecure)
            USE_CERTS=false
            shift
            ;;
        -m|--method)
            TEST_METHOD="$2"
            shift 2
            ;;
        -t|--test-class)
            TEST_CLASS="$2"
            shift 2
            ;;
        -r|--remote)
            MODE="remote"
            shift
            ;;
        --local)
            MODE="local"
            shift
            ;;
        -d|--debug)
            DEBUG_MODE=true
            shift
            ;;
        *)
            # 如果没有指定 -m，第一个参数作为测试方法名
            if [[ -z "$TEST_METHOD" && ! "$1" =~ ^- ]]; then
                TEST_METHOD="$1"
                shift
            else
                echo "未知选项: $1"
                show_help
                exit 1
            fi
            ;;
    esac
done

# 网关 URL 统一使用 https://api.caringfamily.cn
# 通过 resolve.ip 和 resolve.port 控制实际连接目标

# 根据测试类名构建完整类名
case "${TEST_CLASS}" in
    CodegenSdkTest)
        FULL_TEST_CLASS="com.pocrd.api_publish_service.client.CodegenSdkTest"
        ;;
    AllApiMethodsTest)
        FULL_TEST_CLASS="com.pocrd.api_publish_service.client.AllApiMethodsTest"
        ;;
    *)
        # 如果传入的是完整类名，直接使用
        if [[ "${TEST_CLASS}" == *"."* ]]; then
            FULL_TEST_CLASS="${TEST_CLASS}"
        else
            FULL_TEST_CLASS="com.pocrd.api_publish_service.client.${TEST_CLASS}"
        fi
        ;;
esac

# 构建 Maven 命令
if [[ -n "$TEST_METHOD" ]]; then
    # 如果指定了测试方法，运行指定类中的指定方法
    MVN_CMD="mvn test -Dtest=${FULL_TEST_CLASS}#${TEST_METHOD}"
else
    if [[ "${TEST_CLASS}" == "CodegenSdkTest" ]]; then
        # 如果指定了 CodegenSdkTest 且没有方法，只运行该类
        MVN_CMD="mvn test -Dtest=${CODEGEN_SDK_TEST_CLASS}"
    elif [[ "${TEST_CLASS}" == "AllApiMethodsTest" ]]; then
        # 如果指定了 AllApiMethodsTest 且没有方法，只运行该类
        MVN_CMD="mvn test -Dtest=${DEFAULT_TEST_CLASS}"
    else
        # 运行所有测试类
        MVN_CMD="mvn test -Dtest=${ALL_TEST_CLASSES}"
    fi
fi

MVN_CMD="${MVN_CMD} -Dgateway.url=${GATEWAY_URL}"

# 如果开启调试模式，添加 debug 参数
if [[ "$DEBUG_MODE" == true ]]; then
    MVN_CMD="${MVN_CMD} -Ddebug=true"
fi

# 根据模式设置 resolve 参数
if [[ "$MODE" == "local" ]]; then
    # local 模式：启用 --resolve 功能，域名解析到本地 30443 端口
    MVN_CMD="${MVN_CMD} -Dresolve.ip=127.0.0.1 -Dresolve.port=30443"
else
    # remote 模式：解析到远程服务器（禁用本地 resolve）
    MVN_CMD="${MVN_CMD} -Dresolve.ip=api.caringfamily.cn -Dresolve.port=443"
fi

# 如果使用证书模式，添加证书路径
if [[ "$USE_CERTS" == true ]]; then
    # 检查证书文件是否存在
    if [[ ! -f "$CA_CERT" ]]; then
        echo "❌ 错误：CA 证书不存在：$CA_CERT"
        exit 1
    fi
    if [[ ! -f "$CLIENT_CERT" ]]; then
        echo "❌ 错误：客户端证书不存在：$CLIENT_CERT"
        exit 1
    fi
    if [[ ! -f "$CLIENT_KEY" ]]; then
        echo "❌ 错误：客户端私钥不存在：$CLIENT_KEY"
        exit 1
    fi

    MVN_CMD="${MVN_CMD} -Dca.cert.path=${CA_CERT}"
    MVN_CMD="${MVN_CMD} -Dclient.cert.path=${CLIENT_CERT}"
    MVN_CMD="${MVN_CMD} -Dclient.key.path=${CLIENT_KEY}"

    echo "=============================================="
    echo "Codegen SDK 测试 (HTTPS + mTLS)"
    echo "=============================================="
    echo "模式: ${MODE}"
    echo "网关 URL: ${GATEWAY_URL}"
    echo "CA 证书: ${CA_CERT}"
    echo "客户端证书: ${CLIENT_CERT}"
else
    echo "=============================================="
    echo "Codegen SDK 测试 (HTTP 模式)"
    echo "=============================================="
    echo "模式: ${MODE}"
    echo "网关 URL: ${GATEWAY_URL}"
fi

if [[ -n "$TEST_METHOD" ]]; then
    echo "测试方法: ${TEST_METHOD}"
else
    echo "测试方法: 全部"
fi
echo "=============================================="
echo ""

# 执行测试
cd "$PROJECT_ROOT"

# 检查 codegen SDK jar 是否存在
CODEGEN_JAR="${PROJECT_ROOT}/../codegen/java/target/api-publish-service-codegen-java-1.0.0.jar"
if [[ ! -f "$CODEGEN_JAR" ]]; then
    echo "❌ 错误：codegen SDK jar 不存在：$CODEGEN_JAR"
    echo "请先运行 codegen/java/generate_java_sdk.sh 生成 SDK 代码并打包"
    exit 1
fi

# 清理并重新编译测试代码（确保使用最新的 codegen jar）
echo "清理并编译测试代码..."
mvn clean test-compile -q

if [[ $? -ne 0 ]]; then
    echo "❌ 编译失败，请检查错误信息"
    exit 1
fi

echo ""
echo "执行命令:"
echo "$MVN_CMD"
echo ""
$MVN_CMD
