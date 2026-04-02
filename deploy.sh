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
COMPOSE_PROJECT_NAME="api-publish-service"

# 强制模式标志（跳过 API 检查）
FORCE_MODE=false

# 加载环境变量文件
load_env_file() {
    local env_file=".env.${ENV}"
    if [[ -f "$env_file" ]]; then
        info "加载环境变量文件: $env_file"
        # 使用 source 加载环境变量，并导出到当前 shell
        set -a
        source "$env_file"
        set +a
    else
        error "环境变量文件不存在: $env_file"
        exit 1
    fi
}

# 验证环境参数
validate_env() {
    case "$ENV" in
        prod|test)
            load_env_file
            info "使用环境: $ENV"
            ;;
        *)
            error "未知环境: $ENV"
            error "支持的环境: prod, test"
            exit 1
            ;;
    esac
}

# 获取宿主机IP地址
get_host_ip() {
    # macOS 和 Linux 兼容的方式获取IP
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        ip=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "127.0.0.1")
    else
        # Linux
        ip=$(hostname -I | awk '{print $1}' 2>/dev/null || ip route get 8.8.8.8 | awk '{print $7; exit}' 2>/dev/null || echo "127.0.0.1")
    fi
    echo "$ip"
}

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
    check_command docker compose
    success "环境检查通过"
}

# 检查 API 元数据
api_check() {
    info "检查 API 元数据..."
    
    API_BASE_NAME="${SERVICE_NAME%-service}"
    API_JAR_PATTERN="api/target/${API_BASE_NAME}-api-*.jar"
    API_JAR=$(ls $API_JAR_PATTERN 2>/dev/null | grep -v "sources" | grep -v "javadoc" | head -1)
    
    if [ -z "$API_JAR" ] || [ ! -f "$API_JAR" ]; then
        error "API jar 文件不存在: $API_JAR_PATTERN"
        error "请先执行 ./deploy.sh build 进行编译"
        exit 1
    fi
    
    info "使用 API jar: $(basename $API_JAR)"
    
    # 查找 SDK jar - 从 service 的 lib 目录中查找（作为依赖引入）
    SDK_JAR_PATTERN="service/target/lib/api-publish-service-sdk-*.jar"
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
    mvn clean package -pl service -am -DskipTests -B
    success "编译完成"

    # 编译完成后检查 API 元数据
    api_check
}

# 构建 Docker 镜像
docker_build() {
    info "开始构建 Docker 镜像..."
    # 构建镜像，使用最新的 classes 和 lib
    docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" build
    success "Docker 镜像构建完成"
}

# 启动服务
up() {
    info "启动 Dubbo 服务..."
    # 动态获取宿主机IP并设置环境变量
    HOST_IP=$(get_host_ip)
    info "检测到宿主机IP: $HOST_IP"
    DUBBO_IP_TO_REGISTRY="$HOST_IP" docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" up -d 
    success "服务已启动"
    info "等待服务初始化..."
    sleep 5
    status
}

# 停止服务
down() {
    info "停止 Dubbo 服务..."
    docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" down
    success "服务已停止"
}

# 重启服务
restart() {
    info "重启 Dubbo 服务..."
    # 动态获取宿主机IP并设置环境变量
    HOST_IP=$(get_host_ip)
    info "检测到宿主机IP: $HOST_IP"
    # 使用 down + up 重新加载环境变量配置
    docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" down
    DUBBO_IP_TO_REGISTRY="$HOST_IP" docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" up -d
    success "服务已重启"
    info "等待服务初始化..."
    sleep 5
    status
}

# 查看日志
logs() {
    info "查看服务日志 (按 Ctrl+C 退出)..."
    docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" logs -f "$SERVICE_NAME"
}

# 查看状态
status() {
    info "查看服务状态..."
    docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" ps
    
    if docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" ps | grep -q "Up"; then
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
    docker compose -f "$COMPOSE_FILE" -p "$COMPOSE_PROJECT_NAME" down -v --rmi local 2>/dev/null || true
    success "清理完成"
}

# 使用说明
usage() {
    echo "Dubbo 微服务部署脚本"
    echo ""
    echo "Usage:"
    echo "  ./deploy.sh [env] [command]"
    echo "  ./deploy.sh [command]        # 默认使用 prod 环境"
    echo ""
    echo "Environments:"
    echo "  prod        生产环境 (默认)"
    echo "  test        测试环境"
    echo ""
    echo "Commands:"
    echo "  build       本地 Maven 编译打包"
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
    echo "  ./deploy.sh prod deploy      # 生产环境完整部署"
    echo "  ./deploy.sh test deploy      # 测试环境完整部署"
    echo "  ./deploy.sh prod up          # 启动生产环境服务"
    echo "  ./deploy.sh test up          # 启动测试环境服务"
    echo "  ./deploy.sh prod restart     # 重启生产环境服务"
    echo "  ./deploy.sh prod logs        # 查看生产环境日志"
}

# 解析参数
# 支持格式: ./deploy.sh [env] [command] [-force] 或 ./deploy.sh [command] [-force]
ENV="${1:-prod}"
COMMAND="${2:-deploy}"
FORCE_ARG="${3:-}"

# 如果第一个参数是命令而不是环境，则调整参数
if [[ "$ENV" =~ ^(build|docker|up|down|restart|logs|status|deploy|clean|help|--help|-h)$ ]]; then
    COMMAND="$ENV"
    ENV="prod"
    FORCE_ARG="${2:-}"
fi

# 处理 -force 参数
if [[ "$FORCE_ARG" == "-force" ]]; then
    FORCE_MODE=true
fi

# 验证环境
validate_env

# 主逻辑
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
