#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导出六年级上册第4课学情。在服务器项目目录执行。"""
import runpy
import sys

sys.argv = [sys.argv[0], "4"]
runpy.run_path(__import__("os").path.join(__import__("os").path.dirname(__file__), "export_g6_up_lessons.py"), run_name="__main__")
