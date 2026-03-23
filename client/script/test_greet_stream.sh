#!/bin/bash

# =============================================================================
# Higress Dubbo Triple 接口测试脚本
# 测试 GreeterServiceHttpExport.greetStream 接口 (Server Streaming)
# =============================================================================

set -e

BASE_URL="${HIGRESS_URL:-http://localhost}"
SERVICE_PATH="/api/com.pocrd.api_publish_service.api.GreeterServiceHttpExport"
METHOD="greetStream"

echo "=============================================="
echo "Higress Dubbo Triple 接口测试"
echo "接口: ${METHOD} (Server Streaming)"
echo "=============================================="
echo ""

# -----------------------------------------------------------------------------
# 测试 1: HTTP/2 + application/json 访问 greetStream 接口
# -----------------------------------------------------------------------------
echo "测试 1: HTTP/2 + JSON 请求 ${METHOD} 接口"
echo "----------------------------------------------"
echo "请求: POST ${BASE_URL}${SERVICE_PATH}/${METHOD}"
echo "参数: name=StreamUser"
echo ""

# 使用 URL 参数传递（避免单参数 String 的 JSON 解析问题）
REQUEST_PARAMS="name=StreamUser"

echo "请求参数: $REQUEST_PARAMS"
echo ""

echo "响应结果 (流式输出):"
echo ""

# 临时文件存储响应
RESPONSE_FILE=$(mktemp)

# 使用 --no-buffer 来实时显示流式响应，同时保存到文件
curl -s --http2-prior-knowledge -X GET \
  "${BASE_URL}${SERVICE_PATH}/${METHOD}?${REQUEST_PARAMS}" \
  -H "Accept: application/json" \
  --no-buffer | tee "$RESPONSE_FILE"

echo ""
echo ""

# 验证流式响应（SSE 格式：data:"..."）
EXPECTED_COUNT=5
ACTUAL_COUNT=$(grep -c 'data:"Hello StreamUser' "$RESPONSE_FILE" 2>/dev/null || echo "0")

echo "验证结果:"
echo "----------------------------------------------"
echo "期望消息数: $EXPECTED_COUNT"
echo "实际消息数: $ACTUAL_COUNT"
echo ""

if [ "$ACTUAL_COUNT" -eq "$EXPECTED_COUNT" ]; then
    echo "✅ 流式响应验证通过: 收到 $ACTUAL_COUNT 条消息"
else
    echo "❌ 流式响应验证失败: 期望 $EXPECTED_COUNT 条，实际收到 $ACTUAL_COUNT 条"
    rm -f "$RESPONSE_FILE"
    exit 1
fi

# 验证消息序号
for i in 1 2 3 4 5; do
    if grep -q "greeting #$i" "$RESPONSE_FILE"; then
        echo "✅ 消息 #$i 验证通过"
    else
        echo "❌ 消息 #$i 缺失"
        rm -f "$RESPONSE_FILE"
        exit 1
    fi
done

rm -f "$RESPONSE_FILE"
echo ""
echo "✅ 流式接口调用完成，所有验证通过"

echo ""
echo "=============================================="
echo "测试完成!"
echo "=============================================="
