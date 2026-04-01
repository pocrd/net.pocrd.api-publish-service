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
DEFAULT_GATEWAY_URL="https://api.caringfamily.cn:30443"
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
    -u, --url URL       设置网关 URL (默认: ${DEFAULT_GATEWAY_URL})
    -c, --cert          使用 mTLS 模式 (需要证书)
    -i, --insecure      使用 HTTP 模式 (不使用证书)
    -m, --method NAME   指定测试方法名 (如: testGreet, testGreet2)
    -t, --test-class    指定测试类 (默认: AllApiMethodsTest, 可选: CodegenSdkTest)

示例:
    # 运行所有测试 (HTTPS + mTLS)
    $0

    # 运行指定测试方法 (AllApiMethodsTest 中)
    $0 -m testValidApiTestService_TestNestedEntity

    # 运行 CodegenSdkTest 中的指定方法
    $0 -t CodegenSdkTest -m testGreet2

    # 使用 HTTP 模式
    $0 -i

    # 使用自定义网关地址
    $0 -u http://localhost:30080 -i

EOF
}

# 解析参数
USE_CERTS=true
GATEWAY_URL="${DEFAULT_GATEWAY_URL}"
TEST_METHOD=""
TEST_CLASS="${DEFAULT_TEST_CLASS}"

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

# 添加系统属性
MVN_CMD="${MVN_CMD} -Dgateway.url=${GATEWAY_URL}"

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
    echo "网关 URL: ${GATEWAY_URL}"
    echo "CA 证书: ${CA_CERT}"
    echo "客户端证书: ${CLIENT_CERT}"
else
    echo "=============================================="
    echo "Codegen SDK 测试 (HTTP 模式)"
    echo "=============================================="
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

# 先强制编译测试代码（确保修改生效）
echo "编译测试代码..."
mvn test-compile -q

echo ""
echo "执行命令:"
echo "$MVN_CMD"
echo ""
$MVN_CMD
