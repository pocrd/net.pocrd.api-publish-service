#!/bin/bash

# =============================================================================
# Dubbo 微服务部署脚本
# =============================================================================
# 支持功能:
#   - 本地 Maven 编译打包
#   - Docker 镜像构建
#   - Docker Compose 部署/启动/停止/重启
#   - 服务健康检查
#   - 日志查看
#
# 使用方法:
#   ./deploy.sh build       - 本地编译打包
#   ./deploy.sh docker      - 构建 Docker 镜像
#   ./deploy.sh up          - 启动服务
#   ./deploy.sh down        - 停止服务
#   ./deploy.sh restart     - 重启服务
#   ./deploy.sh logs        - 查看日志
#   ./deploy.sh status      - 查看服务状态
#   ./deploy.sh deploy      - 完整部署流程(编译+构建+启动)
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 服务配置
SERVICE_NAME="api-publish-service"
COMPOSE_FILE="docker-compose.yml"

# 强制模式标志（跳过 API 检查）
FORCE_MODE=false

# 打印信息
info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# 打印成功
success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 打印警告
warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# 打印错误
error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v "$1" &> /dev/null; then
        error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# 检查环境
check_env() {
    info "检查部署环境..."
    check_command java
    check_command mvn
    check_command docker
    check_command docker-compose
    success "环境检查通过"
}

# 检查 API 元数据
api_check() {
    info "检查 API 元数据..."
    
    # 查找 api 模块的 jar 文件（使用 SERVICE_NAME 拼接，排除 sources 和 javadoc）
    API_JAR_PATTERN="api/target/${SERVICE_NAME}-api-*.jar"
    API_JAR=$(ls $API_JAR_PATTERN 2>/dev/null | grep -v "sources" | grep -v "javadoc" | head -1)
    
    if [ -z "$API_JAR" ] || [ ! -f "$API_JAR" ]; then
        error "API jar 文件不存在: $API_JAR_PATTERN"
        error "请先执行 ./deploy.sh build 进行编译"
        exit 1
    fi
    
    info "使用 API jar: $(basename $API_JAR)"
    
    # 查找 SDK jar - 从 service 的 lib 目录中查找（作为依赖引入）
    SDK_JAR_PATTERN="service/target/lib/${SERVICE_NAME}-sdk-*.jar"
    SDK_JAR=$(ls $SDK_JAR_PATTERN 2>/dev/null | head -1)
    
    if [ -z "$SDK_JAR" ] || [ ! -f "$SDK_JAR" ]; then
        error "SDK jar 文件不存在: $SDK_JAR_PATTERN"
        error "请确保 service 模块已正确编译，SDK 作为依赖被复制到 lib 目录"
        exit 1
    fi
    
    info "使用 SDK jar: $(basename $SDK_JAR)"
    
    # 运行 ApiMetadataValidator 检查
    info "运行 ApiMetadataValidator 检查 API 接口..."
    
    # 构建 classpath（SDK jar + API jar + service 的所有依赖）
    CP="$SDK_JAR:$API_JAR"
    
    # 添加 service 的所有依赖
    if [ -d "service/target/lib" ]; then
        for lib in service/target/lib/*.jar; do
            if [ -f "$lib" ]; then
                CP="$CP:$lib"
            fi
        done
    fi
    
    # 执行检查
    if ! API_PUBLISH_SERVICE_NAME="$SERVICE_NAME" java -cp "$CP" com.pocrd.api_publish_service.sdk.util.ApiMetadataValidator "$API_JAR"; then
        if [ "$FORCE_MODE" = true ]; then
            warn "API 元数据检查失败，但强制模式已启用，继续部署..."
        else
            error "API 元数据检查失败，请修复上述问题后再部署"
            exit 1
        fi
    else
        success "API 元数据检查通过"
    fi
}

# 本地编译
build() {
    info "开始 Maven 编译打包..."
    # 先清理所有模块，确保 SDK 修改被正确编译
    mvn clean -B -Dmaven.clean.failOnError=false
    # 构建 service 模块及其依赖
    mvn package -pl service -am -DskipTests -B
    success "编译完成"
    
    # 编译完成后检查 API 元数据
    api_check
}

# 构建 Docker 镜像
docker_build() {
    info "开始构建 Docker 镜像..."
    # 构建镜像，使用最新的 classes 和 lib
    docker-compose -f "$COMPOSE_FILE" build
    success "Docker 镜像构建完成"
}

# 启动服务
up() {
    info "启动 Dubbo 服务..."
    docker-compose -f "$COMPOSE_FILE" up -d
    success "服务已启动"
    info "等待服务初始化..."
    sleep 5
    status
}

# 停止服务
down() {
    info "停止 Dubbo 服务..."
    docker-compose -f "$COMPOSE_FILE" down
    success "服务已停止"
}

# 重启服务
restart() {
    info "重启 Dubbo 服务..."
    docker-compose -f "$COMPOSE_FILE" restart
    success "服务已重启"
    info "等待服务初始化..."
    sleep 5
    status
}

# 查看日志
logs() {
    info "查看服务日志 (按 Ctrl+C 退出)..."
    docker-compose -f "$COMPOSE_FILE" logs -f "$SERVICE_NAME"
}

# 查看状态
status() {
    info "查看服务状态..."
    docker-compose -f "$COMPOSE_FILE" ps
    
    if docker-compose -f "$COMPOSE_FILE" ps | grep -q "Up"; then
        success "服务运行正常"
    else
        warn "服务可能未正常运行，请检查日志"
    fi
}

# 完整部署流程
deploy() {
    info "开始完整部署流程..."
    check_env
    build
    docker_build
    up
    success "部署完成！"
}

# 清理
clean() {
    info "清理构建产物..."
    mvn clean
    docker-compose -f "$COMPOSE_FILE" down -v --rmi local 2>/dev/null || true
    success "清理完成"
}

# 使用说明
usage() {
    echo "Dubbo 微服务部署脚本"
    echo ""
    echo "Usage:"
    echo "  ./deploy.sh [options] [command]"
    echo ""
    echo "Options:"
    echo "  -force      强制模式，跳过 API 元数据检查"
    echo ""
    echo "Commands:"
    echo "  build       本地 Maven 编译打包"
    echo "  api-check   检查 API 元数据（自动在 build 后执行）"
    echo "  docker      构建 Docker 镜像"
    echo "  up          启动服务"
    echo "  down        停止服务"
    echo "  restart     重启服务"
    echo "  logs        查看服务日志"
    echo "  status      查看服务状态"
    echo "  deploy      完整部署流程(编译+构建+启动)"
    echo "  clean       清理构建产物和镜像"
    echo "  help        显示使用说明"
    echo ""
    echo "Examples:"
    echo "  ./deploy.sh deploy              # 首次完整部署"
    echo "  ./deploy.sh -force deploy       # 强制部署（跳过 API 检查）"
    echo "  ./deploy.sh restart             # 修改代码后重启"
    echo "  ./deploy.sh logs                # 查看运行日志"
}

# 解析参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -force)
                FORCE_MODE=true
                shift
                ;;
            build|api-check|docker|up|down|restart|logs|status|deploy|clean|help|--help|-h)
                COMMAND="$1"
                shift
                ;;
            *)
                error "未知参数: $1"
                usage
                exit 1
                ;;
        esac
    done
}

# 主逻辑
COMMAND="${1:-deploy}"

# 如果第一个参数是 -force，需要特殊处理
if [ "$1" = "-force" ]; then
    FORCE_MODE=true
    COMMAND="${2:-deploy}"
fi

# 解析所有参数（支持 -force 在任意位置）
parse_args "$@"

case "$COMMAND" in
    build)
        check_env
        build
        ;;
    api-check)
        check_env
        api_check
        ;;
    docker)
        check_env
        docker_build
        ;;
    up)
        check_env
        up
        ;;
    down)
        down
        ;;
    restart)
        check_env
        restart
        ;;
    logs)
        logs
        ;;
    status)
        status
        ;;
    deploy)
        check_env
        info "开始完整部署流程..."
        build
        docker_build
        up
        success "部署完成！"
        ;;
    clean)
        clean
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        error "未知命令: $COMMAND"
        usage
        exit 1
        ;;
esac
