#!/bin/bash

# =============================================================================
# C SDK 测试脚本
# 使用生成的 C SDK 调用 Dubbo 服务接口
# 支持 HTTP 和 HTTPS + mTLS 模式
# =============================================================================

set -e

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLIENT_DIR="$(dirname "$SCRIPT_DIR")"
CODEGEN_C_DIR="$(dirname "$CLIENT_DIR")"

# 默认配置
DEFAULT_MODE="local"
GATEWAY_URL="https://api.caringfamily.cn"
CERT_DIR="${CODEGEN_C_DIR}/../../../higress/certs/files/bagua"

# 客户端证书路径
CLIENT_CERT="${CERT_DIR}/testFactory/devices/device001/device001-fullchain.crt"
CLIENT_KEY="${CERT_DIR}/testFactory/devices/device001/device001.key"

# 测试程序
TEST_GREET="${CODEGEN_C_DIR}/build/bin/test_greet"
TEST_ALL_API="${CODEGEN_C_DIR}/build/bin/test_all_api"

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项] [测试用例]

选项:
    -h, --help          显示帮助信息
    -u, --url URL       设置网关 URL (默认: https://api.caringfamily.cn)
    -r, --remote        使用 remote 模式 (直接连接远程服务器)
    --local             使用 local 模式 (默认): 发往本地 30443 端口
    -c, --cert PATH     客户端证书路径
    -k, --key PATH      客户端私钥路径
    -d, --debug         开启调试模式
    --build             重新编译测试程序

测试用例:
    greet               运行 greet 测试
    all                 运行全量 API 测试
    (默认)              运行所有测试

模式说明:
    local   本地模式 (默认)，请求通过 resolve 发往本地 30443 端口
    remote  远程模式，请求直接发往远程服务器 443 端口

示例:
    # 运行所有测试 (local 模式，HTTPS + mTLS)
    $0

    # 使用 remote 模式
    $0 -r

    # 开启调试模式
    $0 -d

    # 重新编译并测试
    $0 --build

    # 使用自定义证书
    $0 -c /path/to/cert.crt -k /path/to/key.key

EOF
}

# 解析参数
MODE="${DEFAULT_MODE}"
DEBUG_MODE=false
DO_BUILD=false
TEST_CASE=""

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
        -r|--remote)
            MODE="remote"
            shift
            ;;
        --local)
            MODE="local"
            shift
            ;;
        -c|--cert)
            CLIENT_CERT="$2"
            shift 2
            ;;
        -k|--key)
            CLIENT_KEY="$2"
            shift 2
            ;;
        -d|--debug)
            DEBUG_MODE=true
            shift
            ;;
        --build)
            DO_BUILD=true
            shift
            ;;
        greet|all)
            TEST_CASE="$1"
            shift
            ;;
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 检查证书文件
if [[ ! -f "$CLIENT_CERT" ]]; then
    echo "❌ 错误：客户端证书不存在：$CLIENT_CERT"
    exit 1
fi
if [[ ! -f "$CLIENT_KEY" ]]; then
    echo "❌ 错误：客户端私钥不存在：$CLIENT_KEY"
    exit 1
fi

# 转换为绝对路径
CLIENT_CERT="$(cd "$(dirname "$CLIENT_CERT")" && pwd)/$(basename "$CLIENT_CERT")"
CLIENT_KEY="$(cd "$(dirname "$CLIENT_KEY")" && pwd)/$(basename "$CLIENT_KEY")"

# 编译测试程序
if [[ "$DO_BUILD" == true ]] || [[ ! -f "$TEST_GREET" ]]; then
    echo "=============================================="
    echo "编译 C SDK 和测试程序..."
    echo "=============================================="
    cd "$CODEGEN_C_DIR"
    make clean >/dev/null 2>&1 || true
    make && make client && make tests
    echo ""
fi

# 执行测试函数
run_test() {
    local test_bin="$1"
    local test_cmd="$test_bin --cert $CLIENT_CERT --key $CLIENT_KEY"
    
    if [[ "$MODE" != "local" ]]; then
        test_cmd="$test_cmd --remote"
    fi
    
    if [[ "$DEBUG_MODE" == true ]]; then
        test_cmd="$test_cmd --debug"
    fi
    
    echo "执行: $test_cmd"
    $test_cmd
}

# 设置环境变量
export GATEWAY_URL="$GATEWAY_URL"

cd "$CODEGEN_C_DIR"

# 显示配置信息
echo "=============================================="
echo "C SDK 测试"
echo "=============================================="
echo "模式: ${MODE}"
echo "网关 URL: ${GATEWAY_URL}"
echo "客户端证书: ${CLIENT_CERT}"
if [[ "$DEBUG_MODE" == true ]]; then
    echo "调试模式: 开启"
fi
echo "=============================================="
echo ""

# 根据测试用例执行
case "${TEST_CASE}" in
    greet)
        run_test "$TEST_GREET"
        ;;
    all)
        run_test "$TEST_ALL_API"
        ;;
    *)
        # 默认运行所有测试
        run_test "$TEST_GREET"
        echo ""
        run_test "$TEST_ALL_API"
        ;;
esac
