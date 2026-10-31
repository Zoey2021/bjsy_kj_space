#!/bin/bash
cd "$(dirname "$0")/.." || exit 1
python3 database/export_g6_up_lesson3.py
