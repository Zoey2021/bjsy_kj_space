#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导出四年级 / 六年级前测全部提交数据。

在服务器项目根目录执行：
  bash database/export_pretest_g4_g6.sh

只读查询，不修改任何表。产物在 export/前测数据_时间戳/ 并打成 zip。
"""

from __future__ import print_function

import csv
import json
import os
import subprocess
import sys
from datetime import datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STAMP = datetime.now().strftime("%Y%m%d_%H%M%S")
OUT_DIR = os.path.join(ROOT, "export", "前测数据_" + STAMP)
ZIP_PATH = os.path.join(ROOT, "export", "前测数据_" + STAMP + ".zip")

MYSQL = [
    "docker", "exec", "-i", "learn-mysql",
    "mysql", "-ulearnuser", "-plearnpass123", "learn_space",
    "-N", "-B", "--default-character-set=utf8mb4",
]


def q(sql):
    p = subprocess.Popen(
        MYSQL,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    out, err = p.communicate(sql.encode("utf-8"))
    err_text = err.decode("utf-8", "ignore")
    if p.returncode != 0:
        # 过滤密码告警
        useful = "\n".join(
            line for line in err_text.splitlines()
            if "Using a password" not in line and line.strip()
        )
        raise RuntimeError(useful or ("mysql exit %s" % p.returncode))
    text = out.decode("utf-8", "ignore")
    rows = []
    for line in text.splitlines():
        if not line.strip():
            continue
        rows.append(line.split("\t"))
    return rows


def table_exists(name):
    rows = q("SHOW TABLES LIKE '%s';" % name.replace("'", ""))
    return bool(rows)


def ensure_dir(path):
    if not os.path.isdir(path):
        os.makedirs(path)


def write_csv(path, headers, rows):
    ensure_dir(os.path.dirname(path))
    with open(path, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(headers)
        for r in rows:
            w.writerow(r)


def decode_b64(text):
    import base64
    if not text or text == "NULL":
        return ""
    try:
        return base64.b64decode(text.encode("ascii")).decode("utf-8", "ignore")
    except Exception:
        return text


def export_g4(base):
    if not table_exists("learn_pretest_g4"):
        print("跳过：无表 learn_pretest_g4")
        return 0
    # content_json 用 Base64，避免 TSV 被换行/制表符打乱
    sql = (
        "SELECT id, student_id, IFNULL(class_id,''), IFNULL(student_name,''), "
        "IFNULL(class_name,''), IFNULL(student_no,''), "
        "fill_score, choice_right, choice_score, op_score, total_score, "
        "DATE_FORMAT(submitted_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
        "DATE_FORMAT(updated_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
        "TO_BASE64(content_json) "
        "FROM learn_pretest_g4 ORDER BY class_name, student_no, id"
    )
    rows = q(sql)
    summary = []
    detail_dir = os.path.join(base, "四年级前测", "作答明细JSON")
    ensure_dir(detail_dir)
    for r in rows:
        while len(r) < 14:
            r.append("")
        rid, sid, cid, sname, cname, sno = r[0], r[1], r[2], r[3], r[4], r[5]
        fill, cr, cs, op, total = r[6], r[7], r[8], r[9], r[10]
        submitted, updated = r[11], r[12]
        content = decode_b64(r[13])
        summary.append([
            rid, sid, cid, sname, cname, sno,
            fill, cr, cs, op, total, submitted, updated
        ])
        safe = "%s_%s_%s" % (cname or "未分班", sno or sid, sname or "学生")
        safe = "".join(ch if ch.isalnum() or ch in "-_." else "_" for ch in safe)
        jpath = os.path.join(detail_dir, safe + ".json")
        try:
            parsed = json.loads(content) if content else {}
            with open(jpath, "w", encoding="utf-8") as f:
                json.dump(parsed, f, ensure_ascii=False, indent=2)
        except Exception:
            with open(jpath, "w", encoding="utf-8") as f:
                f.write(content or "")
    write_csv(
        os.path.join(base, "四年级前测", "四年级前测_成绩汇总.csv"),
        [
            "记录ID", "学生ID", "班级ID", "姓名", "班级", "学号",
            "填空分", "选择答对", "选择分", "操作分", "机判总分",
            "提交时间", "更新时间",
        ],
        summary,
    )
    # 按班拆分
    by_class = {}
    for row in summary:
        by_class.setdefault(row[4] or "未分班", []).append(row)
    for cname, items in by_class.items():
        safe = "".join(ch if ch.isalnum() or ch in "-_." else "_" for ch in cname)
        write_csv(
            os.path.join(base, "四年级前测", "按班级", "四年级前测_%s.csv" % safe),
            [
                "记录ID", "学生ID", "班级ID", "姓名", "班级", "学号",
                "填空分", "选择答对", "选择分", "操作分", "机判总分",
                "提交时间", "更新时间",
            ],
            items,
        )
    print("四年级前测：%d 条" % len(summary))
    return len(summary)


def export_g6(base):
    if not table_exists("learn_pretest_g6"):
        print("跳过：无表 learn_pretest_g6")
        return 0
    sql = (
        "SELECT id, student_id, IFNULL(class_id,''), IFNULL(student_name,''), "
        "IFNULL(class_name,''), IFNULL(student_no,''), "
        "fill_score, choice_right, choice_score, op_score, att_score, total_score, "
        "IFNULL(level_code,''), "
        "DATE_FORMAT(submitted_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
        "DATE_FORMAT(updated_at,'%%Y-%%m-%%d %%H:%%i:%%s'), "
        "TO_BASE64(content_json) "
        "FROM learn_pretest_g6 ORDER BY class_name, student_no, id"
    )
    rows = q(sql)
    summary = []
    detail_dir = os.path.join(base, "六年级前测", "作答明细JSON")
    ensure_dir(detail_dir)
    for r in rows:
        while len(r) < 16:
            r.append("")
        rid, sid, cid, sname, cname, sno = r[0], r[1], r[2], r[3], r[4], r[5]
        fill, cr, cs, op, att, total = r[6], r[7], r[8], r[9], r[10], r[11]
        level, submitted, updated = r[12], r[13], r[14]
        content = decode_b64(r[15])
        summary.append([
            rid, sid, cid, sname, cname, sno,
            fill, cr, cs, op, att, total, level, submitted, updated
        ])
        safe = "%s_%s_%s" % (cname or "未分班", sno or sid, sname or "学生")
        safe = "".join(ch if ch.isalnum() or ch in "-_." else "_" for ch in safe)
        jpath = os.path.join(detail_dir, safe + ".json")
        try:
            parsed = json.loads(content) if content else {}
            with open(jpath, "w", encoding="utf-8") as f:
                json.dump(parsed, f, ensure_ascii=False, indent=2)
        except Exception:
            with open(jpath, "w", encoding="utf-8") as f:
                f.write(content or "")
    write_csv(
        os.path.join(base, "六年级前测", "六年级前测_成绩汇总.csv"),
        [
            "记录ID", "学生ID", "班级ID", "姓名", "班级", "学号",
            "填空分", "选择答对", "选择分", "操作分", "态度分", "机判总分",
            "前测档位", "提交时间", "更新时间",
        ],
        summary,
    )
    by_class = {}
    for row in summary:
        by_class.setdefault(row[4] or "未分班", []).append(row)
    for cname, items in by_class.items():
        safe = "".join(ch if ch.isalnum() or ch in "-_." else "_" for ch in cname)
        write_csv(
            os.path.join(base, "六年级前测", "按班级", "六年级前测_%s.csv" % safe),
            [
                "记录ID", "学生ID", "班级ID", "姓名", "班级", "学号",
                "填空分", "选择答对", "选择分", "操作分", "态度分", "机判总分",
                "前测档位", "提交时间", "更新时间",
            ],
            items,
        )
    print("六年级前测：%d 条" % len(summary))
    return len(summary)


def zip_dir(src, zip_path):
    import zipfile
    ensure_dir(os.path.dirname(zip_path))
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for root, _dirs, files in os.walk(src):
            for name in files:
                full = os.path.join(root, name)
                arc = os.path.relpath(full, os.path.dirname(src))
                zf.write(full, arc)


def main():
    ensure_dir(OUT_DIR)
    print("输出目录：", OUT_DIR)
    n4 = export_g4(OUT_DIR)
    n6 = export_g6(OUT_DIR)
    overview = os.path.join(OUT_DIR, "导出说明.txt")
    with open(overview, "w", encoding="utf-8") as f:
        f.write("前测数据导出\n")
        f.write("时间：%s\n" % datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        f.write("四年级前测条数：%d\n" % n4)
        f.write("六年级前测条数：%d\n" % n6)
        f.write("说明：本导出为只读，未修改数据库。\n")
        f.write("成绩汇总为 CSV；作答明细JSON 为每人 content_json 原文。\n")
    zip_dir(OUT_DIR, ZIP_PATH)
    print("已打包：", ZIP_PATH)
    print("合计：四年级 %d + 六年级 %d" % (n4, n6))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as e:
        sys.stderr.write("导出失败：%s\n" % e)
        sys.exit(1)
