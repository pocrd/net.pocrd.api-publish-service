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

# 本地编译
build() {
    info "开始 Maven 编译打包..."
    mvn clean package -pl service -am -DskipTests -B
    success "编译完成"
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
    echo "  ./deploy.sh [command]"
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
    echo "  ./deploy.sh deploy      # 首次完整部署"
    echo "  ./deploy.sh restart     # 修改代码后重启"
    echo "  ./deploy.sh logs        # 查看运行日志"
}

# 主逻辑
case "${1:-deploy}" in
    build)
        check_env
        build
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
        deploy
        ;;
    clean)
        clean
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        error "未知命令: $1"
        usage
        exit 1
        ;;
esac
