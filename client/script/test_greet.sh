#!/bin/bash

# =============================================================================
# Higress Dubbo Triple 接口测试脚本
# 测试 GreeterServiceHttpExport.greet 接口
#
# 【重要说明】Dubbo Triple 单参数 String 接口的调用方式
#
# 当接口只有一个参数 且类型为String 时，使用 POST + JSON body 会有歧义：
#   - 发送 {"name":"World"} 时，Dubbo 会把整个 JSON 字符串赋给 name 参数
#   - 结果 name = "{\"name\":\"World\"}" 而不是 "World"
#
# 解决方案：
#   1. 使用 URL 查询参数（推荐）：/greet?name=World
#   2. 使用 POST + form-data：name=World
#   3. 使用对象包装参数（像 greet2 那样）：greet(GreetRequest request)
#
# 本脚本采用方案 1：通过 URL 查询参数传递参数
# =============================================================================

set -e

BASE_URL="${HIGRESS_URL:-http://localhost}"
SERVICE_PATH="/api/com.pocrd.api_publish_service.api.GreeterServiceHttpExport"
METHOD="greet"

echo "=============================================="
echo "Higress Dubbo Triple 接口测试"
echo "接口: ${METHOD}"
echo "=============================================="
echo ""

# -----------------------------------------------------------------------------
# 测试 1: HTTP/2 + URL 参数访问 greet 接口
# -----------------------------------------------------------------------------
echo "测试 1: HTTP/2 + URL 参数请求 ${METHOD} 接口"
echo "----------------------------------------------"
echo "请求: GET ${BASE_URL}${SERVICE_PATH}/${METHOD}?name=World"
echo "参数: name=World"
echo ""

RESPONSE=$(curl -s --http2-prior-knowledge -X GET \
  "${BASE_URL}${SERVICE_PATH}/${METHOD}?name=World")

echo "响应结果:"
echo "$RESPONSE"
echo ""

# 验证响应是否包含预期内容
if echo "$RESPONSE" | grep -q "World"; then
    echo "✅ 测试通过: 响应包含预期内容"
else
    echo "❌ 测试失败: 响应不包含预期内容"
    exit 1
fi

echo ""
echo "=============================================="
echo "所有测试通过!"
echo "=============================================="
