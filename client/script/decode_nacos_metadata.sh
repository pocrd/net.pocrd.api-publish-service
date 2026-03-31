#!/bin/bash

# Nacos API 元数据获取与解码工具
# 此脚本调用 Python 脚本实现功能

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 调用 Python 脚本并传递所有参数
python3 "${SCRIPT_DIR}/decode_nacos_metadata.py" "$@"
