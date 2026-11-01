#!/bin/bash
# 只读导出四 / 六年级前测，不改库
set -euo pipefail
cd "$(dirname "$0")/.."
python3 database/export_pretest_g4_g6.py
echo
echo "请在宝塔文件管理下载："
echo "  $(pwd)/export/前测数据_*.zip"
