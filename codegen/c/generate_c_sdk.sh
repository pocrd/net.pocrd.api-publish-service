#!/bin/bash

# =============================================================================
# C SDK 代码生成脚本
# 从 Nacos 获取元数据并生成 C 客户端代码
# =============================================================================
# 用法:
#   ./generate_c_sdk.sh                    # 生成所有服务的 SDK（默认 Nacos）
#   ./generate_c_sdk.sh http://nacos:8848  # 指定 Nacos 地址生成所有服务
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# 项目根目录
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# 默认配置
DEFAULT_NACOS_URL="http://localhost:30848"
CODEGEN_C_DIR="$SCRIPT_DIR"
# decode_nacos_metadata.py 路径
DECODE_SCRIPT="${PROJECT_ROOT}/../client/script/decode_nacos_metadata.py"

# 参数解析
NACOS_URL=""

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项] [Nacos地址]

选项:
    -h, --help                  显示帮助信息
    -n, --nacos URL             Nacos 地址（默认: http://localhost:30848）

示例:
    # 生成所有服务的 SDK
    $0

    # 指定 Nacos 地址生成所有服务
    $0 http://nacos:8848

EOF
}

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
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
    echo -e "${BLUE}C SDK 代码生成工具${NC}"
    echo -e "${BLUE}==============================================${NC}"
    
    # 检查依赖
    check_dependencies
    
    # 检查目录是否存在
    if [[ ! -d "$CODEGEN_C_DIR" ]]; then
        error "codegen/c 目录不存在: $CODEGEN_C_DIR"
        exit 1
    fi
    
    info "Nacos 地址: $NACOS_URL"
    info "输出目录: $CODEGEN_C_DIR/src/autogen"
    
    # 清理旧的生成代码
    if [[ -d "$CODEGEN_C_DIR/src/autogen" ]]; then
        info "清理旧的生成代码..."
        rm -rf "$CODEGEN_C_DIR/src/autogen"
    fi
    mkdir -p "$CODEGEN_C_DIR/src/autogen"
    
    # 步骤 1: 生成 C 代码
    echo ""
    info "步骤 1: 从 Nacos 获取元数据并生成 C 代码..."
    
    cd "$CODEGEN_C_DIR"
    
    # 运行 cgen.py
    if ! python3 cgen.py --nacos "$NACOS_URL" --all -o ./src/autogen; then
        error "C 代码生成失败"
        exit 1
    fi
    
    echo ""
    success "C SDK 代码生成完成！"
    
    # 步骤 2: 创建 CMakeLists.txt 模板（如果不存在）
    echo ""
    info "步骤 2: 检查 CMakeLists.txt..."
    
    CMAKE_FILE="$CODEGEN_C_DIR/src/CMakeLists.txt"
    if [[ ! -f "$CMAKE_FILE" ]]; then
        cat > "$CMAKE_FILE" << 'EOF'
cmake_minimum_required(VERSION 3.10)
project(apg_sdk C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# 查找依赖
find_package(CURL REQUIRED)
find_package(cJSON REQUIRED)

# 收集源文件
file(GLOB_RECURSE SOURCES "*.c")
file(GLOB_RECURSE HEADERS "*.h")

# 创建静态库
add_library(apg_sdk STATIC ${SOURCES})

target_include_directories(apg_sdk PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CURL_INCLUDE_DIRS}
    ${CJSON_INCLUDE_DIRS}
)

target_link_libraries(apg_sdk
    ${CURL_LIBRARIES}
    ${CJSON_LIBRARIES}
)

# 安装目标
install(TARGETS apg_sdk
    ARCHIVE DESTINATION lib
    LIBRARY DESTINATION lib
)

install(FILES ${HEADERS} DESTINATION include/apg_sdk)
EOF
        success "已创建 CMakeLists.txt 模板"
    else
        info "CMakeLists.txt 已存在，跳过创建"
    fi
    
    echo ""
    echo -e "${GREEN}==============================================${NC}"
    echo -e "${GREEN}全部完成！${NC}"
    echo -e "${GREEN}==============================================${NC}"
    info "生成的代码位于: ${CODEGEN_C_DIR}/src/autogen/"
    info "使用说明:"
    info "  1. 确保系统已安装 cJSON 库"
    info "  2. 运行: make clean && make"
    info "  3. 生成的库文件将位于 build 目录中"
}

# 执行主函数
main
