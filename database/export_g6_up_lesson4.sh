#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 database/export_g6_up_lessons.py 4
