#!/bin/bash
# =============================================================================
# 从 Nacos 获取 Dubbo 服务并生成 Higress Ingress 配置
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认配置
DEFAULT_NACOS_URL="http://localhost:30848"
DEFAULT_DOMAIN="api.example.com"

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项]

选项:
    -h, --help              显示帮助信息
    -n, --nacos URL         Nacos 服务器地址 (默认: ${DEFAULT_NACOS_URL})
    -d, --domain DOMAIN     Ingress 域名 (默认: ${DEFAULT_DOMAIN})
    -g, --group GROUP       Nacos 服务分组 (默认: 空)
    --namespace ID          Nacos 命名空间 ID (默认: 空)
    -o, --output FILE       输出文件路径 (默认: 输出到 stdout)
    --mcp-only              只生成 McpBridge 配置
    --ingress-only          只生成 Ingress 配置

示例:
    # 生成所有服务的配置
    $0 -n http://localhost:8848

    # 指定域名生成配置
    $0 -n http://localhost:8848 -d api.caringfamily.cn

    # 只生成 McpBridge
    $0 -n http://localhost:8848 --mcp-only

    # 输出到文件
    $0 -n http://localhost:8848 -o ingress.yaml

EOF
}

# 解析参数
NACOS_URL="${DEFAULT_NACOS_URL}"
DOMAIN="${DEFAULT_DOMAIN}"
GROUP=""
NAMESPACE=""
OUTPUT=""
MCP_ONLY=false
INGRESS_ONLY=false

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
        -d|--domain)
            DOMAIN="$2"
            shift 2
            ;;
        -g|--group)
            GROUP="$2"
            shift 2
            ;;
        --namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -o|--output)
            OUTPUT="$2"
            shift 2
            ;;
        --mcp-only)
            MCP_ONLY=true
            shift
            ;;
        --ingress-only)
            INGRESS_ONLY=true
            shift
            ;;
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

echo -e "${BLUE}正在从 Nacos 获取 Dubbo 服务...${NC}"
echo "  Nacos: ${NACOS_URL}"
if [[ -n "${GROUP}" ]]; then
    echo "  Group: ${GROUP}"
fi
echo ""

# 获取服务列表
echo -e "${BLUE}获取服务列表...${NC}"

# 构建查询参数
QUERY_PARAMS="pageNo=1&pageSize=100"
if [[ -n "${NAMESPACE}" ]]; then
    QUERY_PARAMS="${QUERY_PARAMS}&namespaceId=${NAMESPACE}"
fi
if [[ -n "${GROUP}" ]]; then
    QUERY_PARAMS="${QUERY_PARAMS}&groupName=${GROUP}"
fi

SERVICE_LIST_URL="${NACOS_URL}/nacos/v1/ns/service/list?${QUERY_PARAMS}"

# 使用 curl 获取服务列表
if ! SERVICES_JSON=$(curl -s --connect-timeout 10 "${SERVICE_LIST_URL}" 2>/dev/null); then
    echo -e "${RED}✗ 无法连接到 Nacos: ${NACOS_URL}${NC}"
    exit 1
fi

# 检查是否有服务
if [[ -z "${SERVICES_JSON}" ]] || [[ "${SERVICES_JSON}" == "null" ]]; then
    echo -e "${YELLOW}⚠ 未找到任何服务${NC}"
    exit 0
fi

# 解析服务列表（使用简单的字符串处理）
# 注意：这里假设返回的是 JSON 格式，但在纯 bash 中解析 JSON 比较困难
# 我们使用 grep 和 sed 来提取服务名
SERVICE_NAMES=$(echo "${SERVICES_JSON}" | grep -o '"doms":\[[^]]*\]' | grep -o '"[^"]*"' | tr -d '"' | tr ',' '\n' | grep -v '^doms$')

if [[ -z "${SERVICE_NAMES}" ]]; then
    echo -e "${YELLOW}⚠ 未找到任何服务${NC}"
    exit 0
fi

SERVICE_COUNT=$(echo "${SERVICE_NAMES}" | wc -l | tr -d ' ')
echo -e "${GREEN}✓ 找到 ${SERVICE_COUNT} 个服务${NC}"

# 收集 Dubbo 服务信息
declare -a DUBBO_SERVICES=()

for SERVICE_NAME in ${SERVICE_NAMES}; do
    # 获取服务实例
    INSTANCE_PARAMS="serviceName=${SERVICE_NAME}"
    if [[ -n "${GROUP}" ]]; then
        INSTANCE_PARAMS="${INSTANCE_PARAMS}&groupName=${GROUP}"
    fi
    
    INSTANCE_URL="${NACOS_URL}/nacos/v1/ns/instance/list?${INSTANCE_PARAMS}"
    
    if ! INSTANCE_JSON=$(curl -s --connect-timeout 10 "${INSTANCE_URL}" 2>/dev/null); then
        continue
    fi
    
    # 检查是否是 Dubbo 服务（简单检查）
    if [[ "${INSTANCE_JSON}" == *'"dubbo":"2.0.2"'* ]]; then
        # 提取接口名
        INTERFACE=$(echo "${INSTANCE_JSON}" | grep -o '"interface":"[^"]*"' | head -1 | cut -d'"' -f4)
        VERSION=$(echo "${INSTANCE_JSON}" | grep -o '"version":"[^"]*"' | head -1 | cut -d'"' -f4)
        APPLICATION=$(echo "${INSTANCE_JSON}" | grep -o '"application":"[^"]*"' | head -1 | cut -d'"' -f4)
        GROUP_NAME=$(echo "${INSTANCE_JSON}" | grep -o '"group":"[^"]*"' | head -1 | cut -d'"' -f4)
        
        if [[ -n "${INTERFACE}" ]] && [[ -n "${VERSION}" ]] && [[ -n "${APPLICATION}" ]]; then
            echo -e "  ${GREEN}•${NC} ${INTERFACE}:${VERSION}"
            
            # 存储服务信息（使用特殊分隔符）
            DUBBO_SERVICES+=("${INTERFACE}|${VERSION}|${APPLICATION}|${GROUP_NAME}")
        fi
    fi
done

if [[ ${#DUBBO_SERVICES[@]} -eq 0 ]]; then
    echo -e "${YELLOW}⚠ 未找到 Dubbo 服务${NC}"
    exit 0
fi

echo -e "\n${GREEN}✓ 共找到 ${#DUBBO_SERVICES[@]} 个 Dubbo 服务${NC}"

# 生成配置
OUTPUT_CONTENT=""

# 生成 McpBridge 配置
if [[ "${INGRESS_ONLY}" == false ]]; then
    echo -e "\n${BLUE}生成 McpBridge 配置...${NC}"
    
    # 按应用分组
    declare -A APP_SERVICES
    for SERVICE in "${DUBBO_SERVICES[@]}"; do
        IFS='|' read -r INTERFACE VERSION APPLICATION GROUP_NAME <<< "${SERVICE}"
        if [[ -z "${APP_SERVICES[${APPLICATION}]}" ]]; then
            APP_SERVICES[${APPLICATION}]=""
        fi
        APP_SERVICES[${APPLICATION}]="${APP_SERVICES[${APPLICATION}]}${INTERFACE}:${VERSION}:${GROUP_NAME},"
    done
    
    MCP_COUNT=0
    for APP_NAME in "${!APP_SERVICES[@]}"; do
        # 生成 McpBridge YAML
        MCP_BRIDGE=$(cat << EOF
---
apiVersion: networking.higress.io/v1
kind: McpBridge
metadata:
  name: ${APP_NAME}
  namespace: higress-system
spec:
  registries:
    - name: ${APP_NAME}-nacos
      type: nacos
      domain: nacos-server.higress-system.svc.cluster.local
      port: 8848
      nacosNamespaceId: public
      nacosGroups:
        - DEFAULT_GROUP

EOF
)
        OUTPUT_CONTENT="${OUTPUT_CONTENT}${MCP_BRIDGE}"
        ((MCP_COUNT++))
    done
    
    echo -e "  ${GREEN}✓ 生成 ${MCP_COUNT} 个 McpBridge${NC}"
fi

# 生成 Ingress 配置
if [[ "${MCP_ONLY}" == false ]]; then
    echo -e "\n${BLUE}生成 Ingress 配置...${NC}"
    
    INGRESS_COUNT=0
    for SERVICE in "${DUBBO_SERVICES[@]}"; do
        IFS='|' read -r INTERFACE VERSION APPLICATION GROUP_NAME <<< "${SERVICE}"
        
        # 从接口名生成路径
        SERVICE_NAME=$(echo "${INTERFACE}" | awk -F'.' '{print $NF}')
        PACKAGE_PATH=$(echo "${INTERFACE}" | sed 's/\./\//g' | sed "s|/${SERVICE_NAME}||")
        PATH_PREFIX="/dubbo/${PACKAGE_PATH}/${SERVICE_NAME}"
        
        # 生成 Ingress YAML
        INGRESS=$(cat << EOF
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ${APPLICATION}-${SERVICE_NAME,,}
  namespace: higress-system
  annotations:
    higress.io/destination: ${APPLICATION}-${INTERFACE}:${VERSION}
    higress.io/rpc-type: dubbo
    higress.io/dubbo-version: ${VERSION}
    higress.io/dubbo-group: ${GROUP_NAME:-default}
spec:
  ingressClassName: higress
  rules:
    - host: ${DOMAIN}
      http:
        paths:
          - path: ${PATH_PREFIX}
            pathType: Prefix
            backend:
              service:
                name: ${APPLICATION}
                port:
                  number: 80

EOF
)
        OUTPUT_CONTENT="${OUTPUT_CONTENT}${INGRESS}"
        ((INGRESS_COUNT++))
    done
    
    echo -e "  ${GREEN}✓ 生成 ${INGRESS_COUNT} 个 Ingress${NC}"
fi

# 添加头部注释
HEADER=$(cat << EOF
# Generated by generate_ingress_from_nacos.sh
# Date: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
# Nacos: ${NACOS_URL}
# Total Services: ${#DUBBO_SERVICES[@]}

EOF
)

FINAL_OUTPUT="${HEADER}${OUTPUT_CONTENT}"

# 输出结果
if [[ -n "${OUTPUT}" ]]; then
    echo "${FINAL_OUTPUT}" > "${OUTPUT}"
    echo -e "\n${GREEN}✓ 配置已保存到: ${OUTPUT}${NC}"
else
    echo -e "\n${YELLOW}========== 生成的配置 ==========${NC}\n"
    echo "${FINAL_OUTPUT}"
fi
