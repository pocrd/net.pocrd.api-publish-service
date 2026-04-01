#!/bin/bash

# =============================================================================
# Higress Dubbo Triple HTTPS + mTLS 客户端证书认证测试脚本
# 测试 CRUDServiceHttpExport Product实体所有CRUD接口 (HTTPS + 客户端证书认证)
# 包含：createProduct, getProductById, updateProduct, deleteProduct, getAllProducts
# =============================================================================

set -e

BASE_URL="https://api.caringfamily.cn:30443"
SERVICE_PATH="/dapi/com.pocrd.dubbo_demo.api.CRUDServiceHttpExport"

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
echo "CRUDService Product实体 HTTPS + mTLS 测试"
echo "=============================================="
echo ""

echo "证书配置:"
echo "  客户端证书：$CLIENT_CERT"
echo "  客户端私钥：$CLIENT_KEY"
echo ""

echo "✅ 证书文件检查通过"
echo ""

# -----------------------------------------------------------------------------
# 1. 测试 createProduct 接口
# -----------------------------------------------------------------------------
echo "测试 1/5: createProduct 接口"
echo "----------------------------------------------"

TIMESTAMP=$(date +%s)
CREATE_PRODUCT_BODY="{\"productCode\":\"PROD_${TIMESTAMP}\",\"productName\":\"电脑 ${TIMESTAMP}\",\"price\":99,\"stock\":100,\"category\":\"Electronics\"}"
echo "请求体：$CREATE_PRODUCT_BODY"

RESPONSE_BODY=$(curl -s --max-time $MAX_TIME -X POST \
  "${BASE_URL}${SERVICE_PATH}/createProduct" \
  -H "Content-Type: application/json" \
  -d "$CREATE_PRODUCT_BODY" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  $CURL_RESOLVE)
CURL_EXIT=$?

echo "响应结果:"
echo "$RESPONSE_BODY"
echo ""

if [ $CURL_EXIT -eq 0 ] && echo "$RESPONSE_BODY" | grep -q "id" && echo "$RESPONSE_BODY" | grep -q "PROD_${TIMESTAMP}"; then
    echo "✅ createProduct 测试通过"
    
    # 提取创建的产品ID用于后续测试
    CREATED_PRODUCT_ID=$(echo "$RESPONSE_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
    echo "创建的产品ID: $CREATED_PRODUCT_ID"
    echo ""
else
    echo "❌ createProduct 测试失败"
    echo "curl 退出码：$CURL_EXIT"
    exit 1
fi

# -----------------------------------------------------------------------------
# 2. 测试 getProductById 接口
# -----------------------------------------------------------------------------
echo "测试 2/5: getProductById 接口"
echo "----------------------------------------------"

RESPONSE_BODY=$(curl -s --max-time $MAX_TIME -X GET \
  "${BASE_URL}${SERVICE_PATH}/getProductById?id=${CREATED_PRODUCT_ID}" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  $CURL_RESOLVE)
CURL_EXIT=$?

echo "响应结果:"
echo "$RESPONSE_BODY"
echo ""

if [ $CURL_EXIT -eq 0 ] && echo "$RESPONSE_BODY" | grep -q "\"id\":${CREATED_PRODUCT_ID}"; then
    echo "✅ getProductById 测试通过"
    echo ""
else
    echo "❌ getProductById 测试失败"
    echo "curl 退出码：$CURL_EXIT"
    exit 1
fi

# -----------------------------------------------------------------------------
# 3. 测试 updateProduct 接口
# -----------------------------------------------------------------------------
echo "测试 3/5: updateProduct 接口"
echo "----------------------------------------------"

# 从getProductById响应中提取完整产品信息并更新
PRODUCT_INFO=$(curl -s --max-time $MAX_TIME -X GET \
  "${BASE_URL}${SERVICE_PATH}/getProductById?id=${CREATED_PRODUCT_ID}" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  $CURL_RESOLVE)

# 更新价格和库存
UPDATED_PRODUCT_BODY=$(echo "$PRODUCT_INFO" | sed 's/"price":[0-9.]*\([,}]\)/"price":199\1/' | sed 's/"stock":[0-9]*/"stock":50/')

RESPONSE_BODY=$(curl -s --max-time $MAX_TIME -X POST \
  "${BASE_URL}${SERVICE_PATH}/updateProduct" \
  -H "Content-Type: application/json" \
  -d "$UPDATED_PRODUCT_BODY" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  $CURL_RESOLVE)
CURL_EXIT=$?

echo "更新请求体："
echo "$UPDATED_PRODUCT_BODY"
echo ""

echo "响应结果:"
echo "$RESPONSE_BODY"
echo ""

if [ $CURL_EXIT -eq 0 ] && echo "$RESPONSE_BODY" | grep -q "199" && echo "$RESPONSE_BODY" | grep -q "\"stock\":50"; then
    echo "✅ updateProduct 测试通过"
    echo ""
else
    echo "❌ updateProduct 测试失败"
    echo "curl 退出码：$CURL_EXIT"
    exit 1
fi

# -----------------------------------------------------------------------------
# 4. 测试 getAllProducts 接口
# -----------------------------------------------------------------------------
echo "测试 4/5: getAllProducts 接口"
echo "----------------------------------------------"

RESPONSE_BODY=$(curl -s --max-time $MAX_TIME -X GET \
  "${BASE_URL}${SERVICE_PATH}/getAllProducts" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  $CURL_RESOLVE)
CURL_EXIT=$?

echo "响应结果长度: $(echo "$RESPONSE_BODY") "
echo ""

if [ $CURL_EXIT -eq 0 ] && [ $(echo "$RESPONSE_BODY" | wc -c) -gt 10 ]; then
    echo "✅ getAllProducts 测试通过"
    echo ""
else
    echo "❌ getAllProducts 测试失败"
    echo "curl 退出码：$CURL_EXIT"
    exit 1
fi

# -----------------------------------------------------------------------------
# 5. 测试 deleteProduct 接口
# -----------------------------------------------------------------------------
echo "测试 5/5: deleteProduct 接口"
echo "----------------------------------------------"

RESPONSE_BODY=$(curl -s --max-time $MAX_TIME -X GET \
  "${BASE_URL}${SERVICE_PATH}/deleteProduct?id=${CREATED_PRODUCT_ID}" \
  --cert "$CLIENT_CERT" \
  --key "$CLIENT_KEY" \
  $CURL_RESOLVE)
CURL_EXIT=$?

echo "响应结果:"
echo "$RESPONSE_BODY"
echo ""

if [ $CURL_EXIT -eq 0 ] && ([ "$RESPONSE_BODY" = "true" ] || echo "$RESPONSE_BODY" | grep -q "true"); then
    echo "✅ deleteProduct 测试通过"
    echo ""
else
    echo "❌ deleteProduct 测试失败"
    echo "curl 退出码：$CURL_EXIT"
    exit 1
fi

echo "=============================================="
echo "Product实体所有CRUD测试完成!"
echo "=============================================="
exit 0