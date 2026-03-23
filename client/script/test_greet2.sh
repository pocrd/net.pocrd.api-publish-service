#!/bin/bash

# =============================================================================
# Higress Dubbo Triple 接口测试脚本
# 测试 GreeterServiceHttpExport.greet2 接口
# =============================================================================

set -e

BASE_URL="${HIGRESS_URL:-http://localhost}"
SERVICE_PATH="/api/com.pocrd.api_publish_service.api.GreeterServiceHttpExport"
METHOD="greet2"

echo "=============================================="
echo "Higress Dubbo Triple 接口测试"
echo "接口: ${METHOD}"
echo "=============================================="
echo ""

# -----------------------------------------------------------------------------
# 测试 1: HTTP/2 + application/json 访问 greet2 接口
# -----------------------------------------------------------------------------
echo "测试 1: HTTP/2 + JSON 请求 ${METHOD} 接口"
echo "----------------------------------------------"
echo "请求: POST ${BASE_URL}${SERVICE_PATH}/${METHOD}"
echo "参数: name1=张三, name2=李四"
echo ""

REQUEST_BODY='{"name1":"张三","name2":"李四"}'

echo "请求 Body:"
echo "$REQUEST_BODY"
echo ""

RESPONSE=$(curl -s --http2-prior-knowledge -X POST \
  "${BASE_URL}${SERVICE_PATH}/${METHOD}" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_BODY")

echo "响应结果:"
echo "$RESPONSE"
echo ""

# 验证响应是否包含预期内容
if echo "$RESPONSE" | grep -q "Hello 张三 and 李四"; then
    echo "✅ 测试 1 通过: 响应包含预期内容"
else
    echo "❌ 测试 1 失败: 响应不包含预期内容"
    exit 1
fi

# -----------------------------------------------------------------------------
# 测试 2: 使用不同参数测试
# -----------------------------------------------------------------------------
echo ""
echo "测试 2: 使用英文参数测试"
echo "----------------------------------------------"
echo "参数: name1=Alice, name2=Bob"
echo ""

REQUEST_BODY_2='{"name1":"Alice","name2":"Bob"}'

RESPONSE_2=$(curl -s --http2-prior-knowledge -X POST \
  "${BASE_URL}${SERVICE_PATH}/${METHOD}" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_BODY_2")

echo "响应结果:"
echo "$RESPONSE_2"
echo ""

if echo "$RESPONSE_2" | grep -q "Hello Alice and Bob"; then
    echo "✅ 测试 2 通过"
else
    echo "❌ 测试 2 失败"
    exit 1
fi

# -----------------------------------------------------------------------------
# 测试 3: 特殊字符参数测试
# -----------------------------------------------------------------------------
echo ""
echo "测试 3: 特殊字符参数测试"
echo "----------------------------------------------"
echo "参数: name1=User@123, name2=Test#456"
echo ""

REQUEST_BODY_3='{"name1":"User@123","name2":"Test#456"}'

RESPONSE_3=$(curl -s --http2-prior-knowledge -X POST \
  "${BASE_URL}${SERVICE_PATH}/${METHOD}" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_BODY_3")

echo "响应结果:"
echo "$RESPONSE_3"
echo ""

if echo "$RESPONSE_3" | grep -q "Hello User@123 and Test#456"; then
    echo "✅ 测试 3 通过"
else
    echo "❌ 测试 3 失败"
    exit 1
fi

echo ""
echo "=============================================="
echo "所有测试通过!"
echo "=============================================="
