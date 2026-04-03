#!/bin/bash

# =============================================================================
# Java SDK 代码生成脚本
# 从 Nacos 获取元数据并生成 Java 客户端代码
# =============================================================================
# 用法:
#   ./generate_java_sdk.sh                    # 生成所有服务的 SDK（默认 Nacos）
#   ./generate_java_sdk.sh http://nacos:8848  # 指定 Nacos 地址生成所有服务
#   ./generate_java_sdk.sh -s com.pocrd.api.GreeterService  # 生成单个服务
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 获取脚本所在目录（现在在 codegen/java/ 下）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# 项目根目录（codegen/java 的上两级）
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

# 默认配置
DEFAULT_NACOS_URL="http://localhost:30848"
CODEGEN_JAVA_DIR="$SCRIPT_DIR"
# decode_nacos_metadata.py 路径
DECODE_SCRIPT="${PROJECT_ROOT}/client/script/decode_nacos_metadata.py"

# 参数解析
NACOS_URL=""
SERVICE_INTERFACE=""
VERSION="1.0.0"
GROUP="public"
GENERATE_ALL=true

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项] [Nacos地址]

选项:
    -h, --help                  显示帮助信息
    -s, --service INTERFACE     指定服务接口名（如 com.pocrd.api.GreeterService）
    -v, --version VERSION       服务版本（默认: 1.0.0）
    -g, --group GROUP           服务分组（默认: public）
    -n, --nacos URL             Nacos 地址（默认: http://localhost:30848）

示例:
    # 生成所有服务的 SDK
    $0

    # 指定 Nacos 地址生成所有服务
    $0 http://nacos:8848

    # 生成单个服务的 SDK
    $0 -s com.pocrd.api.GreeterService

    # 完整参数指定单个服务
    $0 -n http://nacos:8848 -s com.pocrd.api.GreeterService -v 1.0.0 -g public

EOF
}

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -s|--service)
            SERVICE_INTERFACE="$2"
            GENERATE_ALL=false
            shift 2
            ;;
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -g|--group)
            GROUP="$2"
            shift 2
            ;;
        -n|--nacos)
            NACOS_URL="$2"
            shift 2
            ;;
        *)
            # 如果未指定 -n，第一个位置参数作为 Nacos 地址
            if [[ -z "$NACOS_URL" && ! "$1" =~ ^- ]]; then
                NACOS_URL="$1"
                shift
            else
                echo -e "${RED}未知选项: $1${NC}"
                show_help
                exit 1
            fi
            ;;
    esac
done

# 设置默认 Nacos 地址
if [[ -z "$NACOS_URL" ]]; then
    NACOS_URL="$DEFAULT_NACOS_URL"
fi

# 检查依赖
check_dependencies() {
    local missing=()
    
    if ! command -v python3 &> /dev/null; then
        missing+=("python3")
    fi
    
    if [[ ${#missing[@]} -gt 0 ]]; then
        echo -e "${RED}错误：缺少以下依赖: ${missing[*]}${NC}"
        exit 1
    fi
}

# 打印信息
info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 主函数
main() {
    echo -e "${BLUE}==============================================${NC}"
    echo -e "${BLUE}Java SDK 代码生成工具${NC}"
    echo -e "${BLUE}==============================================${NC}"
    
    # 检查依赖
    check_dependencies
    
    # 检查目录是否存在
    if [[ ! -d "$CODEGEN_JAVA_DIR" ]]; then
        error "codegen/java 目录不存在: $CODEGEN_JAVA_DIR"
        exit 1
    fi
    
    info "Nacos 地址: $NACOS_URL"
    info "输出目录: $CODEGEN_JAVA_DIR"
    
    # 步骤 1: 获取 Nacos 元数据
    echo ""
    info "步骤 1: 从 Nacos 获取元数据..."
    
    # 检查 decode_nacos_metadata.py 是否存在
    if [[ ! -f "$DECODE_SCRIPT" ]]; then
        error "decode_nacos_metadata.py 脚本不存在: $DECODE_SCRIPT"
        exit 1
    fi
    
    if [[ "$GENERATE_ALL" == true ]]; then
        # 获取所有服务
        info "模式: 获取所有 Dubbo 服务"
        if ! python3 "$DECODE_SCRIPT" "$NACOS_URL"; then
            error "获取 Nacos 元数据失败"
            exit 1
        fi
    else
        # 获取单个服务
        info "模式: 获取单个服务 [$SERVICE_INTERFACE]"
        if ! python3 "$DECODE_SCRIPT" "$NACOS_URL" "$SERVICE_INTERFACE" "$VERSION" "$GROUP"; then
            error "获取 Nacos 元数据失败"
            exit 1
        fi
    fi
    
    # 检查元数据文件是否生成（decode_nacos_metadata.py 默认输出到 ../../codegen/nacos_metadata.json）
    METADATA_FILE="${PROJECT_ROOT}/codegen/nacos_metadata.json"
    
    if [[ ! -f "$METADATA_FILE" ]]; then
        error "元数据文件未生成"
        exit 1
    fi
    
    success "元数据已保存到: $METADATA_FILE"
    
    # 步骤 2: 生成 Java 代码
    echo ""
    info "步骤 2: 生成 Java 客户端代码..."
    
    cd "$CODEGEN_JAVA_DIR"
    
    # 运行 javagen.py
    if ! python3 javagen.py "$METADATA_FILE" -o .; then
        error "Java 代码生成失败"
        exit 1
    fi
    
    echo ""
    success "Java SDK 代码生成完成！"
    
    # 步骤 3: 编译验证
    echo ""
    info "步骤 3: 编译验证生成的代码..."
    
    if command -v mvn &> /dev/null; then
        if mvn clean compile -q; then
            success "编译验证通过！"
        else
            warn "编译验证失败，请检查生成的代码"
            exit 1
        fi
    else
        warn "未找到 Maven，跳过编译验证"
    fi
    
    # 步骤 4: 打包 JAR（供 client 模块使用）
    echo ""
    info "步骤 4: 打包 JAR 文件..."
    
    if command -v mvn &> /dev/null; then
        if mvn package -DskipTests -q; then
            success "JAR 打包完成！"
            info "JAR 文件: ${CODEGEN_JAVA_DIR}/target/api-publish-service-codegen-java-1.0.0.jar"
        else
            warn "JAR 打包失败"
            exit 1
        fi
    else
        warn "未找到 Maven，跳过打包"
    fi
    
    echo ""
    echo -e "${GREEN}==============================================${NC}"
    echo -e "${GREEN}全部完成！${NC}"
    echo -e "${GREEN}==============================================${NC}"
    info "生成的代码位于: ${CODEGEN_JAVA_DIR}/com/pocrd/clientsdk/"
}

# 执行主函数
main
