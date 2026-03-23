#!/bin/bash

# =============================================================================
# Higress Dubbo Triple 接口测试脚本
# 运行所有 HttpExport 接口测试
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=============================================="
echo "Higress Dubbo Triple 接口测试套件"
echo "=============================================="
echo ""

# 检查 HIGRESS_URL 环境变量
if [ -n "$HIGRESS_URL" ]; then
    echo "使用 Higress URL: $HIGRESS_URL"
else
    echo "使用默认 Higress URL: http://localhost"
    echo "(可通过设置 HIGRESS_URL 环境变量修改)"
fi
echo ""

# 统计结果
bash_passed=0
bash_failed=0

# -----------------------------------------------------------------------------
# 第一部分: Bash 脚本测试 (HttpExport 接口)
# -----------------------------------------------------------------------------
echo ""
echo "##############################################"
echo "第一部分: HttpExport 接口 Bash 测试"
echo "##############################################"
echo ""

# 运行所有测试脚本
test_scripts=(
    "test_greet.sh"
    "test_greet2.sh"
    "test_greet_stream.sh"
)

for script in "${test_scripts[@]}"; do
    echo ""
    echo "----------------------------------------------"
    echo "运行测试: $script"
    echo "----------------------------------------------"
    echo ""
    
    if bash "${SCRIPT_DIR}/${script}"; then
        ((bash_passed++))
    else
        ((bash_failed++))
        echo "❌ $script 测试失败"
    fi
done

# -----------------------------------------------------------------------------
# 第二部分: Java 测试 (Internal 接口)
# -----------------------------------------------------------------------------
echo ""
echo "##############################################"
echo "第二部分: Internal 接口 Java 测试"
echo "##############################################"
echo ""
echo "注意: Java 测试需要在 Dubbo RPC 模式下运行"
echo "运行命令: mvn test -pl client -Pdubbo-test"
echo ""

# 汇总结果
echo ""
echo "=============================================="
echo "测试套件执行完成"
echo "=============================================="
echo ""
echo "Bash 脚本测试 (HttpExport 接口):"
echo "  通过: $bash_passed"
echo "  失败: $bash_failed"
echo ""
echo "Java 测试 (Internal 接口):"
echo "  请使用 Maven 运行: mvn test -pl client -Pdubbo-test"
echo ""

if [ $bash_failed -eq 0 ]; then
    echo "✅ 所有 Bash 测试通过!"
    echo ""
    echo "测试类说明:"
    echo "  - GreeterServiceTest: HttpExport 接口的 HTTP 模式测试"
    echo "  - GreeterServiceInternalTest: Internal 接口的 Dubbo RPC 测试"
    exit 0
else
    echo "❌ 部分 Bash 测试失败"
    exit 1
fi
