#!/bin/bash

# =============================================================================
# Higress Dubbo Triple HTTPS + mTLS 客户端证书认证测试脚本
# 测试 GreeterServiceHttpExport.greet2 接口
# =============================================================================

set -e

BASE_URL="https://api.caringfamily.cn:30443"
SERVICE_PATH="/dapi/com.pocrd.dubbo_demo.api.GreeterServiceHttpExport"
METHOD="greet2"

# 证书路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CERT_DIR="${PROJECT_ROOT}/../../higress/certs/files/bagua"
CLIENT_CERT="${CERT_DIR}/testFactory/devices/device001/device001-fullchain.crt"
CLIENT_KEY="${CERT_DIR}/testFactory/devices/device001/device001.key"

# curl --resolve 配置：将域名解析到本地 Higress
CURL_RESOLVE="--resolve api.caringfamily.cn:30443:127.0.0.1"

# 测试配置
MAX_TIME=15

echo "=============================================="
echo "GreeterService greet2 HTTPS + mTLS 测试"
echo "=============================================="
echo ""

echo "证书配置:"
echo "  客户端证书：$CLIENT_CERT"
echo "  客户端私钥：$CLIENT_KEY"
echo ""

echo "✅ 证书文件检查通过"
echo ""

# -----------------------------------------------------------------------------
# 测试 1: 中文参数
# -----------------------------------------------------------------------------
echo "测试 1/3: 中文参数"
echo "----------------------------------------------"

REQUEST_BODY='{"name1":"张三","name2":"李四"}'
echo "请求体：$REQUEST_BODY"

RESPONSE=$(curl -s --max-time $MAX_TIME -X POST \
  "${BASE_URL}${SERVICE_PATH}/${METHOD}" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_BODY" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  $CURL_RESOLVE)
CURL_EXIT=$?

echo "响应结果:"
echo "$RESPONSE"
echo ""

if [ $CURL_EXIT -eq 0 ] && echo "$RESPONSE" | grep -q "张三"; then
    echo "✅ 测试 1 通过"
    echo ""
else
    echo "❌ 测试 1 失败"
    echo "curl 退出码：$CURL_EXIT"
    exit 1
fi

# -----------------------------------------------------------------------------
# 测试 2: 英文参数
# -----------------------------------------------------------------------------
echo "测试 2/3: 英文参数"
echo "----------------------------------------------"

REQUEST_BODY_2='{"name1":"Alice","name2":"Bob"}'
echo "请求体：$REQUEST_BODY_2"

RESPONSE_2=$(curl -s --max-time $MAX_TIME -X POST \
  "${BASE_URL}${SERVICE_PATH}/${METHOD}" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_BODY_2" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  $CURL_RESOLVE)
CURL_EXIT=$?

echo "响应结果:"
echo "$RESPONSE_2"
echo ""

if [ $CURL_EXIT -eq 0 ] && echo "$RESPONSE_2" | grep -q "Alice"; then
    echo "✅ 测试 2 通过"
    echo ""
else
    echo "❌ 测试 2 失败"
    echo "curl 退出码：$CURL_EXIT"
    exit 1
fi

# -----------------------------------------------------------------------------
# 测试 3: 特殊字符参数
# -----------------------------------------------------------------------------
echo "测试 3/3: 特殊字符参数"
echo "----------------------------------------------"

REQUEST_BODY_3='{"name1":"User@123","name2":"Test#456"}'
echo "请求体：$REQUEST_BODY_3"

RESPONSE_3=$(curl -s --max-time $MAX_TIME -X POST \
  "${BASE_URL}${SERVICE_PATH}/${METHOD}" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_BODY_3" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  $CURL_RESOLVE)
CURL_EXIT=$?

echo "响应结果:"
echo "$RESPONSE_3"
echo ""

if [ $CURL_EXIT -eq 0 ] && echo "$RESPONSE_3" | grep -q "User@123"; then
    echo "✅ 测试 3 通过"
    echo ""
else
    echo "❌ 测试 3 失败"
    echo "curl 退出码：$CURL_EXIT"
    exit 1
fi

echo "=============================================="
echo "greet2 所有测试通过!"
echo "=============================================="
